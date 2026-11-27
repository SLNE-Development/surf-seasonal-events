package dev.slne.surf.seasonalevents.paper.npc

import dev.slne.surf.npc.api.SurfNpcApi
import dev.slne.surf.npc.api.npc.Npc
import dev.slne.surf.npc.api.npc.property.NpcProperty
import dev.slne.surf.npc.api.npc.property.NpcPropertyType
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent

/**
 * Links surf-npc NPCs to seasonal events. The link is a property on the NPC holding the event's technical
 * name, so surf-npc stores it with the NPC. Only use when [EventNpcs.enabled].
 */
internal object NpcLinks {

    private const val EVENT_PROPERTY = "seasonal_events_event"

    /** The technical name of the event [npc] belongs to, or null if it is no event NPC. */
    fun eventNameOf(npc: Npc): String? = npc.getProperty(EVENT_PROPERTY)?.value as? String

    fun link(npc: Npc, event: SeasonalEvent) =
        npc.addProperty(NpcProperty(EVENT_PROPERTY, event.name, NpcPropertyType.Types.STRING_TYPE))

    fun unlink(npc: Npc) = npc.removeProperty(EVENT_PROPERTY)

    fun find(uniqueName: String): Npc? = SurfNpcApi.getNpc(uniqueName)

    /** Unique names of every NPC, for command suggestions. */
    fun names(): List<String> = SurfNpcApi.getNpcs().map { it.uniqueName }.sorted()
}
