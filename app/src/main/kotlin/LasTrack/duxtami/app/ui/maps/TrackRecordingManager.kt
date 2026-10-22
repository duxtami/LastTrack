package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.core.model.GeoPoint
import LasTrack.duxtami.app.core.model.GeoRoute
import LasTrack.duxtami.app.core.model.GeoSegment
import LasTrack.duxtami.app.core.model.GeoSegmentBuilder
import LasTrack.duxtami.app.core.model.GeoWaypoint
import LasTrack.duxtami.app.core.model.RouteMetadata
import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.core.model.SegmentKind
import LasTrack.duxtami.app.core.model.SourceFormat
import LasTrack.duxtami.app.core.parser.GeoMath
import LasTrack.duxtami.app.core.parser.RouteStatsCalculator
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
}

data class ParkedBikeReminder(
    val latitude: Double,
    val longitude: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class RecordingStats(
    val durationMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val currentElevationMeters: Double? = null,
    val pointCount: Int = 0,
)

@Singleton
class TrackRecordingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routeRepository: RouteRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _status = MutableStateFlow(RecordingStatus.IDLE)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private val _recordedPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val recordedPoints: StateFlow<List<GeoPoint>> = _recordedPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    private val _stats = MutableStateFlow(RecordingStats())
    val stats: StateFlow<RecordingStats> = _stats.asStateFlow()

    private val _parkedBike = MutableStateFlow<ParkedBikeReminder?>(null)
    val parkedBike: StateFlow<ParkedBikeReminder?> = _parkedBike.asStateFlow()

    private val recordedWaypoints = mutableListOf<GeoWaypoint>()
    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L
    private var accumulatedDurationMillis: Long = 0L
    private var lastTimerTickTime: Long = 0L
    private var totalDistanceMeters: Double = 0.0
    private var elevationGainMeters: Double = 0.0
    private var lastRecordedElevation: Double? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            val point = GeoPoint(
                latitude = loc.latitude,
                longitude = loc.longitude,
                elevation = if (loc.hasAltitude()) loc.altitude else null,
                timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis(),
            )
            _currentLocation.value = point

            if (_status.value == RecordingStatus.RECORDING) {
                appendPoint(loc)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationManager == null) return
        try {
            val providers = locationManager.getProviders(true)
            if (LocationManager.GPS_PROVIDER in providers) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener)
            }
            if (LocationManager.NETWORK_PROVIDER in providers) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, locationListener)
            }
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) {
                _currentLocation.value = GeoPoint(best.latitude, best.longitude, if (best.hasAltitude()) best.altitude else null, best.time)
            }
        } catch (_: SecurityException) {
            // Permissions handled by UI launcher
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocationDirectly(): GeoPoint? {
        if (locationManager == null) return _currentLocation.value
        try {
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) {
                val pt = GeoPoint(best.latitude, best.longitude, if (best.hasAltitude()) best.altitude else null, best.time)
                _currentLocation.value = pt
                return pt
            }
        } catch (_: SecurityException) {}
        return _currentLocation.value
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (locationManager == null) return false
        _status.value = RecordingStatus.RECORDING
        _recordedPoints.value = emptyList()
        recordedWaypoints.clear()
        totalDistanceMeters = 0.0
        elevationGainMeters = 0.0
        lastRecordedElevation = null
        accumulatedDurationMillis = 0L
        startTimeMillis = SystemClock.elapsedRealtime()
        lastTimerTickTime = startTimeMillis

        startLocationUpdates()
        startTimer()
        runCatching { TrackerService.start(context) }
        return true
    }

    fun pauseRecording() {
        if (_status.value == RecordingStatus.RECORDING) {
            _status.value = RecordingStatus.PAUSED
            accumulatedDurationMillis += SystemClock.elapsedRealtime() - lastTimerTickTime
            timerJob?.cancel()
        }
    }

    fun resumeRecording() {
        if (_status.value == RecordingStatus.PAUSED) {
            _status.value = RecordingStatus.RECORDING
            lastTimerTickTime = SystemClock.elapsedRealtime()
            startTimer()
        }
    }

    fun stopAndDiscard() {
        runCatching { TrackerService.stop(context) }
        _status.value = RecordingStatus.IDLE
        timerJob?.cancel()
        _recordedPoints.value = emptyList()
        _stats.value = RecordingStats()
    }

    suspend fun stopAndSave(trackName: String? = null, description: String? = null): GeoRoute? {
        val points = _recordedPoints.value
        if (points.isEmpty()) {
            stopAndDiscard()
            return null
        }

        val name = trackName?.trim()?.ifEmpty { null }
            ?: ("Track " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()))

        val builder = GeoSegmentBuilder(points.size)
        points.forEach { pt ->
            builder.add(
                latitude = pt.latitude,
                longitude = pt.longitude,
                elevation = pt.elevation ?: Double.NaN,
                timestamp = pt.timestamp ?: GeoSegment.TIME_UNKNOWN,
            )
        }
        val segment = builder.build(name = name, kind = SegmentKind.TRACK)
        val segments = listOf(segment)

        val waypointsList = ArrayList(recordedWaypoints)
        _parkedBike.value?.let { bike ->
            waypointsList.add(
                GeoWaypoint(
                    latitude = bike.latitude,
                    longitude = bike.longitude,
                    name = "Parked Bike",
                    description = bike.note,
                )
            )
        }

        val computedStats = RouteStatsCalculator.calculate(segments, waypointsList.size)
        val id = UUID.randomUUID().toString()

        val route = GeoRoute(
            id = id,
            name = name,
            sourceFile = "$name.gpx",
            sourceFormat = SourceFormat.GPX,
            segments = segments,
            waypoints = waypointsList,
            metadata = RouteMetadata(description = description ?: "Recorded live in LastTrack"),
            stats = computedStats,
            createdAt = System.currentTimeMillis(),
        )

        // Write a temporary GPX file to fulfill saveRoute requirement
        val tempFile = File(context.cacheDir, "rec_$id.gpx")
        tempFile.writeText(generateGpx(route))

        routeRepository.saveRoute(route, tempFile.absolutePath)
        tempFile.delete()

        stopAndDiscard()
        return route
    }

    fun setParkedBike(note: String = "Parked Bike"): ParkedBikeReminder? {
        val loc = _currentLocation.value ?: _recordedPoints.value.lastOrNull() ?: return null
        val reminder = ParkedBikeReminder(
            latitude = loc.latitude,
            longitude = loc.longitude,
            note = note,
            timestamp = System.currentTimeMillis(),
        )
        _parkedBike.value = reminder
        recordedWaypoints.add(
            GeoWaypoint(
                latitude = reminder.latitude,
                longitude = reminder.longitude,
                name = "Parked Bike",
                description = reminder.note,
            )
        )
        return reminder
    }

    fun clearParkedBike() {
        _parkedBike.value = null
    }

    fun addWaypoint(name: String, description: String? = null) {
        val loc = _currentLocation.value ?: _recordedPoints.value.lastOrNull() ?: return
        recordedWaypoints.add(
            GeoWaypoint(
                latitude = loc.latitude,
                longitude = loc.longitude,
                name = name,
                description = description,
                elevation = loc.elevation,
            )
        )
    }

    private fun appendPoint(loc: Location) {
        val point = GeoPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            elevation = if (loc.hasAltitude()) loc.altitude else null,
            timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis(),
        )

        val currentList = _recordedPoints.value
        val lastPoint = currentList.lastOrNull()

        if (lastPoint != null) {
            val dist = GeoMath.haversineMeters(lastPoint.latitude, lastPoint.longitude, point.latitude, point.longitude)
            if (dist < 1.0) return // filter tiny GPS jitter
            totalDistanceMeters += dist

            point.elevation?.let { ele ->
                val prevEle = lastRecordedElevation
                if (prevEle != null && ele > prevEle + 1.0) {
                    elevationGainMeters += (ele - prevEle)
                }
                lastRecordedElevation = ele
            }
        } else {
            lastRecordedElevation = point.elevation
        }

        val updated = currentList + point
        _recordedPoints.value = updated

        val currentDuration = accumulatedDurationMillis + (SystemClock.elapsedRealtime() - lastTimerTickTime)
        val durationSec = currentDuration / 1000.0
        val currentSpeedKmh = if (loc.hasSpeed()) loc.speed * 3.6 else 0.0
        val avgSpeedKmh = if (durationSec > 2.0 && totalDistanceMeters > 0.0) (totalDistanceMeters / durationSec) * 3.6 else 0.0

        _stats.value = RecordingStats(
            durationMillis = currentDuration,
            distanceMeters = totalDistanceMeters,
            currentSpeedKmh = currentSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            elevationGainMeters = elevationGainMeters,
            currentElevationMeters = point.elevation,
            pointCount = updated.size,
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _status.value == RecordingStatus.RECORDING) {
                delay(1000L)
                val currentDuration = accumulatedDurationMillis + (SystemClock.elapsedRealtime() - lastTimerTickTime)
                val durationSec = currentDuration / 1000.0
                val avg = if (durationSec > 2.0 && totalDistanceMeters > 0.0) (totalDistanceMeters / durationSec) * 3.6 else 0.0

                _stats.value = _stats.value.copy(
                    durationMillis = currentDuration,
                    avgSpeedKmh = avg,
                )
            }
        }
    }

    private fun generateGpx(route: GeoRoute): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="LastTrack" xmlns="http://www.topografix.com/GPX/1/1">""").append("\n")
        sb.append("  <metadata><name>").append(route.name).append("</name></metadata>\n")

        route.waypoints.forEach { wp ->
            sb.append("""  <wpt lat="${wp.latitude}" lon="${wp.longitude}">""").append("\n")
            sb.append("    <name>").append(wp.name ?: "Waypoint").append("</name>\n")
            if (wp.description != null) sb.append("    <desc>").append(wp.description).append("</desc>\n")
            sb.append("  </wpt>\n")
        }

        route.segments.forEach { seg ->
            sb.append("  <trk>\n    <name>").append(route.name).append("</name>\n    <trkseg>\n")
            for (i in 0 until seg.size) {
                val pt = seg[i]
                sb.append("""      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""").append("\n")
                pt.elevation?.let { sb.append("        <ele>").append(it).append("</ele>\n") }
                pt.timestamp?.let {
                    val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(it))
                    sb.append("        <time>").append(iso).append("</time>\n")
                }
                sb.append("      </trkpt>\n")
            }
            sb.append("    </trkseg>\n  </trk>\n")
        }
        sb.append("</gpx>\n")
        return sb.toString()
    }
}
