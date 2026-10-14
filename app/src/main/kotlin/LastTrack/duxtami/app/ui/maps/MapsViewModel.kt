package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.map.MapProviderRegistry
import LastTrack.duxtami.app.core.model.MapProviderId
import LastTrack.duxtami.app.core.model.PreferencesRepository
import LastTrack.duxtami.app.core.model.RouteRepository
import LastTrack.duxtami.app.core.model.RouteSummary
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderOption(val id: MapProviderId, val availability: MapAvailability)

data class MapsUiState(
    val providers: List<ProviderOption> = emptyList(),
    val selected: MapProviderId = MapProviderId.GOOGLE_MAPS,
    val selectedAvailability: MapAvailability = MapAvailability.Unavailable("loading"),
    val basemap: BasemapStyle? = null,
    val route: RouteSummary? = null,
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val registry: MapProviderRegistry,
    private val preferences: PreferencesRepository,
    repository: RouteRepository,
) : ViewModel() {
    private val routeId: String? = savedStateHandle["routeId"]

    private val routeFlow: Flow<RouteSummary?> = routeId?.let { repository.observeRoute(it) } ?: flowOf(null)

    val uiState: StateFlow<MapsUiState> = combine(preferences.preferences, routeFlow) { prefs, route ->
        val provider: MapProvider? = registry.get(prefs.mapProvider)
        MapsUiState(
            providers = registry.all.map { ProviderOption(it.id, it.availability) },
            selected = prefs.mapProvider,
            selectedAvailability = provider?.availability ?: MapAvailability.Unavailable("unknown provider"),
            basemap = registry.resolveBasemap(prefs.mapProvider, prefs.basemapId),
            route = route,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapsUiState())

    fun selectProvider(id: MapProviderId) {
        viewModelScope.launch {
            // Switching provider resets the basemap: ids differ between providers.
            preferences.update { it.copy(mapProvider = id, basemapId = LastTrack.duxtami.app.core.model.DEFAULT_BASEMAP_ID) }
        }
    }
}
