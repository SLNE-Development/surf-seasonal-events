package dev.slne.surf.seasonalevents.paper.npc

import org.bukkit.Bukkit

/**
 * Whether the optional surf-npc plugin runs. Every class that touches surf-npc ([NpcLinks],
 * [NpcInteractListener], the `npc` subcommand) may only be loaded when [enabled] is true.
 */
object EventNpcs {
    private const val PLUGIN = "surf-npc-paper"

    val enabled get() = Bukkit.getPluginManager().isPluginEnabled(PLUGIN)
}
