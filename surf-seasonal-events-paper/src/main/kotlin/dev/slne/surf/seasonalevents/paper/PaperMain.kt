package dev.slne.surf.seasonalevents.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.seasonalevents.core.client.rabbit.RabbitService
import dev.slne.surf.seasonalevents.paper.command.seasonalEventsCommand
import dev.slne.surf.seasonalevents.paper.gui.lootOverviewView
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.tick.EventTicker
import org.bukkit.plugin.java.JavaPlugin

/**
 * Paper entry point of surf-seasonal-events.
 */
class PaperMain : SuspendingJavaPlugin() {
    private val ticker = EventTicker()

    override suspend fun onLoadAsync() {
        // Views must be registered before the shared view frame starts, before any suspending call.
        lootOverviewView.register()
    }

    override suspend fun onEnableAsync() {
        RabbitService.connect(dataPath)
        seasonalEventsCommand()
        PaperListenerManager.registerAll()

        // Also covers a reload with players online.
        ViewService.reloadAll()
        ticker.start()
    }

    override suspend fun onDisableAsync() {
        ticker.stop()
        PaperListenerManager.unregisterAll()
        RabbitService.disconnect()
    }
}

/** The running plugin instance. */
val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)
