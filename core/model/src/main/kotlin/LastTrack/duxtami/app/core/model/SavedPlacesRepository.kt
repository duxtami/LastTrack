package LastTrack.duxtami.app.core.model

import kotlinx.coroutines.flow.Flow

interface SavedPlacesRepository {
    fun observeAll(): Flow<List<SavedPlace>>
    fun observe(id: String): Flow<SavedPlace?>
    suspend fun get(id: String): SavedPlace?
    suspend fun savePlace(place: SavedPlace)
    suspend fun deletePlace(id: String)
    suspend fun saveImage(sourceUri: String): String?
}
