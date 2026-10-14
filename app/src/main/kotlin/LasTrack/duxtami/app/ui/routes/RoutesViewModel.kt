package LasTrack.duxtami.app.ui.routes

import LasTrack.duxtami.app.core.model.RouteFilter
import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.core.model.RouteSummary
import LasTrack.duxtami.app.core.model.ShareRequest
import LasTrack.duxtami.app.core.model.SourceFormat
import LasTrack.duxtami.app.ui.common.RouteActions
import LasTrack.duxtami.app.ui.common.UiMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutesUiState(
    val isLoading: Boolean = true,
    val routes: List<RouteSummary> = emptyList(),
    /** Routes saved in total, regardless of the current search/filter. */
    val totalCount: Int = 0,
    val query: String = "",
    val format: SourceFormat? = null,
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    repository: RouteRepository,
    private val actions: RouteActions,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val format = MutableStateFlow<SourceFormat?>(null)

    val uiState: StateFlow<RoutesUiState> = combine(repository.observeRoutes(), query, format) { all, q, f ->
        RoutesUiState(
            isLoading = false,
            routes = RouteFilter.filter(all, q, f),
            totalCount = all.size,
            query = q,
            format = f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutesUiState())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onQueryChange(value: String) { query.value = value }
    fun onFormatChange(value: SourceFormat?) { format.value = value }

    fun rename(id: String, name: String) = viewModelScope.launch { _messages.send(actions.rename(id, name)) }
    fun delete(id: String) = viewModelScope.launch { _messages.send(actions.delete(id)) }
    fun export(id: String, uri: String) = viewModelScope.launch { _messages.send(actions.export(id, uri)) }

    suspend fun prepareShare(id: String): ShareRequest? =
        actions.prepareShare(id).also { if (it == null) _messages.send(UiMessage.SHARE_FAILED) }
}
