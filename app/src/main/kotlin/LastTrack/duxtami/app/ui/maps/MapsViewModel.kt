package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.map.MapProviderRegistry
import LastTrack.duxtami.app.core.model.AppPreferences
import LastTrack.duxtami.app.core.model.DEFAULT_BASEMAP_ID
import LastTrack.duxtami.app.core.model.MapProviderId
import LastTrack.duxtami.app.core.model.PreferencesRepository
import LastTrack.duxtami.app.core.model.RouteGeometry
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderOption(val id: MapProviderId, val availability: MapAvailability)

data class MapsUiState(
    val providers: List<ProviderOption> = emptyList(),
    val selected: MapProviderId = MapProviderId.OPEN_STREET_MAP,
    val selectedAvailability: MapAvailability = MapAvailability.Unavailable("loading"),
    val basemap: BasemapStyle? = null,
    val availableBasemaps: List<BasemapStyle> = emptyList(),
    val route: RouteSummary? = null,
    val routeGeometry: RouteGeometry? = null,
    val preferences: AppPreferences = AppPreferences(),
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val registry: MapProviderRegistry,
    private val preferences: PreferencesRepository,
    private val repository: RouteRepository,
) : ViewModel() {
    private val routeId: String? = savedStateHandle["routeId"]

    private val routeFlow: Flow<RouteSummary?> = routeId?.let { repository.observeRoute(it) } ?: flowOf(null)

    private val geometryFlow: Flow<RouteGeometry?> = flow {
        if (routeId != null) {
            emit(repository.getRoutePoints(routeId))
        } else {
            emit(null)
        }
    }

    val uiState: StateFlow<MapsUiState> = combine(preferences.preferences, routeFlow, geometryFlow) { prefs, route, geometry ->
        val provider: MapProvider? = registry.get(prefs.mapProvider)
        MapsUiState(
            providers = registry.all.map { ProviderOption(it.id, it.availability) },
            selected = prefs.mapProvider,
            selectedAvailability = provider?.availability ?: MapAvailability.Unavailable("unknown provider"),
            basemap = registry.resolveBasemap(prefs.mapProvider, prefs.basemapId),
            availableBasemaps = registry.basemapsFor(prefs.mapProvider),
            route = route,
            routeGeometry = geometry,
            preferences = prefs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapsUiState())

    fun selectProvider(id: MapProviderId) {
        viewModelScope.launch {
            // Switching provider resets the basemap: ids differ between providers.
            preferences.update { it.copy(mapProvider = id, basemapId = DEFAULT_BASEMAP_ID) }
        }
    }

    fun selectBasemap(id: String) {
        viewModelScope.launch {
            preferences.update { it.copy(basemapId = id) }
        }
    }
}
