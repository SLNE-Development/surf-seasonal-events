package dev.slne.surf.seasonalevents.paper.npc

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.npc.api.SurfNpcApi
import dev.slne.surf.npc.api.npc.Npc
import dev.slne.surf.npc.api.npc.property.NpcProperty
import dev.slne.surf.npc.api.npc.property.NpcPropertyType
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import net.kyori.adventure.text.Component

/**
 * The NPC side of the link between surf-npc NPCs and seasonal events. The event's stored NPC name is the
 * authority; the property on the NPC only holds the event id, so a click can be routed without a call.
 * Only use when [EventNpcs.enabled].
 */
internal object NpcLinks {

    private const val EVENT_PROPERTY = "seasonal_events_event_id"

    /** The id of the event [npc] points to, or null if it is no event NPC. */
    fun eventIdOf(npc: Npc): ULong? = (npc.getProperty(EVENT_PROPERTY)?.value as? String)?.toULongOrNull()

    fun link(npc: Npc, event: SeasonalEvent) =
        npc.addProperty(NpcProperty(EVENT_PROPERTY, event.id.toString(), NpcPropertyType.Types.STRING_TYPE))

    fun unlink(npc: Npc) = npc.removeProperty(EVENT_PROPERTY)

    fun find(uniqueName: String): Npc? = SurfNpcApi.getNpc(uniqueName)

    /** Unique names of every NPC, for command suggestions. */
    fun names(): List<String> = SurfNpcApi.getNpcs().map { it.uniqueName }.sorted()

    /** A warning if the NPC [event] requires no longer exists, so nobody could start the search; else null. */
    fun missingNpcWarning(event: SeasonalEvent): Component? {
        val name = event.npcName ?: return null
        if (find(name) != null) return null
        return buildText {
            appendWarningPrefix()
            warning("Der NPC ")
            variableValue(name)
            warning(" von ${event.name} existiert nicht mehr. Ohne ihn sieht niemand die Blöcke: lege ihn neu an oder entferne die Verknüpfung.")
        }
    }
}
