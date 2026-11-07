package dev.slne.surf.seasonalevents.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.seasonalevents.core.client.rabbit.RabbitService
import dev.slne.surf.seasonalevents.paper.command.seasonalEventsCommand
import org.bukkit.plugin.java.JavaPlugin

/**
 * Paper entry point of surf-seasonal-events.
 */
class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onEnableAsync() {
        RabbitService.connect(dataPath)
        seasonalEventsCommand()
    }

    override suspend fun onDisableAsync() {
        RabbitService.disconnect()
    }
}

/** The running plugin instance. */
val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)
