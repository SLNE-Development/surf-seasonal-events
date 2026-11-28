package dev.slne.surf.seasonalevents.core.rpc

import dev.slne.surf.rabbitmq.api.rpc.RpcService
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.progress.NpcStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerView
import dev.slne.surf.seasonalevents.api.result.CollectResult
import dev.slne.surf.seasonalevents.api.result.PreviewResult
import dev.slne.surf.seasonalevents.api.result.RewardResult
import java.util.UUID

/** Tracks what each player sees and collects in the active event. */
@RpcService
interface ProgressRpcService {

    /**
     * Returns the markers [player] should currently see as fake blocks.
     * In a `DAILY` event, today's assignment is created on the first call of the day.
     *
     * @return null if no event is active
     */
    suspend fun getPlayerView(player: UUID): PlayerView?

    /** Returns all markers of an event for staff in edit mode, in every state including `DRAFT`. */
    suspend fun getEditView(eventId: ULong): List<EventMarker>

    /**
     * Checks whether [player] may collect the marker and picks the package they would get.
     * Nothing is booked; the plugin uses the items to check for inventory space before calling [collect].
     */
    suspend fun previewCollect(player: UUID, markerId: ULong): PreviewResult

    /**
     * Books the collection of [packageId] in one transaction, repeating every check of [previewCollect].
     * The plugin hands out the items only on [CollectResult.Success].
     *
     * @return [CollectResult.PackageUnavailable] if the previewed package can no longer be given
     */
    suspend fun collect(player: UUID, markerId: ULong, packageId: ULong): CollectResult

    /** Returns the progress of [player] in the active event, as shown by `/seasonalevents`. */
    suspend fun getStatus(player: UUID): PlayerStatus

    /**
     * Records that [player] talked to NPC [npcName] of event [eventId] and returns what the NPC knows about them.
     * [NpcStatus.Active.firstVisit] is true only for the first talk per event.
     *
     * @return [NpcStatus.NoActiveEvent], recording nothing, unless [eventId] is the active event and
     * [npcName] its linked NPC
     */
    suspend fun visitNpc(player: UUID, eventId: ULong, npcName: String): NpcStatus

    /**
     * Checks whether [player] may receive the NPC reward of event [eventId] and returns its items, without
     * booking anything. The plugin uses the items to check for inventory space before calling [claimReward].
     *
     * @return [RewardResult.NoActiveEvent] if [eventId] is no longer the active event
     */
    suspend fun previewReward(player: UUID, eventId: ULong): RewardResult

    /**
     * Books the NPC reward [packageId] of event [eventId] in one transaction, repeating every check of
     * [previewReward]. The plugin hands out the items only on [RewardResult.Granted].
     */
    suspend fun claimReward(player: UUID, eventId: ULong, packageId: ULong): RewardResult
}
