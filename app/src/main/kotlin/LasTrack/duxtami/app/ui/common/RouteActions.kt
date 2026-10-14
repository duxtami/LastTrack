package LasTrack.duxtami.app.ui.common

import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.core.model.ShareRequest
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** One-shot messages surfaced as snackbars. Mapped to strings in the UI layer. */
enum class UiMessage { RENAMED, DELETED, EXPORTED, EXPORT_FAILED, SHARE_FAILED, ACTION_FAILED }

/** Shared route operations for every screen that lists or shows a route. No Android types, no UI. */
class RouteActions @Inject constructor(private val repository: RouteRepository) {
    suspend fun rename(id: String, name: String): UiMessage =
        runCatchingMessage(UiMessage.RENAMED, UiMessage.ACTION_FAILED) { repository.renameRoute(id, name) }

    suspend fun delete(id: String): UiMessage =
        runCatchingMessage(UiMessage.DELETED, UiMessage.ACTION_FAILED) { repository.deleteRoute(id) }

    suspend fun export(id: String, destinationUri: String): UiMessage =
        runCatchingMessage(UiMessage.EXPORTED, UiMessage.EXPORT_FAILED) { repository.exportRoute(id, destinationUri) }

    /** Returns the share request, or null (after which the caller shows [UiMessage.SHARE_FAILED]). */
    suspend fun prepareShare(id: String): ShareRequest? = try {
        repository.prepareShare(id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun runCatchingMessage(ok: UiMessage, fail: UiMessage, block: suspend () -> Unit): UiMessage = try {
        block()
        ok
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fail
    }
}
