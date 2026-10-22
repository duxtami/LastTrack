package LasTrack.duxtami.app.ui.settings

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapProviderRegistry
import LasTrack.duxtami.app.core.model.AppPreferences
import LasTrack.duxtami.app.core.model.PreferencesRepository
import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.ui.settings.backup.BackupManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val basemaps: List<BasemapStyle> = emptyList(),
    val selectedBasemap: BasemapStyle? = null,
    val routeCount: Int = 0,
    val importedBytes: Long = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    registry: MapProviderRegistry,
    repository: RouteRepository,
    val backupManager: BackupManager,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(preferences.preferences, repository.observeRoutes()) { prefs, routes ->
        SettingsUiState(
            preferences = prefs,
            basemaps = registry.basemapsFor(prefs.mapProvider),
            selectedBasemap = registry.resolveBasemap(prefs.mapProvider, prefs.basemapId),
            routeCount = routes.size,
            importedBytes = routes.sumOf { it.fileSizeBytes },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (AppPreferences) -> AppPreferences) {
        viewModelScope.launch { preferences.update(transform) }
    }
}
