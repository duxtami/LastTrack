package LasTrack.duxtami.app.core.data.db

import LasTrack.duxtami.app.core.model.RouteStats
import LasTrack.duxtami.app.core.model.RouteSummary
import LasTrack.duxtami.app.core.model.SourceFormat

internal fun RouteEntity.toStats() = RouteStats(
    distanceMeters = distanceMeters,
    durationMillis = durationMillis,
    elevationGainMeters = elevationGainMeters,
    elevationLossMeters = elevationLossMeters,
    maxElevationMeters = maxElevationMeters,
    minElevationMeters = minElevationMeters,
    averageSpeedMetersPerSecond = averageSpeedMps,
    pointCount = pointCount,
    waypointCount = waypointCount,
    segmentCount = segmentCount,
)

internal fun RouteEntity.sourceFormat(): SourceFormat =
    SourceFormat.entries.firstOrNull { it.name == format } ?: SourceFormat.GPX

internal fun RouteEntity.toSummary() = RouteSummary(
    id = id,
    name = name,
    fileName = fileName,
    format = sourceFormat(),
    fileSizeBytes = fileSizeBytes,
    stats = toStats(),
    description = description,
    importedAt = importedAt,
)
