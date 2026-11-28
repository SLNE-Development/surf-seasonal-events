package dev.slne.surf.seasonalevents.paper.hud

import dev.slne.surf.hud.api.SurfHud
import dev.slne.surf.hud.api.hud
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * The only code that touches surf-hud classes. Only call it when surf-hud is enabled, otherwise its
 * classes are missing at runtime. Call it on the player's thread.
 */
internal object SurfHudBridge {

    private const val ELEMENT_ID = "seasonal-events-progress"

    /** HUD line of the element, centered. */
    private const val LINE = 0

    /** Shows [content] as this plugin's HUD element of [player], replacing the previous one. */
    fun show(player: Player, content: Component) {
        val hud = player.hud
        hud.remove(ELEMENT_ID)
        hud.add(ELEMENT_ID, LINE, 0, content)
    }

    fun hide(player: Player) {
        SurfHud.hudOrNull(player.uniqueId)?.remove(ELEMENT_ID)
    }
}
