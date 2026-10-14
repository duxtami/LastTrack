package LasTrack.duxtami.app.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places", indices = [Index("createdAt")])
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val description: String?,
    val imagePath: String?,
    val isReminderActive: Boolean,
    val reminderNote: String?,
    val createdAt: Long,
)
