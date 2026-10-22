package LasTrack.duxtami.app.ui.places

import LasTrack.duxtami.app.core.model.CoordinateParser
import LasTrack.duxtami.app.core.model.GeoPoint
import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.SavedPlace
import LasTrack.duxtami.app.core.model.SavedPlacesRepository
import LasTrack.duxtami.app.ui.maps.TrackRecordingManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class SavedPlacesUiState(
    val places: List<SavedPlace> = emptyList(),
    val filteredPlaces: List<SavedPlace> = emptyList(),
    val selectedFilter: String? = null,
    val searchQuery: String = "",
    val availableCustomLabels: List<String> = emptyList(),
    val currentLocation: GeoPoint? = null,
)

@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
    private val repository: SavedPlacesRepository,
    private val recordingManager: TrackRecordingManager,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val userCustomLabels = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<SavedPlacesUiState> = combine(
        repository.observeAll(),
        selectedFilter,
        searchQuery,
        userCustomLabels,
        recordingManager.currentLocation,
    ) { allPlaces, filter, query, customSet, curLoc ->
        val allCustomLabels = (customSet + allPlaces.mapNotNull { it.customLabel })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val filtered = allPlaces.filter { place ->
            val matchesFilter = when {
                filter == null -> true
                place.customLabel != null && place.customLabel.equals(filter, ignoreCase = true) -> true
                place.category.name.equals(filter, ignoreCase = true) -> true
                place.category.displayName.equals(filter, ignoreCase = true) -> true
                else -> false
            }
            val matchesQuery = query.isBlank() ||
                place.name.contains(query, ignoreCase = true) ||
                (place.description?.contains(query, ignoreCase = true) == true) ||
                (place.customLabel?.contains(query, ignoreCase = true) == true) ||
                "${place.latitude}, ${place.longitude}".contains(query)
            matchesFilter && matchesQuery
        }

        SavedPlacesUiState(
            places = allPlaces,
            filteredPlaces = filtered,
            selectedFilter = filter,
            searchQuery = query,
            availableCustomLabels = allCustomLabels,
            currentLocation = curLoc,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedPlacesUiState())

    fun setFilter(filter: String?) {
        selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addCustomLabel(label: String) {
        val trimmed = label.trim()
        if (trimmed.isNotBlank()) {
            userCustomLabels.update { it + trimmed }
        }
    }

    suspend fun addPlaceAsync(
        name: String,
        rawCoordinates: String,
        category: PlaceCategory,
        customLabel: String? = null,
        description: String?,
        imageUri: String?,
        iconKey: String? = null,
    ): Boolean {
        val parsed = CoordinateParser.resolveAndParse(rawCoordinates) ?: return false
        val savedImagePath = if (imageUri != null) repository.saveImage(imageUri) else null
        val place = SavedPlace(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { parsed.suggestedName ?: "Saved Location" },
            latitude = parsed.latitude,
            longitude = parsed.longitude,
            category = category,
            customLabel = if (category == PlaceCategory.CUSTOM) customLabel?.trim()?.ifEmpty { null } else null,
            description = description?.trim()?.ifEmpty { null },
            imagePath = savedImagePath,
            isReminderActive = false,
            reminderNote = null,
            createdAt = System.currentTimeMillis(),
            iconKey = iconKey,
        )
        repository.savePlace(place)
        return true
    }

    suspend fun updatePlaceAsync(
        id: String,
        name: String,
        rawCoordinates: String,
        category: PlaceCategory,
        customLabel: String? = null,
        description: String?,
        imageUri: String?,
        removeImage: Boolean = false,
        iconKey: String? = null,
    ): Boolean {
        val existing = repository.get(id) ?: return false
        val parsed = CoordinateParser.resolveAndParse(rawCoordinates)
        val lat = parsed?.latitude ?: existing.latitude
        val lon = parsed?.longitude ?: existing.longitude

        val finalImagePath = when {
            removeImage -> {
                if (existing.imagePath != null) {
                    runCatching { File(existing.imagePath).delete() }
                }
                null
            }
            imageUri != null && imageUri != existing.imagePath -> {
                if (existing.imagePath != null) {
                    runCatching { File(existing.imagePath).delete() }
                }
                repository.saveImage(imageUri)
            }
            else -> existing.imagePath
        }

        val updated = existing.copy(
            name = name.trim().ifEmpty { existing.name },
            latitude = lat,
            longitude = lon,
            category = category,
            customLabel = if (category == PlaceCategory.CUSTOM) customLabel?.trim()?.ifEmpty { null } else null,
            description = description?.trim()?.ifEmpty { null },
            imagePath = finalImagePath,
            iconKey = iconKey ?: existing.iconKey,
        )
        repository.savePlace(updated)
        return true
    }

    fun deletePlace(id: String) {
        viewModelScope.launch {
            repository.deletePlace(id)
        }
    }
}
