package dev.slne.surf.seasonalevents.paper.gui

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.Instant

// Buttons and items of the loot GUIs, in the surf menu style. The icons need the surf resource pack.

private fun icon(
    type: ViewIconType,
    color: ViewIconColor,
    name: SurfComponentBuilder.() -> Unit,
    description: String,
): ItemStack = viewIcon(type, color) {
    displayName(name)
    buildLore {
        emptyLine()
        line { spacer(description.toSmallCaps()) }
        emptyLine()
    }
}

val closeIcon = icon(ViewIconType.CROSS, ViewIconColor.RED, { primary("Schließen", TextDecoration.BOLD) }, "Das Menü schließen")

val newPackageIcon = icon(
    ViewIconType.PLUS, ViewIconColor.GREEN,
    { success("Neues Paket", TextDecoration.BOLD) },
    "Pakete ohne festen Tag werden zufällig vergeben",
)

val backIcon = icon(ViewIconType.ARROW_LEFT, ViewIconColor.WHITE, { primary("Zurück", TextDecoration.BOLD) }, "Speichern und zur Übersicht")

val deleteIcon = icon(ViewIconType.MINUS, ViewIconColor.RED, { error("Paket löschen", TextDecoration.BOLD) }, "Fragt vorher nach")

fun fixedDayIcon(lootPackage: LootPackage): ItemStack = viewIcon(ViewIconType.BELL, ViewIconColor.YELLOW) {
    displayName { primary("Fester Tag", TextDecoration.BOLD) }
    buildLore {
        emptyLine()
        line { variableKey("Aktuell: "); variableValue(lootPackage.distribution()) }
        line { spacer("Optional: an diesem Tag bekommt jeder dieses Paket".toSmallCaps()) }
        line { spacer("Ohne festen Tag ist es Teil des Zufallspools".toSmallCaps()) }
        emptyLine()
    }
}

/** An entry of the overview: name, content and how it is handed out. */
fun packageIcon(lootPackage: LootPackage): ItemStack = buildItem(Material.CHEST) {
    val stacks = LootItems.decode(lootPackage.items)
    displayName { primary(lootPackage.name) }
    buildLore {
        emptyLine()
        line { variableKey("Inhalt: "); variableValue(if (stacks.isEmpty()) "leer, wird nie vergeben" else "${stacks.size} Stapel") }
        line { variableKey("Vergabe: "); variableValue(lootPackage.distribution()) }
        emptyLine()
        line { spacer("Klicken zum Bearbeiten".toSmallCaps()) }
    }
}

/** "zufällig" for the random pool, otherwise the fixed day, or that the NPC hands it out. */
private fun LootPackage.distribution(): String {
    val day = fixedDay?.let(LootItems::formatDay) ?: return "zufällig"
    return if (npcReward) "vom NPC ab $day" else "fest am $day"
}

/** Toggles whether the event's NPC hands the package out instead of a block on the map. */
fun npcRewardIcon(lootPackage: LootPackage): ItemStack =
    viewIcon(ViewIconType.USERS, if (lootPackage.npcReward) ViewIconColor.GREEN else ViewIconColor.WHITE) {
        displayName { primary("NPC-Belohnung", TextDecoration.BOLD) }
        buildLore {
            emptyLine()
            line { variableKey("Aktuell: "); variableValue(if (lootPackage.npcReward) "an" else "aus") }
            line { spacer("Der NPC übergibt dieses Paket ab seinem festen Tag,".toSmallCaps()) }
            line { spacer("sobald alle Blöcke der Tage davor gefunden sind".toSmallCaps()) }
            line { spacer("Braucht einen festen Tag, nur ein Paket pro Event".toSmallCaps()) }
            emptyLine()
            line { spacer("Klicken zum Umschalten".toSmallCaps()) }
        }
    }

/**
 * Explains how packages are handed out in the event's mode and whether there are enough of them:
 * one package per day in `DAILY`, one per found block in `COLLECT`, each at most once per player.
 */
fun lootInfoIcon(event: SeasonalEvent, packages: List<LootPackage>, markers: List<EventMarker>): ItemStack {
    // The NPC reward is handed out by the NPC, not for a block.
    val filled = packages.count { it.items.isNotEmpty() && !it.npcReward }
    val needed = when (event.mode) {
        EventMode.DAILY -> dailyDays(event, markers)
        EventMode.COLLECT -> markers.size
    }
    /** Packages still to fill; null if there are enough. */
    val missing = needed?.minus(filled)?.takeIf { it > 0 }

    return viewIcon(ViewIconType.QUESTION_MARK, if (missing == null) ViewIconColor.BLUE else ViewIconColor.YELLOW) {
        displayName { primary("So werden Pakete vergeben", TextDecoration.BOLD) }
        buildLore {
            emptyLine()
            when (event.mode) {
                EventMode.DAILY -> {
                    line { info("Jeder Spieler findet pro Tag einen Block") }
                    line { info("und bekommt dafür ein Paket.") }
                    line { info("Ein Paket mit festem Tag hat an dem Tag Vorrang,") }
                    line { info("sonst kommt ein zufälliges ohne festen Tag.") }
                    line { info("Ist ein Block auf einen Tag festgelegt, gilt er") }
                    line { info("für alle und kann später nachgeholt werden.") }
                    line { info("Die NPC-Belohnung gibt nur der NPC aus.") }
                }

                EventMode.COLLECT -> {
                    line { info("Jeder gefundene Block gibt ein zufälliges Paket.") }
                    line { info("Feste Tage gibt es in diesem Modus nicht.") }
                }
            }
            line { info("Jedes Paket bekommt ein Spieler höchstens einmal.") }
            emptyLine()
            line { variableKey("Befüllte Pakete: "); variableValue(filled.toString()) }
            line {
                variableKey("Benötigt: ")
                variableValue(
                    when {
                        needed == null -> "1 pro Event-Tag (kein Enddatum gesetzt)"
                        event.mode == EventMode.DAILY -> "mindestens $needed (1 pro Tag mit Block)"
                        else -> "mindestens $needed (1 pro Block)"
                    },
                )
            }
            if (missing != null) {
                emptyLine()
                line { warning("Zu wenige Pakete: so kann das Event nicht starten.") }
                line {
                    warning(
                        when (event.mode) {
                            EventMode.DAILY -> "Noch $missing befüllen oder ein früheres Ende wählen."
                            EventMode.COLLECT -> "Noch $missing befüllen oder Blöcke entmarkieren."
                        },
                    )
                }
            }
            emptyLine()
        }
    }
}

/**
 * Days that hand out a block, the same rule as the microservice's `PackageRequirement`: every day from the
 * start (or today) until the end while the random pool has markers, otherwise only the pinned days up to the
 * end. Null without an end date.
 */
private fun dailyDays(event: SeasonalEvent, markers: List<EventMarker>): Int? {
    val end = event.endsAt?.atZone(SeasonalTime.ZONE)?.toLocalDate() ?: return null
    val start = (event.startedAt ?: Instant.now()).atZone(SeasonalTime.ZONE).toLocalDate()
    val pinned = markers.mapNotNullTo(HashSet()) { it.day }
    val hasRandomPool = pinned.isEmpty() || markers.size > pinned.size
    val eventDays = if (hasRandomPool) {
        generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toSet()
    } else {
        emptySet()
    }
    return (eventDays + pinned.filter { !it.isAfter(end) }).size
}
