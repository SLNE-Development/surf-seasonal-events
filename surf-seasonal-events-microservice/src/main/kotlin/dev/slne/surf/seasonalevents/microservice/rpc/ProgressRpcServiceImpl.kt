package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.progress.NpcReward
import dev.slne.surf.seasonalevents.api.progress.NpcStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerView
import dev.slne.surf.seasonalevents.api.result.CollectResult
import dev.slne.surf.seasonalevents.api.result.PreviewResult
import dev.slne.surf.seasonalevents.api.result.RewardResult
import dev.slne.surf.seasonalevents.core.rpc.EventRpcService
import dev.slne.surf.seasonalevents.core.rpc.ProgressRpcService
import dev.slne.surf.seasonalevents.microservice.db.repository.AssignmentRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.CollectionRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.MarkerRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.NpcVisitRepository
import dev.slne.surf.seasonalevents.microservice.logic.CollectRules
import dev.slne.surf.seasonalevents.microservice.logic.DailySchedule
import dev.slne.surf.seasonalevents.microservice.logic.DedupKeys
import dev.slne.surf.seasonalevents.microservice.logic.NpcRules
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
 * Microservice side of [ProgressRpcService]: daily assignments, pinned days, previews, booking collections
 * and the event's NPC. Package items are only loaded for the package that is previewed or handed out.
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
     * Serializes [collect] and [claimReward] per player, so parallel clicks cannot both pass the checks and
     * receive the same package twice. Valid because only one microservice instance runs.
     */
    private val collectLocks = Array(64) { Mutex() }

    private fun lockFor(player: UUID) = collectLocks[(player.hashCode() and Int.MAX_VALUE) % collectLocks.size]

    override suspend fun getPlayerView(player: UUID): PlayerView? = suspendTransaction {
        val event = events.getActiveEvent() ?: return@suspendTransaction null
        if (mustVisitNpcFirst(event, player)) return@suspendTransaction PlayerView(event, emptyList())
        val markers = when (event.mode) {
            EventMode.DAILY -> dailyMarkers(event, player, clock.today())
            EventMode.COLLECT -> {
                val collected = CollectionRepository.collectedMarkerIds(event.id, player)
                MarkerRepository.findByEvent(event.id).filter { it.id !in collected }
            }
        }
        PlayerView(event, markers)
    }

    /** Whether [event] hides every block from [player] until they talked to its NPC once. */
    private suspend fun mustVisitNpcFirst(event: SeasonalEvent, player: UUID): Boolean =
        event.npcVisitRequired && !NpcVisitRepository.hasVisited(event.id, player)

    /**
     * The markers [player] sees in a `DAILY` event: every pinned marker up to today they have not found,
     * plus, on a day nothing is pinned to, their marker of the random pool.
     */
    private suspend fun dailyMarkers(event: SeasonalEvent, player: UUID, today: LocalDate): List<EventMarker> {
        val markers = MarkerRepository.findByEvent(event.id)
        val keys = CollectionRepository.dedupKeys(event.id, player)
        val pinned = DailySchedule.visible(markers, today, DedupKeys.pinnedDays(keys))
        if (DailySchedule.isPinned(markers, today)) return pinned
        return pinned + listOfNotNull(randomMarker(event, player, today, markers, keys))
    }

    /**
     * Returns today's random-pool marker of [player], creating the assignment on the first view of the day.
     * Null if the player already found it today, the pool is empty or no package is left for them today.
     */
    private suspend fun randomMarker(
        event: SeasonalEvent,
        player: UUID,
        today: LocalDate,
        markers: List<EventMarker>,
        keys: Set<String>,
    ): EventMarker? {
        if (DedupKeys.random(today) in keys) return null
        val pool = markers.filter { it.day == null }

        // A marker pinned after it was assigned is no longer part of the pool.
        AssignmentRepository.find(event.id, player, today)?.let { assigned -> return pool.find { it.id == assigned } }

        if (pool.isEmpty() || candidates(event, player, today).isEmpty()) return null

        // Prefer markers the player has not found on earlier days; fall back to the whole pool.
        val collected = CollectionRepository.collectedMarkerIds(event.id, player)
        val markerId = pool.filter { it.id !in collected }.ifEmpty { pool }.random(random).id

        AssignmentRepository.insert(event.id, player, today, markerId)
        // Read back: a parallel first view may have inserted a different marker.
        val assigned = AssignmentRepository.find(event.id, player, today) ?: return null
        return pool.find { it.id == assigned }
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
                    dedupKey = CollectionRepository.dedupKey(check.event.mode, check.today, check.marker),
                )
                if (booked) CollectResult.Success(lootPackage) else CollectResult.AlreadyCollected
            }
        }

    override suspend fun getStatus(player: UUID): PlayerStatus = suspendTransaction {
        val event = events.getActiveEvent() ?: return@suspendTransaction PlayerStatus.NoActiveEvent
        val today = clock.today()
        val found = CollectionRepository.count(event.id, player)

        when (event.mode) {
            EventMode.DAILY -> {
                val markers = MarkerRepository.findByEvent(event.id)
                val keys = CollectionRepository.dedupKeys(event.id, player)
                val pinnedToday = DailySchedule.isPinned(markers, today)
                val open = openPackages(event.id, player).size
                PlayerStatus.Daily(
                    event = event,
                    today = StatusRules.dailyToday(
                        collectedToday = if (pinnedToday) DedupKeys.pinned(today) in keys else DedupKeys.random(today) in keys,
                        hasBlock = pinnedToday || AssignmentRepository.find(event.id, player, today)
                            ?.let { assigned -> markers.any { it.id == assigned && it.day == null } } == true,
                        openPackages = open,
                    ),
                    found = found,
                    total = found + open,
                    openEarlier = DailySchedule.openEarlier(
                        DailySchedule.pinnedDays(markers),
                        today,
                        DedupKeys.pinnedDays(keys),
                    ),
                    npcVisitNeeded = mustVisitNpcFirst(event, player),
                )
            }

            EventMode.COLLECT -> PlayerStatus.Collect(
                event = event,
                found = found,
                total = StatusRules.collectTotal(
                    found = found,
                    foundStillMarked = CollectionRepository.collectedMarkerIds(event.id, player).size,
                    markerCount = MarkerRepository.count(event.id),
                ),
                remainingToday = StatusRules.remainingToday(
                    event.dailyLimit,
                    CollectionRepository.countOnDay(event.id, player, today),
                ),
                npcVisitNeeded = mustVisitNpcFirst(event, player),
            )
        }
    }

    override suspend fun visitNpc(player: UUID, eventName: String): NpcStatus = suspendTransaction {
        val event = events.getActiveEvent()?.takeIf { it.name == eventName }
            ?: return@suspendTransaction NpcStatus.NoActiveEvent
        val today = clock.today()
        val firstVisit = NpcVisitRepository.recordVisit(event.id, player, clock.now())
        val progress = npcProgress(event, player)

        NpcStatus.Active(
            event = event,
            firstVisit = firstVisit,
            today = NpcRules.today(progress.pinnedDays, progress.foundDays, today),
            openEarlier = DailySchedule.openEarlier(progress.pinnedDays, today, progress.foundDays),
            found = progress.foundDays.size,
            total = NpcRules.total(progress.pinnedDays, progress.foundDays),
            reward = progress.reward(today),
        )
    }

    override suspend fun previewReward(player: UUID): RewardResult = suspendTransaction {
        when (val check = checkReward(player)) {
            is RewardCheck.Denied -> check.result
            is RewardCheck.Allowed -> LootRepository.findById(check.reward.id)
                ?.takeIf { it.items.isNotEmpty() }
                ?.let { RewardResult.Preview(it.id, it.items) }
                ?: RewardResult.NoReward
        }
    }

    override suspend fun claimReward(player: UUID, packageId: ULong): RewardResult = lockFor(player).withLock {
        suspendTransaction {
            val check = when (val check = checkReward(player)) {
                is RewardCheck.Denied -> return@suspendTransaction check.result
                is RewardCheck.Allowed -> check
            }
            if (check.reward.id != packageId) return@suspendTransaction RewardResult.PackageUnavailable
            val lootPackage = LootRepository.findById(packageId)?.takeIf { it.items.isNotEmpty() }
                ?: return@suspendTransaction RewardResult.PackageUnavailable

            val booked = CollectionRepository.insert(
                eventId = check.event.id,
                player = player,
                markerId = null,
                packageId = packageId,
                day = check.today,
                dedupKey = DedupKeys.NPC_REWARD,
            )
            if (booked) RewardResult.Granted(lootPackage) else RewardResult.AlreadyClaimed
        }
    }

    /** What the NPC needs to know about [player]: pinned days, found days and the reward package. */
    private class NpcProgress(
        val pinnedDays: Set<LocalDate>,
        val foundDays: Set<LocalDate>,
        val rewardClaimed: Boolean,
        val reward: PackageRef?,
    ) {
        fun reward(today: LocalDate): NpcReward =
            NpcRules.reward(reward?.fixedDay, rewardClaimed, pinnedDays, foundDays, today)
    }

    private suspend fun npcProgress(event: SeasonalEvent, player: UUID): NpcProgress {
        val keys = CollectionRepository.dedupKeys(event.id, player)
        return NpcProgress(
            pinnedDays = DailySchedule.pinnedDays(MarkerRepository.findByEvent(event.id)),
            foundDays = DedupKeys.pinnedDays(keys),
            rewardClaimed = DedupKeys.NPC_REWARD in keys,
            reward = LootRepository.findFilledRefs(event.id).firstOrNull { it.npcReward },
        )
    }

    private sealed interface RewardCheck {
        data class Denied(val result: RewardResult) : RewardCheck
        data class Allowed(val event: SeasonalEvent, val today: LocalDate, val reward: PackageRef) : RewardCheck
    }

    /** Runs every rule shared by [previewReward] and [claimReward]. */
    private suspend fun checkReward(player: UUID): RewardCheck {
        val event = events.getActiveEvent() ?: return RewardCheck.Denied(RewardResult.NoActiveEvent)
        val today = clock.today()
        val progress = npcProgress(event, player)
        return when (val reward = progress.reward(today)) {
            NpcReward.None -> RewardCheck.Denied(RewardResult.NoReward)
            NpcReward.Claimed -> RewardCheck.Denied(RewardResult.AlreadyClaimed)
            is NpcReward.Waiting -> RewardCheck.Denied(RewardResult.NotYet(reward.day, reward.missing))
            NpcReward.Ready -> RewardCheck.Allowed(event, today, checkNotNull(progress.reward))
        }
    }

    private sealed interface Check {
        data class Denied(val result: PreviewResult) : Check
        data class Allowed(
            val event: SeasonalEvent,
            val today: LocalDate,
            val marker: EventMarker,
            val candidates: List<PackageRef>,
        ) : Check
    }

    /** Runs every rule shared by [previewCollect] and [collect]. */
    private suspend fun check(player: UUID, markerId: ULong): Check {
        val event = events.getActiveEvent()
        val today = clock.today()
        val marker = MarkerRepository.findById(markerId)?.takeIf { event != null && it.eventId == event.id }
        val keys = event?.let { CollectionRepository.dedupKeys(it.id, player) }.orEmpty()

        val ownsMarker: Boolean
        val alreadyCollected: Boolean
        var collectedToday = 0
        when (event?.mode) {
            EventMode.DAILY -> {
                val pinnedDay = marker?.day
                if (pinnedDay != null) {
                    // A pinned marker belongs to everyone from its day on, also on any later day.
                    ownsMarker = !pinnedDay.isAfter(today)
                    alreadyCollected = DedupKeys.pinned(pinnedDay) in keys
                } else {
                    ownsMarker = marker != null && AssignmentRepository.find(event.id, player, today) == markerId
                    alreadyCollected = DedupKeys.random(today) in keys
                }
            }

            EventMode.COLLECT -> {
                ownsMarker = marker != null
                alreadyCollected = DedupKeys.marker(markerId) in keys
                collectedToday = CollectionRepository.countOnDay(event.id, player, today)
            }

            null -> {
                ownsMarker = false
                alreadyCollected = false
            }
        }

        CollectRules.check(
            event = event,
            // Blocks are hidden until the NPC visit, so they cannot be collected before it either.
            ownsMarker = ownsMarker && (event == null || !mustVisitNpcFirst(event, player)),
            alreadyCollected = alreadyCollected,
            collectedToday = collectedToday,
        )?.let { return Check.Denied(it) }

        // CollectRules returns NoActiveEvent or NotYours otherwise.
        checkNotNull(event)
        checkNotNull(marker)
        // A caught-up pinned marker gets the package of its own day, not today's.
        return Check.Allowed(event, today, marker, candidates(event, player, marker.day ?: today))
    }

    /** The filled packages [player] may receive for [day], following [PackagePicker]. */
    private suspend fun candidates(event: SeasonalEvent, player: UUID, day: LocalDate): List<PackageRef> =
        PackagePicker.candidates(
            mode = event.mode,
            packages = LootRepository.findFilledRefs(event.id),
            receivedIds = CollectionRepository.receivedPackageIds(event.id, player),
            today = day,
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
