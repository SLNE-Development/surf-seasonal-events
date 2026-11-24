package dev.slne.surf.seasonalevents.paper.gui

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemRender
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.modifyConfig
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.onOpen
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.lootRpc
import dev.slne.surf.seasonalevents.core.client.rpc.markerRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.plugin
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player

/** What the overview is opened with: the event and its packages, loaded beforehand by [openLootOverview]. */
private class LootOverviewData(val event: SeasonalEvent, val packages: List<LootPackage>, val markerCount: Int)

/** All loot packages of an event; open it with [openLootOverview]. */
val lootOverviewView: AbstractPaginatedSurfView = paginatedSurfView("Loot-Pakete") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }

    layoutTarget('P')

    pagination<LootPackage> {
        lazySource { ctx -> (ctx.initialData as LootOverviewData).packages }
        itemFactory { lootPackage ->
            withItem(packageIcon(lootPackage))
            onItemClick {
                val event = (initialData as LootOverviewData).event
                closeForPlayer()
                LootEditor.open(player, event, lootPackage)
            }
        }
    }

    onOpen {
        val event = (initialData as LootOverviewData).event
        modifyConfig { titleBuilder { primary("Loot: "); variableValue(event.name) } }
    }

    onFirstRender {
        slot(4, 1) {
            withItem(closeIcon)
            onItemClick { closeForPlayer() }
        }
        // The page indicator sits in the middle of this row, so the buttons stay at the edges.
        slot(4, 8) {
            onItemRender {
                val data = initialData as LootOverviewData
                item = lootInfoIcon(data.event, data.packages, data.markerCount)
            }
        }
        slot(4, 9) {
            withItem(newPackageIcon)
            onItemClick {
                val event = (initialData as LootOverviewData).event
                val viewer = player
                closeForPlayer()
                plugin.launch { createPackage(viewer, event) }
            }
        }
    }
}

/** Loads the packages of [event] and opens the overview with them; can be called from any thread. */
fun openLootOverview(player: Player, event: SeasonalEvent) {
    plugin.launch {
        val outcome = rpcCall { lootRpc.findByEvent(event.id) to markerRpc.findByEvent(event.id).size }
        if (outcome !is RpcOutcome.Ok) return@launch player.sendMessage(unavailableMessage())
        withContext(plugin.entityDispatcher(player)) {
            val (packages, markerCount) = outcome.value
            if (player.isOnline) lootOverviewView.open(player, LootOverviewData(event, packages, markerCount))
        }
    }
}

/** Creates "Paket N" and opens it in the editor right away. */
private suspend fun createPackage(player: Player, event: SeasonalEvent) {
    val existing = (rpcCall { lootRpc.findByEvent(event.id) } as? RpcOutcome.Ok)?.value?.size ?: 0
    val outcome = rpcCall { lootRpc.create(event.id, "Paket ${existing + 1}") }
    if (outcome !is RpcOutcome.Ok) return player.sendMessage(unavailableMessage())

    val result = outcome.value
    player.sendMessage(result.message())
    if (result is UpdateLootResult.Success) {
        withContext(plugin.entityDispatcher(player)) { LootEditor.open(player, event, result.lootPackage) }
    }
}

/** Only `DAILY` events support fixed days; used by the editor to show the button. */
internal val SeasonalEvent.supportsFixedDay get() = mode == EventMode.DAILY
