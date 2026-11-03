package dev.slne.surf.seasonalevents.listeners

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.seasonalevents.config.gift.GiftService
import dev.slne.surf.seasonalevents.db.dailygift.DailyGift
import dev.slne.surf.seasonalevents.db.dailygift.DailyGiftService
import dev.slne.surf.seasonalevents.plugin
import dev.slne.surf.seasonalevents.services.activeGiftEntities
import dev.slne.surf.seasonalevents.services.visualEntities
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.core.api.messages.adventure.playSound
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.Particle
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.bukkit.Sound as BukkitSound

object PlayerInteractListener : PacketListener {

    private val processingPlayers = Caffeine.newBuilder()
        .expireAfterWrite(5.seconds.toJavaDuration())
        .build<UUID, Unit>()

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType != PacketType.Play.Client.INTERACT_ENTITY) return
        val player = server.getPlayer(event.user.uuid) ?: return
        val wrapper = WrapperPlayClientInteractEntity(event)

//        println("Geklickte ID: ${wrapper.entityId} | Aktion: ${wrapper.action}")
        val storedInteractionId = activeGiftEntities[player.uniqueId] ?: return
        if (wrapper.entityId != storedInteractionId) return

        if (processingPlayers.getIfPresent(player.uniqueId) != null) {
            event.isCancelled = true
            return
        }

        processingPlayers.put(player.uniqueId, Unit)

        plugin.launch {
            val todayGift: DailyGift? = DailyGiftService.getTodayGift(player.uniqueId)

            if (todayGift == null) {
                player.sendText {
                    appendErrorPrefix()
                    text("Du hast bereits alle Geschenke eingesammelt!")
                }
                return@launch
            }

            val linkedGift = GiftService.getById(todayGift.giftId) ?: return@launch
            val itemsToGive = linkedGift.content.map { it.clone() }

            val freeSlots = player.inventory.storageContents.count { it == null || it.type == Material.AIR }

            val requiredSlots = itemsToGive.count { it.maxStackSize == 1 || it.amount > 1 }

            if (freeSlots < requiredSlots) {
                player.sendText {
                    appendErrorPrefix()
                    error("In deinem Inventar ist zu wenig Platz, um dieses Geschenk zu öffnen!")
                }
                return@launch
            }
            itemsToGive.forEach { item ->
                player.inventory.addItem(item)
            }

            DailyGiftService.markTodaysAsClaimed(player.uniqueId)

            val armorStandId = visualEntities[player.uniqueId]
            val ids = mutableListOf(storedInteractionId)
            if (armorStandId != null) ids.add(armorStandId)

            val destroy = WrapperPlayServerDestroyEntities(*ids.toIntArray())
            PacketEvents.getAPI().playerManager.sendPacket(player, destroy)

            activeGiftEntities.remove(player.uniqueId)
            visualEntities.remove(player.uniqueId)

            player.sendText {
                appendSuccessPrefix()
                success("Du hast das Geschenk")
                appendSpace()
                variableValue(linkedGift.name)
                appendSpace()
                success("erhalten!")
            }

            player.playSound(true) {
                type(BukkitSound.ENTITY_PLAYER_LEVELUP)
                source(Sound.Source.NEUTRAL)
                pitch(0.5f)
            }

            player.spawnParticle(
                Particle.ENCHANT,
                todayGift.location.clone().add(0.5, 0.5, 0.5),
                50,
                0.3, 0.3, 0.3, 0.01
            )
        }
    }
}