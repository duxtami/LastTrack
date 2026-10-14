package LasTrack.duxtami.app.core.parser

import LasTrack.duxtami.app.core.model.GeoSegment
import LasTrack.duxtami.app.core.model.RouteStats
import LasTrack.duxtami.app.core.model.SegmentKind

/**
 * Computes [RouteStats] from segments. Rules (also documented in the README):
 *
 *  - Distance is the sum of great-circle distances between consecutive points *within* a segment; gaps
 *    between segments are not counted.
 *  - If the file contains recorded tracks, planned routes (GPX `rte`) are ignored for distance, elevation and
 *    duration so a file that stores the same path twice is not double counted. Routes-only files use routes.
 *  - Elevation gain/loss uses a hysteresis threshold ([elevationThresholdMeters]) so GPS noise does not
 *    inflate the totals. Use 0.0 for raw sums.
 *  - Duration is the elapsed time between the earliest and latest timestamp of each timestamped segment,
 *    summed over segments. Average speed uses only those segments' distance. Missing data -> null.
 */
object RouteStatsCalculator {
    const val DEFAULT_ELEVATION_THRESHOLD_METERS = 3.0

    fun calculate(
        segments: List<GeoSegment>,
        waypointCount: Int,
        elevationThresholdMeters: Double = DEFAULT_ELEVATION_THRESHOLD_METERS,
    ): RouteStats {
        val tracks = segments.filter { it.kind == SegmentKind.TRACK && it.size > 0 }
        val effective = tracks.ifEmpty { segments.filter { it.size > 0 } }

        var distance = 0.0
        var gain = 0.0
        var loss = 0.0
        var maxEle = Double.NEGATIVE_INFINITY
        var minEle = Double.POSITIVE_INFINITY
        var eleCount = 0
        var gainLossPossible = false
        var durationMs = 0L
        var timedDistance = 0.0
        var anyDuration = false

        for (seg in effective) {
            val segDistance = segmentDistance(seg)
            distance += segDistance

            // Elevation
            var ref = Double.NaN
            var segEleCount = 0
            for (i in 0 until seg.size) {
                val e = seg.elevationOrNaN(i)
                if (e.isNaN()) continue
                eleCount++
                segEleCount++
                if (e > maxEle) maxEle = e
                if (e < minEle) minEle = e
                if (ref.isNaN()) {
                    ref = e
                } else {
                    val d = e - ref
                    if (d > 0.0 && d >= elevationThresholdMeters) {
                        gain += d
                        ref = e
                    } else if (d < 0.0 && -d >= elevationThresholdMeters) {
                        loss += -d
                        ref = e
                    }
                }
            }
            if (segEleCount >= 2) gainLossPossible = true

            // Time
            var minT = Long.MAX_VALUE
            var maxT = Long.MIN_VALUE
            var timed = 0
            for (i in 0 until seg.size) {
                val t = seg.timestampOrUnknown(i)
                if (t == GeoSegment.TIME_UNKNOWN) continue
                timed++
                if (t < minT) minT = t
                if (t > maxT) maxT = t
            }
            if (timed >= 2 && maxT > minT) {
                durationMs += maxT - minT
                timedDistance += segDistance
                anyDuration = true
            }
        }

        val duration = if (anyDuration) durationMs else null
        val avgSpeed = if (duration != null && duration >= 1000L && timedDistance > 0.0) {
            timedDistance / (duration / 1000.0)
        } else null

        return RouteStats(
            distanceMeters = distance,
            durationMillis = duration,
            elevationGainMeters = if (gainLossPossible) gain else null,
            elevationLossMeters = if (gainLossPossible) loss else null,
            maxElevationMeters = if (eleCount > 0) maxEle else null,
            minElevationMeters = if (eleCount > 0) minEle else null,
            averageSpeedMetersPerSecond = avgSpeed,
            pointCount = segments.sumOf { it.size },
            waypointCount = waypointCount,
            segmentCount = segments.count { it.size > 0 },
        )
    }

    fun segmentDistance(seg: GeoSegment): Double {
        var d = 0.0
        for (i in 1 until seg.size) {
            d += GeoMath.haversineMeters(
                seg.latitude(i - 1), seg.longitude(i - 1),
                seg.latitude(i), seg.longitude(i),
            )
        }
        return d
    }
}
