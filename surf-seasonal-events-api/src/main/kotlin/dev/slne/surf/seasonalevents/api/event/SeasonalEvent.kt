package dev.slne.surf.seasonalevents.api.event

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import java.time.Instant

/** A seasonal event such as Easter, Halloween or Christmas. */
@Serializable
data class SeasonalEvent(
    val id: ULong,
    /** Unique technical name used in commands, e.g. `christmas-2026`. */
    val name: String,
    /** Name shown to players. */
    val displayName: @Contextual Component,
    val mode: EventMode,
    val state: EventState,
    /** Maximum number of blocks that can be marked for this event. */
    val markerLimit: Int,
    /** Maximum collections per player and day in [EventMode.COLLECT]; 0 means unlimited. */
    val dailyLimit: Int,
    /** When the event ends automatically; null if it only ends by a manual stop. */
    val endsAt: @Contextual Instant?,
    /** When the event was started; null while it is still a draft. */
    val startedAt: @Contextual Instant?,
    val createdAt: @Contextual Instant,
    /**
     * Unique name of the surf-npc NPC linked to the event, or null. This is the authoritative link: only
     * this NPC talks for the event. At most one NPC per event.
     */
    val npcName: String? = null,
) {
    /** Whether a player has to talk to the event's NPC once before they see any block: while one is linked. */
    val npcVisitRequired: Boolean get() = npcName != null

    companion object {
        /** Longest allowed [name]; matches the database column. */
        const val MAX_NAME_LENGTH = 64

        /** Longest allowed [displayName], counted in visible characters without formatting. */
        const val MAX_DISPLAY_NAME_LENGTH = 64

        /** Longest allowed [npcName]; matches the database column. */
        const val MAX_NPC_NAME_LENGTH = 64
    }
}
