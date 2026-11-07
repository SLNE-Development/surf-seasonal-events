package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerView
import dev.slne.surf.seasonalevents.api.result.CollectResult
import dev.slne.surf.seasonalevents.api.result.PreviewResult
import dev.slne.surf.seasonalevents.core.rpc.EventRpcService
import dev.slne.surf.seasonalevents.core.rpc.ProgressRpcService
import dev.slne.surf.seasonalevents.microservice.db.repository.AssignmentRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.CollectionRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.MarkerRepository
import dev.slne.surf.seasonalevents.microservice.logic.CollectRules
import dev.slne.surf.seasonalevents.microservice.logic.PackagePicker
import dev.slne.surf.seasonalevents.microservice.logic.PackageRef
import dev.slne.surf.seasonalevents.microservice.logic.SeasonalClock
import dev.slne.surf.seasonalevents.microservice.logic.StatusRules
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

/**
 * Microservice side of [ProgressRpcService]: daily assignments, previews and booking collections.
 * Package items are only loaded for the package that is previewed or handed out.
 *
 * @param events used to read the active event, which also ends it once its end date has passed
 * @param clock source of "today"; replaceable to test day changes
 */
class ProgressRpcServiceImpl(
    private val events: EventRpcService,
    private val clock: SeasonalClock = SeasonalClock(),
    private val random: Random = Random.Default,
) : ProgressRpcService {

    /**
     * Serializes [collect] per player, so parallel clicks cannot both pass the checks and receive
     * the same package twice. Valid because only one microservice instance runs.
     */
    private val collectLocks = Array(64) { Mutex() }

    private fun lockFor(player: UUID) = collectLocks[(player.hashCode() and Int.MAX_VALUE) % collectLocks.size]

    override suspend fun getPlayerView(player: UUID): PlayerView? = suspendTransaction {
        val event = events.getActiveEvent() ?: return@suspendTransaction null
        val markers = when (event.mode) {
            EventMode.DAILY -> listOfNotNull(dailyMarker(event, player, clock.today()))
            EventMode.COLLECT -> {
                val collected = CollectionRepository.collectedMarkerIds(event.id, player)
                MarkerRepository.findByEvent(event.id).filter { it.id !in collected }
            }
        }
        PlayerView(event, markers)
    }

    /**
     * Returns today's marker of [player], creating the assignment on the first view of the day.
     * Null if the player already collected today or no package is left for them today.
     */
    private suspend fun dailyMarker(event: SeasonalEvent, player: UUID, today: LocalDate): EventMarker? {
        if (CollectionRepository.countOnDay(event.id, player, today) > 0) return null

        AssignmentRepository.find(event.id, player, today)?.let { return MarkerRepository.findById(it) }

        if (candidates(event, player, today).isEmpty()) return null

        // Prefer markers the player has not found on earlier days; fall back to all of them.
        val markers = MarkerRepository.findByEvent(event.id)
        val collected = CollectionRepository.collectedMarkerIds(event.id, player)
        val fresh = markers.filter { it.id !in collected }.ifEmpty { markers }
        val markerId = fresh.randomOrNull(random)?.id ?: return null

        AssignmentRepository.insert(event.id, player, today, markerId)
        // Read back: a parallel first view may have inserted a different marker.
        val assigned = AssignmentRepository.find(event.id, player, today) ?: return null
        return MarkerRepository.findById(assigned)
    }

    override suspend fun getEditView(eventId: ULong): List<EventMarker> = MarkerRepository.findByEvent(eventId)

    override suspend fun previewCollect(player: UUID, markerId: ULong): PreviewResult = suspendTransaction {
        when (val check = check(player, markerId)) {
            is Check.Denied -> check.result
            is Check.Allowed -> {
                // A package deleted or emptied since the candidates were read is skipped.
                val lootPackage = check.candidates.shuffled(random).firstNotNullOfOrNull { ref ->
                    LootRepository.findById(ref.id)?.takeIf { it.items.isNotEmpty() }
                } ?: return@suspendTransaction PreviewResult.NothingLeft
                PreviewResult.Preview(lootPackage.id, lootPackage.items)
            }
        }
    }

    override suspend fun collect(player: UUID, markerId: ULong, packageId: ULong): CollectResult =
        lockFor(player).withLock {
            suspendTransaction {
                val check = when (val check = check(player, markerId)) {
                    is Check.Denied -> return@suspendTransaction check.result.toCollectResult()
                    is Check.Allowed -> check
                }
                if (check.candidates.isEmpty()) return@suspendTransaction CollectResult.NothingLeft
                if (check.candidates.none { it.id == packageId }) return@suspendTransaction CollectResult.PackageUnavailable

                val lootPackage = LootRepository.findById(packageId)?.takeIf { it.items.isNotEmpty() }
                    ?: return@suspendTransaction CollectResult.PackageUnavailable

                val booked = CollectionRepository.insert(
                    eventId = check.event.id,
                    player = player,
                    markerId = markerId,
                    packageId = packageId,
                    day = check.today,
                    dedupKey = CollectionRepository.dedupKey(check.event.mode, check.today, markerId),
                )
                if (booked) CollectResult.Success(lootPackage) else CollectResult.AlreadyCollected
            }
        }

    override suspend fun getStatus(player: UUID): PlayerStatus = suspendTransaction {
        val event = events.getActiveEvent() ?: return@suspendTransaction PlayerStatus.NoActiveEvent
        val today = clock.today()
        val collectedToday = CollectionRepository.countOnDay(event.id, player, today)

        when (event.mode) {
            EventMode.DAILY -> PlayerStatus.Daily(
                event = event,
                position = AssignmentRepository.find(event.id, player, today)
                    ?.let { MarkerRepository.findById(it) }
                    ?.position,
                collectedToday = collectedToday > 0,
                nothingLeft = openPackages(event.id, player).isEmpty(),
            )

            EventMode.COLLECT -> {
                val found = CollectionRepository.count(event.id, player)
                PlayerStatus.Collect(
                    event = event,
                    found = found,
                    total = StatusRules.collectTotal(
                        found = found,
                        foundStillMarked = CollectionRepository.collectedMarkerIds(event.id, player).size,
                        markerCount = MarkerRepository.count(event.id),
                    ),
                    remainingToday = StatusRules.remainingToday(event.dailyLimit, collectedToday),
                )
            }
        }
    }

    private sealed interface Check {
        data class Denied(val result: PreviewResult) : Check
        data class Allowed(val event: SeasonalEvent, val today: LocalDate, val candidates: List<PackageRef>) : Check
    }

    /** Runs every rule shared by [previewCollect] and [collect]. */
    private suspend fun check(player: UUID, markerId: ULong): Check {
        val event = events.getActiveEvent()
        val today = clock.today()
        val marker = MarkerRepository.findById(markerId)?.takeIf { event != null && it.eventId == event.id }
        val collectedToday = event?.let { CollectionRepository.countOnDay(it.id, player, today) } ?: 0

        val ownsMarker: Boolean
        val alreadyCollected: Boolean
        when (event?.mode) {
            EventMode.DAILY -> {
                ownsMarker = marker != null && AssignmentRepository.find(event.id, player, today) == markerId
                alreadyCollected = collectedToday > 0
            }

            EventMode.COLLECT -> {
                ownsMarker = marker != null
                alreadyCollected = markerId in CollectionRepository.collectedMarkerIds(event.id, player)
            }

            null -> {
                ownsMarker = false
                alreadyCollected = false
            }
        }

        CollectRules.check(
            event = event,
            ownsMarker = ownsMarker,
            alreadyCollected = alreadyCollected,
            collectedToday = collectedToday,
        )?.let { return Check.Denied(it) }

        checkNotNull(event) // CollectRules returns NoActiveEvent otherwise
        return Check.Allowed(event, today, candidates(event, player, today))
    }

    /** The filled packages [player] may receive today, following [PackagePicker]. */
    private suspend fun candidates(event: SeasonalEvent, player: UUID, today: LocalDate): List<PackageRef> =
        PackagePicker.candidates(
            mode = event.mode,
            packages = LootRepository.findFilledRefs(event.id),
            receivedIds = CollectionRepository.receivedPackageIds(event.id, player),
            today = today,
        )

    /** Filled packages that [player] has not received yet, regardless of fixed days. */
    private suspend fun openPackages(eventId: ULong, player: UUID): List<PackageRef> {
        val received = CollectionRepository.receivedPackageIds(eventId, player)
        return LootRepository.findFilledRefs(eventId).filter { it.id !in received }
    }

    private fun PreviewResult.toCollectResult(): CollectResult = when (this) {
        PreviewResult.NotYours -> CollectResult.NotYours
        PreviewResult.AlreadyCollected -> CollectResult.AlreadyCollected
        PreviewResult.DailyLimitReached -> CollectResult.DailyLimitReached
        PreviewResult.NothingLeft -> CollectResult.NothingLeft
        PreviewResult.NoActiveEvent -> CollectResult.NoActiveEvent
        is PreviewResult.Preview -> error("A preview is not a denial")
    }
}
