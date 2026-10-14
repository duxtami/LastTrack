package LasTrack.duxtami.app.ui.places

import LasTrack.duxtami.app.core.model.CoordinateParser
import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.SavedPlace
import LasTrack.duxtami.app.core.model.SavedPlacesRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SavedPlacesUiState(
    val places: List<SavedPlace> = emptyList(),
    val filteredPlaces: List<SavedPlace> = emptyList(),
    val selectedCategory: PlaceCategory? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
    private val repository: SavedPlacesRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<SavedPlacesUiState> = combine(
        repository.observeAll(),
        selectedCategory,
        searchQuery,
    ) { allPlaces, category, query ->
        val filtered = allPlaces.filter { place ->
            val matchesCategory = category == null || place.category == category
            val matchesQuery = query.isBlank() ||
                place.name.contains(query, ignoreCase = true) ||
                (place.description?.contains(query, ignoreCase = true) == true) ||
                "${place.latitude}, ${place.longitude}".contains(query)
            matchesCategory && matchesQuery
        }
        SavedPlacesUiState(
            places = allPlaces,
            filteredPlaces = filtered,
            selectedCategory = category,
            searchQuery = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedPlacesUiState())

    fun setCategoryFilter(category: PlaceCategory?) {
        selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addPlaceDirect(
        name: String,
        latitude: Double,
        longitude: Double,
        category: PlaceCategory,
        description: String?,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            val savedImagePath = if (imageUri != null) repository.saveImage(imageUri) else null
            val place = SavedPlace(
                id = UUID.randomUUID().toString(),
                name = name.trim().ifEmpty { "Saved Location" },
                latitude = latitude,
                longitude = longitude,
                category = category,
                description = description?.trim()?.ifEmpty { null },
                imagePath = savedImagePath,
                isReminderActive = false,
                reminderNote = null,
                createdAt = System.currentTimeMillis(),
            )
            repository.savePlace(place)
        }
    }

    suspend fun addPlaceAsync(
        name: String,
        rawCoordinates: String,
        category: PlaceCategory,
        description: String?,
        imageUri: String?,
    ): Boolean {
        val parsed = CoordinateParser.resolveAndParse(rawCoordinates) ?: return false
        val savedImagePath = if (imageUri != null) repository.saveImage(imageUri) else null
        val place = SavedPlace(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { parsed.suggestedName ?: "Saved Location" },
            latitude = parsed.latitude,
            longitude = parsed.longitude,
            category = category,
            description = description?.trim()?.ifEmpty { null },
            imagePath = savedImagePath,
            isReminderActive = false,
            reminderNote = null,
            createdAt = System.currentTimeMillis(),
        )
        repository.savePlace(place)
        return true
    }

    fun addPlace(
        name: String,
        rawCoordinates: String,
        category: PlaceCategory,
        description: String?,
        imageUri: String?,
    ): Boolean {
        val parsed = CoordinateParser.parse(rawCoordinates) ?: return false
        viewModelScope.launch {
            val savedImagePath = if (imageUri != null) repository.saveImage(imageUri) else null
            val place = SavedPlace(
                id = UUID.randomUUID().toString(),
                name = name.trim().ifEmpty { parsed.suggestedName ?: "Saved Location" },
                latitude = parsed.latitude,
                longitude = parsed.longitude,
                category = category,
                description = description?.trim()?.ifEmpty { null },
                imagePath = savedImagePath,
                isReminderActive = false,
                reminderNote = null,
                createdAt = System.currentTimeMillis(),
            )
            repository.savePlace(place)
        }
        return true
    }

    fun deletePlace(id: String) {
        viewModelScope.launch {
            repository.deletePlace(id)
        }
    }
}
