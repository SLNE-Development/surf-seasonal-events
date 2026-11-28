package dev.slne.surf.seasonalevents.paper.npc

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.seasonalevents.paper.collect.ClickCooldown
import dev.slne.surf.seasonalevents.paper.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.time.Duration.Companion.seconds

/** Starts a talk when a player clicks an NPC linked to an event. Only registered when [EventNpcs.enabled]. */
internal object NpcInteractListener : Listener {

    /** A click opens a dialog, so one per second is plenty and keeps spam away from the microservice. */
    private val cooldown = ClickCooldown(1.seconds)

    @EventHandler
    fun onInteract(event: NpcInteractEvent) {
        val eventId = NpcLinks.eventIdOf(event.npc) ?: return
        val player = event.player
        if (!cooldown.tryAcquire(player.uniqueId)) return

        val npcName = event.npc.uniqueName
        val speaker = event.npc.getDisplayName()
        plugin.launch { NpcConversation.talk(player, eventId, npcName, speaker) }
    }
}
