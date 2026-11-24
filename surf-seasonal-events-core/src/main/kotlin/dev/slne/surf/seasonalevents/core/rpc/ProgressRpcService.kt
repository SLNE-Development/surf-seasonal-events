package dev.slne.surf.seasonalevents.core.rpc

import dev.slne.surf.rabbitmq.api.rpc.RpcService
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import dev.slne.surf.seasonalevents.api.progress.PlayerView
import dev.slne.surf.seasonalevents.api.result.CollectResult
import dev.slne.surf.seasonalevents.api.result.PreviewResult
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
}
