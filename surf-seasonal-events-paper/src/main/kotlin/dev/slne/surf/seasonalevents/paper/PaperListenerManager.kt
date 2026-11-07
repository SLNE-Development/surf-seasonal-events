package dev.slne.surf.seasonalevents.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.seasonalevents.paper.listener.ChunkLoadListener
import dev.slne.surf.seasonalevents.paper.listener.FakeBlockClickListener
import dev.slne.surf.seasonalevents.paper.listener.FakeBlockPacketListener
import dev.slne.surf.seasonalevents.paper.listener.LootEditorListener
import dev.slne.surf.seasonalevents.paper.listener.MarkerProtectionListener
import dev.slne.surf.seasonalevents.paper.listener.MarkerToolListener
import dev.slne.surf.seasonalevents.paper.listener.PlayerConnectionListener

/** Registers every Bukkit and PacketEvents listener of the plugin. */
object PaperListenerManager {

    private val packetListeners = mutableListOf<PacketListenerCommon>()

    fun registerAll() {
        PlayerConnectionListener.register()
        ChunkLoadListener.register()
        MarkerProtectionListener.register()
        MarkerToolListener.register()
        LootEditorListener.register()

        val packetEvents = PacketEvents.getAPI().eventManager
        packetListeners += packetEvents.registerListener(FakeBlockPacketListener, PacketListenerPriority.HIGH)
        packetListeners += packetEvents.registerListener(FakeBlockClickListener, PacketListenerPriority.NORMAL)
    }

    /** PacketEvents keeps its listeners across a plugin reload, so they are removed explicitly. */
    fun unregisterAll() {
        packetListeners.forEach { PacketEvents.getAPI().eventManager.unregisterListener(it) }
        packetListeners.clear()
    }
}
