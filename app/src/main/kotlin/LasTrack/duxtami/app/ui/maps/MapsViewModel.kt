package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.map.MapProviderRegistry
import LasTrack.duxtami.app.core.model.AppPreferences
import LasTrack.duxtami.app.core.model.DEFAULT_BASEMAP_ID
import LasTrack.duxtami.app.core.model.GeoPoint
import LasTrack.duxtami.app.core.model.GeoRoute
import LasTrack.duxtami.app.core.model.MapProviderId
import LasTrack.duxtami.app.core.model.PreferencesRepository
import LasTrack.duxtami.app.core.model.RouteGeometry
import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.core.model.RouteSummary
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
    val recordingStatus: RecordingStatus = RecordingStatus.IDLE,
    val recordingStats: RecordingStats = RecordingStats(),
    val recordedPoints: List<GeoPoint> = emptyList(),
    val parkedBike: ParkedBikeReminder? = null,
    val currentLocation: GeoPoint? = null,
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val registry: MapProviderRegistry,
    private val preferences: PreferencesRepository,
    private val repository: RouteRepository,
    private val savedPlacesRepository: LasTrack.duxtami.app.core.model.SavedPlacesRepository,
    val recordingManager: TrackRecordingManager,
) : ViewModel() {
    private val routeId: String? = savedStateHandle["routeId"]
    private val activeRouteDismissed = MutableStateFlow(false)

    private val routeFlow: Flow<RouteSummary?> = combine(
        routeId?.let { repository.observeRoute(it) } ?: flowOf(null),
        activeRouteDismissed,
    ) { route, dismissed -> if (dismissed) null else route }

    private val geometryFlow: Flow<RouteGeometry?> = combine(
        flow {
            if (routeId != null) {
                emit(repository.getRoutePoints(routeId))
            } else {
                emit(null)
            }
        },
        activeRouteDismissed,
    ) { geom, dismissed -> if (dismissed) null else geom }

private data class RecordStateBundle(
    val status: RecordingStatus,
    val stats: RecordingStats,
    val recPoints: List<GeoPoint>,
    val parkedBike: ParkedBikeReminder?,
    val curLoc: GeoPoint?,
)

    private val baseMapFlow = combine(preferences.preferences, routeFlow, geometryFlow) { prefs, route, geometry ->
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
    }

    private val recordingStateFlow = combine(
        recordingManager.status,
        recordingManager.stats,
        recordingManager.recordedPoints,
        recordingManager.parkedBike,
        recordingManager.currentLocation,
    ) { status, stats, recPoints, parkedBike, curLoc ->
        RecordStateBundle(status, stats, recPoints, parkedBike, curLoc)
    }

    val uiState: StateFlow<MapsUiState> = combine(baseMapFlow, recordingStateFlow) { base, rec ->
        base.copy(
            recordingStatus = rec.status,
            recordingStats = rec.stats,
            recordedPoints = rec.recPoints,
            parkedBike = rec.parkedBike,
            currentLocation = rec.curLoc,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapsUiState())

    fun selectProvider(id: MapProviderId) {
        viewModelScope.launch {
            preferences.update { it.copy(mapProvider = id, basemapId = DEFAULT_BASEMAP_ID) }
        }
    }

    fun selectBasemap(id: String) {
        viewModelScope.launch {
            preferences.update { it.copy(basemapId = id) }
        }
    }

    fun startRecording(): Boolean = recordingManager.startRecording()

    fun pauseRecording() = recordingManager.pauseRecording()

    fun resumeRecording() = recordingManager.resumeRecording()

    fun stopAndDiscard() = recordingManager.stopAndDiscard()

    suspend fun stopAndSave(name: String? = null, desc: String? = null): GeoRoute? =
        recordingManager.stopAndSave(name, desc)

    fun setParkedBike(note: String = "Parked Bike"): ParkedBikeReminder? =
        recordingManager.setParkedBike(note)

    fun clearActiveRoute() {
        activeRouteDismissed.value = true
    }

    fun savePlace(
        name: String,
        latitude: Double,
        longitude: Double,
        category: LasTrack.duxtami.app.core.model.PlaceCategory = LasTrack.duxtami.app.core.model.PlaceCategory.STARRED,
        description: String? = null,
    ) {
        viewModelScope.launch {
            savedPlacesRepository.savePlace(
                LasTrack.duxtami.app.core.model.SavedPlace(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name.trim().ifEmpty { "Saved Location" },
                    latitude = latitude,
                    longitude = longitude,
                    category = category,
                    description = description?.trim()?.ifEmpty { null },
                    imagePath = null,
                    isReminderActive = false,
                    reminderNote = null,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }
}
