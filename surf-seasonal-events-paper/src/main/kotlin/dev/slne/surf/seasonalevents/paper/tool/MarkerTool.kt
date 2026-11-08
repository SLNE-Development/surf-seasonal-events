package dev.slne.surf.seasonalevents.paper.tool

import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.paper.plugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** The item staff use to mark blocks; it carries the id of its event. */
object MarkerTool {

    private val eventIdKey by lazy { NamespacedKey(plugin, "marker_tool_event_id") }

    fun create(event: SeasonalEvent): ItemStack = buildItem(Material.BLAZE_ROD) {
        displayName {
            primary("Marker-Werkzeug: ")
            variableValue(event.name)
        }
        buildLore {
            line { info("Linksklick auf einen Block: markieren") }
            line { info("Rechtsklick auf einen Marker: entmarkieren") }
        }
        editPersistentDataContainer { it.set(eventIdKey, PersistentDataType.LONG, event.id.toLong()) }
    }

    /** The event the tool belongs to, or null if [item] is not a marker tool. */
    fun eventIdOf(item: ItemStack?): ULong? =
        item?.persistentDataContainer?.get(eventIdKey, PersistentDataType.LONG)?.toULong()
}
