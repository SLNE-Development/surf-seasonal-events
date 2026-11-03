package dev.slne.surf.seasonalevents.config.gift

import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*
import kotlinx.serialization.Transient
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

@ConfigSerializable
data class Gift(
    val giftId: UUID,
    val name: String,
    var encodedContent: String = ""
) {

    @Transient
    private var cachedContent: List<ItemStack>? = null

    @Transient
    var content: List<ItemStack>
        get() {
            if (cachedContent == null && encodedContent.isNotEmpty()) {
                val bytes = Base64.getDecoder().decode(encodedContent)
                cachedContent = ItemStack.deserializeItemsFromBytes(bytes).toList()
            }
            return cachedContent ?: emptyList()
        }
        set(value) {
            cachedContent = value
            val bytes = serializeItems(value)
            encodedContent = Base64.getEncoder().encodeToString(bytes)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Gift) return false
        return giftId == other.giftId
    }

    override fun hashCode(): Int = giftId.hashCode()
}

fun gift(
    giftId: UUID,
    name: String,
    content: List<ItemStack>
): Gift {
    val gift = Gift(giftId = giftId, name = name)
    gift.content = content
    return gift
}

private fun serializeItems(items: List<ItemStack>): ByteArray = ByteArrayOutputStream().use { arrayOut ->
    DataOutputStream(arrayOut).use { dataOut ->
        dataOut.writeByte(1)
        dataOut.writeInt(items.size)

        for (item in items) {
            if (item.isEmpty) {
                dataOut.writeInt(0)
            } else {
                val itemBytes = item.serializeAsBytes()
                dataOut.writeInt(itemBytes.size)
                dataOut.write(itemBytes)
            }
        }
    }
    arrayOut.toByteArray()
}

private fun deserializeItems(bytes: ByteArray): List<ItemStack> {
    if (bytes.isEmpty()) return emptyList()
    return ItemStack.deserializeItemsFromBytes(bytes).toList()
}