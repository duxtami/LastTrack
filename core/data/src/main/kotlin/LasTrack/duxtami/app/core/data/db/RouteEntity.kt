package LasTrack.duxtami.app.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Metadata + statistics only. Coordinates live in a compact binary file ([coordinatesPath]); the untouched
 * imported file lives at [originalPath]. Nullable stats mean "not available in the source", not zero.
 */
@Entity(tableName = "routes", indices = [Index("importedAt")])
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fileName: String,
    val format: String,
    val fileSizeBytes: Long,
    val distanceMeters: Double,
    val durationMillis: Long?,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val maxElevationMeters: Double?,
    val minElevationMeters: Double?,
    val averageSpeedMps: Double?,
    val pointCount: Int,
    val segmentCount: Int,
    val waypointCount: Int,
    val description: String?,
    val importedAt: Long,
    val coordinatesPath: String,
    val originalPath: String,
)
