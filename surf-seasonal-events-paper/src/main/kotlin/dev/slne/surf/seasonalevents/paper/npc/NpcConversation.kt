package dev.slne.surf.seasonalevents.paper.npc

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.progress.NpcStatus
import dev.slne.surf.seasonalevents.api.result.RewardResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.progressRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.collect.CollectService
import dev.slne.surf.seasonalevents.paper.collect.InventorySpace
import dev.slne.surf.seasonalevents.paper.collect.PlayerClaims
import dev.slne.surf.seasonalevents.paper.command.errorMessage
import dev.slne.surf.seasonalevents.paper.command.inventoryFullMessage
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.gui.LootItems
import dev.slne.surf.seasonalevents.paper.hud.GiftHud
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.LocalDate

/**
 * A talk with an event NPC: loads what the NPC knows about the player, shows the matching dialog and
 * hands out the NPC reward. Items are only given after the microservice booked the reward.
 */
internal object NpcConversation {

    /** Talks to [player] as NPC [npcName] of event [eventId]; can be called from any thread. */
    suspend fun talk(player: Player, eventId: ULong, npcName: String, speaker: Component) {
        val status = when (val outcome = rpcCall { progressRpc.visitNpc(player.uniqueId, eventId, npcName) }) {
            is RpcOutcome.Unavailable -> return player.sendMessage(unavailableMessage())
            is RpcOutcome.Ok -> outcome.value
        }
        val today = LocalDate.now(SeasonalTime.ZONE)
        val dialog = when {
            status !is NpcStatus.Active -> NpcDialogs.resting(speaker)
            status.firstVisit -> NpcDialogs.intro(speaker, status, today)
            else -> NpcDialogs.progress(speaker, status, today)
        }
        withContext(plugin.entityDispatcher(player)) { player.showDialog(dialog) }

        // The first talk unlocks the blocks of an event that requires it, so they appear right away.
        if (status is NpcStatus.Active && status.firstVisit) ViewService.reload(player)
    }

    /**
     * Previews the reward of event [eventId], checks the inventory, books it and hands it over; can be called
     * from any thread. [eventId] is the event of the dialog, so an old dialog cannot claim for a newer event.
     */
    suspend fun claimReward(player: Player, eventId: ULong, speaker: Component) =
        PlayerClaims.exclusive(player.uniqueId) { claim(player, eventId, speaker) }

    private suspend fun claim(player: Player, eventId: ULong, speaker: Component) {
        val preview = when (val outcome = rpcCall { progressRpc.previewReward(player.uniqueId, eventId) }) {
            is RpcOutcome.Unavailable -> return player.sendMessage(unavailableMessage())
            is RpcOutcome.Ok -> outcome.value as? RewardResult.Preview ?: return player.sendMessage(outcome.value.message())
        }

        // Nothing is booked yet, so a full inventory just stops here.
        val items = LootItems.decode(preview.items)
        val fits = withContext(plugin.entityDispatcher(player)) {
            InventorySpace.fits(player.inventory.storageContents, items)
        }
        if (!fits) return player.sendMessage(inventoryFullMessage())

        val result = when (val outcome = rpcCall { progressRpc.claimReward(player.uniqueId, eventId, preview.packageId) }) {
            is RpcOutcome.Unavailable -> return player.sendMessage(unavailableMessage())
            is RpcOutcome.Ok -> outcome.value
        }
        if (result !is RewardResult.Granted) return player.sendMessage(result.message())

        withContext(plugin.entityDispatcher(player)) {
            CollectService.give(player, LootItems.decode(result.lootPackage.items))
            celebrate(player)
            player.showDialog(NpcDialogs.thanks(speaker))
        }
        GiftHud.showProgress(player)
    }

    private fun celebrate(player: Player) {
        player.playSound(true) { type(Sound.UI_TOAST_CHALLENGE_COMPLETE) }
        player.spawnParticle(Particle.FIREWORK, player.location.add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.05)
    }

    /** Only for the cases in which nothing is handed out. */
    private fun RewardResult.message(): Component = when (this) {
        is RewardResult.Preview, is RewardResult.Granted -> error("Not a denial: $this")
        is RewardResult.NotYet -> errorMessage("Das Geschenk gibt es erst, wenn alle anderen Geschenke gefunden sind.")
        RewardResult.AlreadyClaimed -> errorMessage("Dieses Geschenk hast du schon bekommen.")
        RewardResult.NoReward -> errorMessage("Für dieses Event gibt es kein Geschenk vom NPC.")
        RewardResult.NoActiveEvent -> errorMessage("Das Event dieses Geschenks läuft nicht mehr.")
        RewardResult.NotVisited -> errorMessage("Sprich zuerst mit dem Event-NPC.")
        RewardResult.PackageUnavailable -> errorMessage("Das Geschenk hat sich gerade geändert. Sprich den NPC noch einmal an.")
    }
}
