package LasTrack.duxtami.app.ui.details

import LasTrack.duxtami.app.core.model.RouteGeometry
import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.core.model.RouteSummary
import LasTrack.duxtami.app.core.model.ShareRequest
import LasTrack.duxtami.app.ui.common.RouteActions
import LasTrack.duxtami.app.ui.common.UiMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data object NotFound : DetailsUiState
    data class Loaded(val route: RouteSummary) : DetailsUiState
}

@HiltViewModel
class RouteDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RouteRepository,
    private val actions: RouteActions,
) : ViewModel() {
    val routeId: String = checkNotNull(savedStateHandle["routeId"])

    val uiState: StateFlow<DetailsUiState> = repository.observeRoute(routeId)
        .map { if (it == null) DetailsUiState.NotFound else DetailsUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState.Loading)

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun rename(name: String) = viewModelScope.launch { _messages.send(actions.rename(routeId, name)) }
    fun export(uri: String) = viewModelScope.launch { _messages.send(actions.export(routeId, uri)) }

    /** Deletes the route; [onDone] runs afterwards so the screen can navigate back. */
    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        val result = actions.delete(routeId)
        if (result == UiMessage.DELETED) onDone() else _messages.send(result)
    }

    suspend fun prepareShare(): ShareRequest? =
        actions.prepareShare(routeId).also { if (it == null) _messages.send(UiMessage.SHARE_FAILED) }

    suspend fun getRouteGeometry(): RouteGeometry? = repository.getRoutePoints(routeId)
}
