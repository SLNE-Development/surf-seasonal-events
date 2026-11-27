package dev.slne.surf.seasonalevents.core

import dev.slne.surf.api.core.serializer.SurfSerializerModule
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.api.progress.DailyToday
import dev.slne.surf.seasonalevents.api.progress.NpcReward
import dev.slne.surf.seasonalevents.api.progress.NpcStatus
import dev.slne.surf.seasonalevents.api.progress.NpcToday
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerView
import dev.slne.surf.seasonalevents.api.result.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Same serializers surf-rabbitmq uses: the surf-api defaults. */
@OptIn(ExperimentalSerializationApi::class)
private val cbor = Cbor {
    serializersModule = SurfSerializerModule.all
}

/** Encodes [value] to bytes and decodes it again, like a trip through RabbitMQ. */
private inline fun <reified T> roundtrip(value: T): T =
    cbor.decodeFromByteArray(cbor.encodeToByteArray(value))

/**
 * Verifies that every type sent between plugin and microservice survives serialization unchanged.
 */
class SerializationRoundtripTest {

    private val world = UUID.fromString("c926568e-d50f-4b97-851f-65415c131e3b")

    private val event = SeasonalEvent(
        id = 1u,
        name = "christmas-2026",
        displayName = Component.text("Adventskalender", NamedTextColor.RED),
        mode = EventMode.DAILY,
        state = EventState.ACTIVE,
        markerLimit = 24,
        dailyLimit = 0,
        endsAt = Instant.parse("2026-12-24T22:59:00Z"),
        startedAt = Instant.parse("2026-12-01T00:00:00Z"),
        createdAt = Instant.parse("2026-11-04T10:00:00Z"),
    )

    private val marker = EventMarker(
        id = 7u,
        eventId = 1u,
        position = MarkerPosition(world, 258, 123, -120),
        blockData = "minecraft:player_head[rotation=4]",
        skullTexture = "eyJ0ZXh0dXJlcyI6e30=",
    )

    private val lootPackage = LootPackage(
        id = 3u,
        eventId = 1u,
        name = "Tag 24",
        items = byteArrayOf(1, 2, 3, 4),
        fixedDay = LocalDate.of(2026, 12, 24),
    )

    // ---------- domain types ----------

    @Test
    fun `seasonal event survives roundtrip`() {
        assertEquals(event, roundtrip(event))
    }

    @Test
    fun `seasonal event with null dates survives roundtrip`() {
        val draft = event.copy(state = EventState.DRAFT, endsAt = null, startedAt = null)
        assertEquals(draft, roundtrip(draft))
    }

    @Test
    fun `marker position keeps its world uuid`() {
        val position = MarkerPosition(world, 10, 64, -20)
        assertEquals(position, roundtrip(position))
    }

    @Test
    fun `marker with and without skull texture survives roundtrip`() {
        assertEquals(marker, roundtrip(marker))

        val pumpkin = marker.copy(blockData = "minecraft:carved_pumpkin[facing=north]", skullTexture = null)
        assertEquals(pumpkin, roundtrip(pumpkin))
    }

    @Test
    fun `loot package keeps all fields including its bytes`() {
        val result = roundtrip(lootPackage)

        // LootPackage.equals only compares the id, so every field is checked on its own.
        assertEquals(lootPackage.id, result.id)
        assertEquals(lootPackage.eventId, result.eventId)
        assertEquals(lootPackage.name, result.name)
        assertEquals(lootPackage.fixedDay, result.fixedDay)
        assertContentEquals(lootPackage.items, result.items)
    }

    @Test
    fun `loot package without fixed day survives roundtrip`() {
        val random = LootPackage(4u, 1u, "Zufall", byteArrayOf(), null)
        val result = roundtrip(random)
        assertEquals(null, result.fixedDay)
        assertContentEquals(random.items, result.items)
    }

    @Test
    fun `player view survives roundtrip`() {
        val view = PlayerView(event, listOf(marker))
        assertEquals(view, roundtrip(view))
    }

    @Test
    fun `every player status case survives roundtrip`() {
        val cases: List<PlayerStatus> = listOf(
            PlayerStatus.NoActiveEvent,
            *DailyToday.entries.map { PlayerStatus.Daily(event, it, found = 3, total = 24) }.toTypedArray(),
            PlayerStatus.Collect(event.copy(mode = EventMode.COLLECT), found = 12, total = 40, remainingToday = 3),
            PlayerStatus.Collect(event.copy(mode = EventMode.COLLECT), found = 0, total = 40, remainingToday = null),
        )
        cases.forEach { assertEquals(it, roundtrip<PlayerStatus>(it)) }
    }

    // ---------- results ----------
    // Each list is typed with the sealed interface, so the case is encoded and restored polymorphically.

    @Test
    fun `every create event result case survives roundtrip`() {
        val cases: List<CreateEventResult> = listOf(
            CreateEventResult.Success(event),
            CreateEventResult.NameTaken,
            CreateEventResult.InvalidName,
            CreateEventResult.InvalidLimit,
        )
        cases.forEach { assertEquals(it, roundtrip<CreateEventResult>(it)) }
    }

    @Test
    fun `every update event result case survives roundtrip`() {
        val cases: List<UpdateEventResult> = listOf(
            UpdateEventResult.Success(event),
            UpdateEventResult.NotFound,
            UpdateEventResult.AlreadyEnded,
            UpdateEventResult.InvalidDisplayName,
            UpdateEventResult.NotEnoughPackages(filled = 20, needed = 24),
        )
        cases.forEach { assertEquals(it, roundtrip<UpdateEventResult>(it)) }
    }

    @Test
    fun `every start event result case survives roundtrip`() {
        val cases: List<StartEventResult> = listOf(
            StartEventResult.Success(event),
            StartEventResult.AnotherEventActive("halloween-2026"),
            StartEventResult.AlreadyEnded,
            StartEventResult.NoMarkers,
            StartEventResult.NoPackages,
            StartEventResult.NotEnoughPackages(filled = 30, needed = 40),
            StartEventResult.NotFound,
        )
        cases.forEach { assertEquals(it, roundtrip<StartEventResult>(it)) }
    }

    @Test
    fun `every stop event result case survives roundtrip`() {
        val cases: List<StopEventResult> = listOf(
            StopEventResult.Success(event.copy(state = EventState.ENDED)),
            StopEventResult.NoActiveEvent,
        )
        cases.forEach { assertEquals(it, roundtrip<StopEventResult>(it)) }
    }

    @Test
    fun `every delete event result case survives roundtrip`() {
        val cases: List<DeleteEventResult> = listOf(
            DeleteEventResult.Success,
            DeleteEventResult.IsActive,
            DeleteEventResult.HasMarkers(markerCount = 12),
            DeleteEventResult.NotFound,
        )
        cases.forEach { assertEquals(it, roundtrip<DeleteEventResult>(it)) }
    }

    @Test
    fun `every mark result case survives roundtrip`() {
        val cases: List<MarkResult> = listOf(
            MarkResult.Success(marker, markerCount = 3, markerLimit = 4),
            MarkResult.LimitReached(24),
            MarkResult.AlreadyMarked,
            MarkResult.NotEnoughPackages(filled = 40, needed = 41),
            MarkResult.EventEnded,
            MarkResult.NotFound,
        )
        cases.forEach { assertEquals(it, roundtrip<MarkResult>(it)) }
    }

    @Test
    fun `every unmark result case survives roundtrip`() {
        val cases: List<UnmarkResult> = listOf(
            UnmarkResult.Success(marker, markerCount = 2, markerLimit = 4),
            UnmarkResult.NotMarked,
        )
        cases.forEach { assertEquals(it, roundtrip<UnmarkResult>(it)) }
    }

    @Test
    fun `every restore result case survives roundtrip`() {
        val cases: List<RestoreResult> = listOf(
            RestoreResult.Success(listOf(marker, marker.copy(id = 8u))),
            RestoreResult.Success(emptyList()),
            RestoreResult.IsActive,
            RestoreResult.NotFound,
        )
        cases.forEach { assertEquals(it, roundtrip<RestoreResult>(it)) }
    }

    @Test
    fun `every update loot result case survives roundtrip`() {
        val success = roundtrip<UpdateLootResult>(UpdateLootResult.Success(lootPackage))
        val restored = assertIs<UpdateLootResult.Success>(success).lootPackage
        assertContentEquals(lootPackage.items, restored.items)
        assertEquals(lootPackage.fixedDay, restored.fixedDay)

        val cases: List<UpdateLootResult> = listOf(
            UpdateLootResult.NotFound,
            UpdateLootResult.FixedDayTaken,
            UpdateLootResult.NotDailyEvent,
            UpdateLootResult.EventEnded,
            UpdateLootResult.Conflict,
            UpdateLootResult.NotEnoughPackages(filled = 39, needed = 40),
        )
        cases.forEach { assertEquals(it, roundtrip<UpdateLootResult>(it)) }
    }

    @Test
    fun `every delete loot result case survives roundtrip`() {
        val cases: List<DeleteLootResult> = listOf(
            DeleteLootResult.Success,
            DeleteLootResult.NotFound,
            DeleteLootResult.EventEnded,
            DeleteLootResult.NotEnoughPackages(filled = 39, needed = 40),
        )
        cases.forEach { assertEquals(it, roundtrip<DeleteLootResult>(it)) }
    }

    @Test
    fun `marker positions survive roundtrip as a list`() {
        val positions = listOf(marker.position, MarkerPosition(world, 0, -64, 0))
        assertEquals(positions, roundtrip(positions))
    }

    @Test
    fun `every preview result case survives roundtrip`() {
        val cases: List<PreviewResult> = listOf(
            PreviewResult.Preview(packageId = 3u, items = byteArrayOf(9, 8, 7)),
            PreviewResult.NotYours,
            PreviewResult.AlreadyCollected,
            PreviewResult.DailyLimitReached,
            PreviewResult.NothingLeft,
            PreviewResult.NoActiveEvent,
        )
        // Preview implements equals with contentEquals, so the bytes are compared as well.
        cases.forEach { assertEquals(it, roundtrip<PreviewResult>(it)) }
    }

    @Test
    fun `every collect result case survives roundtrip`() {
        val success = roundtrip<CollectResult>(CollectResult.Success(lootPackage))
        val restored = assertIs<CollectResult.Success>(success).lootPackage
        assertContentEquals(lootPackage.items, restored.items)

        val cases: List<CollectResult> = listOf(
            CollectResult.NotYours,
            CollectResult.AlreadyCollected,
            CollectResult.DailyLimitReached,
            CollectResult.NothingLeft,
            CollectResult.NoActiveEvent,
            CollectResult.PackageUnavailable,
        )
        cases.forEach { assertEquals(it, roundtrip<CollectResult>(it)) }
    }

    @Test
    fun `pinned marker keeps its day`() {
        val pinned = marker.copy(day = LocalDate.of(2026, 12, 5))
        assertEquals(pinned, roundtrip(pinned))
    }

    @Test
    fun `npc reward flag survives roundtrip`() {
        val reward = LootPackage(5u, 1u, "Geschenk 24", byteArrayOf(1), LocalDate.of(2026, 12, 24), npcReward = true)
        assertEquals(true, roundtrip(reward).npcReward)
    }

    @Test
    fun `npc visit requirement survives roundtrip`() {
        val gated = event.copy(npcVisitRequired = true)
        assertEquals(gated, roundtrip(gated))
        val daily: PlayerStatus = PlayerStatus.Daily(gated, DailyToday.HIDDEN, found = 0, total = 24, npcVisitNeeded = true)
        val collect: PlayerStatus = PlayerStatus.Collect(gated, found = 0, total = 10, remainingToday = null, npcVisitNeeded = true)
        assertEquals(daily, roundtrip<PlayerStatus>(daily))
        assertEquals(collect, roundtrip<PlayerStatus>(collect))
    }

    @Test
    fun `daily status keeps open earlier days`() {
        val status: PlayerStatus = PlayerStatus.Daily(event, DailyToday.HIDDEN, found = 2, total = 24, openEarlier = 3)
        assertEquals(status, roundtrip<PlayerStatus>(status))
    }

    @Test
    fun `every npc status case survives roundtrip`() {
        val rewards = listOf(
            NpcReward.None,
            NpcReward.Waiting(LocalDate.of(2026, 12, 24), missing = 4),
            NpcReward.Ready,
            NpcReward.Claimed,
        )
        val cases: List<NpcStatus> = listOf(NpcStatus.NoActiveEvent) + rewards.flatMap { reward ->
            NpcToday.entries.map { today ->
                NpcStatus.Active(event, firstVisit = true, today, openEarlier = 1, found = 5, total = 23, reward)
            }
        }
        cases.forEach { assertEquals(it, roundtrip<NpcStatus>(it)) }
    }

    @Test
    fun `every reward result case survives roundtrip`() {
        val granted = roundtrip<RewardResult>(RewardResult.Granted(lootPackage))
        assertContentEquals(lootPackage.items, assertIs<RewardResult.Granted>(granted).lootPackage.items)

        val cases: List<RewardResult> = listOf(
            RewardResult.Preview(packageId = 3u, items = byteArrayOf(4, 5)),
            RewardResult.NotYet(LocalDate.of(2026, 12, 24), missing = 2),
            RewardResult.AlreadyClaimed,
            RewardResult.NoReward,
            RewardResult.NoActiveEvent,
            RewardResult.PackageUnavailable,
        )
        cases.forEach { assertEquals(it, roundtrip<RewardResult>(it)) }
    }

    @Test
    fun `every marker day result case survives roundtrip`() {
        val cases: List<MarkerDayResult> = listOf(
            MarkerDayResult.Success(marker.copy(day = LocalDate.of(2026, 12, 1))),
            MarkerDayResult.NotFound,
            MarkerDayResult.NotDailyEvent,
            MarkerDayResult.DayTaken,
            MarkerDayResult.EventEnded,
            MarkerDayResult.NotEnoughPackages(filled = 22, needed = 23),
        )
        cases.forEach { assertEquals(it, roundtrip<MarkerDayResult>(it)) }
    }

    @Test
    fun `new update loot result cases survive roundtrip`() {
        val cases: List<UpdateLootResult> = listOf(UpdateLootResult.NpcRewardNeedsFixedDay, UpdateLootResult.NpcRewardTaken)
        cases.forEach { assertEquals(it, roundtrip<UpdateLootResult>(it)) }
    }
}
