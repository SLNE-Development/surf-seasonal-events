package dev.slne.surf.seasonalevents.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.seasonalevents.paper.listener.FakeBlockPacketListener
import dev.slne.surf.api.paper.extensions.pluginManager
import dev.slne.surf.seasonalevents.core.client.rabbit.RabbitService
import dev.slne.surf.seasonalevents.paper.command.seasonalEventsCommand
import dev.slne.surf.seasonalevents.paper.listener.ChunkLoadListener
import dev.slne.surf.seasonalevents.paper.listener.MarkerProtectionListener
import dev.slne.surf.seasonalevents.paper.listener.PlayerConnectionListener
import dev.slne.surf.seasonalevents.paper.render.ViewService
import org.bukkit.plugin.java.JavaPlugin

/**
 * Paper entry point of surf-seasonal-events.
 */
class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onEnableAsync() {
        RabbitService.connect(dataPath)
        seasonalEventsCommand()

        pluginManager.registerEvents(PlayerConnectionListener, this)
        pluginManager.registerEvents(ChunkLoadListener, this)
        pluginManager.registerEvents(MarkerProtectionListener, this)
        PacketEvents.getAPI().eventManager.registerListener(FakeBlockPacketListener, PacketListenerPriority.HIGH)

        // Also covers a reload with players online.
        ViewService.reloadAll()
    }

    override suspend fun onDisableAsync() {
        RabbitService.disconnect()
    }
}

/** The running plugin instance. */
val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)
