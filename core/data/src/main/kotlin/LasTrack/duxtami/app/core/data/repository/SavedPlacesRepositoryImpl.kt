package LasTrack.duxtami.app.core.data.repository

import LasTrack.duxtami.app.core.data.db.SavedPlaceDao
import LasTrack.duxtami.app.core.data.db.SavedPlaceEntity
import LasTrack.duxtami.app.core.data.di.IoDispatcher
import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.SavedPlace
import LasTrack.duxtami.app.core.model.SavedPlacesRepository
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedPlacesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SavedPlaceDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SavedPlacesRepository {

    override fun observeAll(): Flow<List<SavedPlace>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override fun observe(id: String): Flow<SavedPlace?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun get(id: String): SavedPlace? = withContext(io) {
        dao.get(id)?.toDomain()
    }

    override suspend fun savePlace(place: SavedPlace) = withContext(io) {
        dao.insert(place.toEntity())
    }

    override suspend fun deletePlace(id: String) = withContext(io) {
        val existing = dao.get(id)
        if (existing?.imagePath != null) {
            runCatching { File(existing.imagePath).delete() }
        }
        dao.delete(id)
        Unit
    }

    override suspend fun saveImage(sourceUri: String): String? = withContext(io) {
        try {
            val uri = Uri.parse(sourceUri)
            val dir = File(context.filesDir, "place_images").also { it.mkdirs() }
            val destFile = File(dir, "img_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun SavedPlaceEntity.toDomain() = SavedPlace(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = PlaceCategory.fromString(category),
        description = description,
        imagePath = imagePath,
        isReminderActive = isReminderActive,
        reminderNote = reminderNote,
        createdAt = createdAt,
    )

    private fun SavedPlace.toEntity() = SavedPlaceEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = category.name,
        description = description,
        imagePath = imagePath,
        isReminderActive = isReminderActive,
        reminderNote = reminderNote,
        createdAt = createdAt,
    )
}
