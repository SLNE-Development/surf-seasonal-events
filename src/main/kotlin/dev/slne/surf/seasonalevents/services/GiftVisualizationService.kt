package dev.slne.surf.seasonalevents.services

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.seasonalevents.db.dailygift.DailyGiftService
import dev.slne.surf.seasonalevents.plugin
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.ktor.util.collections.*
import org.bukkit.entity.Player
import java.util.*

val activeGiftEntities = ConcurrentMap<UUID, Int>()
val visualEntities = ConcurrentMap<UUID, Int>()

object GiftVisualizationService {

    fun visualizeGiftForPlayer(player: Player) {
        plugin.launch {
            removeGiftForPlayer(player)

            val gift = DailyGiftService.getOrCreateTodayGift(player.uniqueId) ?: return@launch
            if (gift.claimed) return@launch

            val interactionId = (Int.MIN_VALUE..-1000000).random()
            val armorStandId = (Int.MIN_VALUE..-1000000).random()

            activeGiftEntities[player.uniqueId] = interactionId
            visualEntities[player.uniqueId] = armorStandId

            val spawnInteraction = WrapperPlayServerSpawnEntity(
                interactionId, Optional.of(UUID.randomUUID()), EntityTypes.INTERACTION,
                Vector3d(gift.location.x + 0.5, gift.location.y, gift.location.z + 0.5), 0f, 0f, 0f, 0, Optional.empty()
            )

            val interactionMetadata = WrapperPlayServerEntityMetadata(
                interactionId, listOf(
                    EntityData(8, EntityDataTypes.FLOAT, 1.0f),
                    EntityData(9, EntityDataTypes.FLOAT, 1.0f)
                )
            )

            val spawnArmorStand = WrapperPlayServerSpawnEntity(
                armorStandId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                Vector3d(gift.location.x + 0.5, gift.location.y - 1.4, gift.location.z + 0.5),
                0f,
                0f,
                0f,
                0,
                Optional.empty()
            )

            val armorStandMetadata = WrapperPlayServerEntityMetadata(
                armorStandId, listOf(
                    EntityData(0, EntityDataTypes.BYTE, 0x20.toByte())
                )
            )

            val packetSkull = SpigotConversionUtil.fromBukkitItemStack(SkullUtil.getRandomPresentHead())
            val equipment = WrapperPlayServerEntityEquipment(
                armorStandId, listOf(Equipment(EquipmentSlot.HELMET, packetSkull))
            )

            val api = PacketEvents.getAPI().playerManager
            api.sendPacket(player, spawnInteraction)
            api.sendPacket(player, interactionMetadata)
            api.sendPacket(player, spawnArmorStand)
            api.sendPacket(player, armorStandMetadata)
            api.sendPacket(player, equipment)

            player.sendText {
                appendSuccessPrefix()
                success("Ein neues")
                appendSpace()
                gold("Tägliches Geschenk")
                appendSpace()
                success("wartet am Spawn auf dich!")
            }
        }
    }

    fun removeGiftForPlayer(player: Player) {
        val interactionId = activeGiftEntities.remove(player.uniqueId)
        val armorId = visualEntities.remove(player.uniqueId)

        val ids = mutableListOf<Int>()
        if (interactionId != null) ids.add(interactionId)
        if (armorId != null) ids.add(armorId)

        if (ids.isNotEmpty()) {
            val destroy = WrapperPlayServerDestroyEntities(*ids.toIntArray())
            PacketEvents.getAPI().playerManager.sendPacket(player, destroy)
        }
    }
}