package dev.slne.surf.seasonalevents.paper.gui

import org.bukkit.inventory.ItemStack
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Converts package contents between items and the opaque bytes stored by the microservice. */
object LootItems {

    /**
     * The `items` column is a MariaDB BLOB, which holds at most 64 KiB. Larger contents are refused
     * instead of being cut off by the database.
     */
    const val MAX_BYTES = 60_000

    private val dayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Empty bytes mean an empty package, which is never handed out. */
    fun encode(items: List<ItemStack>): ByteArray =
        if (items.isEmpty()) ByteArray(0) else ItemStack.serializeItemsAsBytes(items)

    fun decode(bytes: ByteArray): List<ItemStack> =
        if (bytes.isEmpty()) emptyList() else ItemStack.deserializeItemsFromBytes(bytes).filterNot { it.isEmpty }

    fun formatDay(day: LocalDate): String = dayFormat.format(day)
}
