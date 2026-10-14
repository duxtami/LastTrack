@file:OptIn(ExperimentalMaterial3Api::class)

package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.model.AppPreferences
import LastTrack.duxtami.app.core.model.GeoPoint
import LastTrack.duxtami.app.core.model.MapProviderId
import LastTrack.duxtami.app.core.model.RouteGeometry
import LastTrack.duxtami.app.core.model.RouteLineStyle
import LastTrack.duxtami.app.core.parser.GeoMath
import LastTrack.duxtami.app.ui.common.Formatters
import LastTrack.duxtami.app.ui.common.GoogleMapsLauncher
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.DashPathEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@Composable
fun providerName(id: MapProviderId): String = stringResource(
    when (id) {
        MapProviderId.OPEN_STREET_MAP -> R.string.provider_osm
        MapProviderId.GOOGLE_MAPS -> R.string.provider_google
        MapProviderId.MAPBOX -> R.string.provider_mapbox
        MapProviderId.ARCGIS -> R.string.provider_arcgis
    },
)

@Composable
fun providerRequirement(id: MapProviderId): String = stringResource(
    when (id) {
        MapProviderId.OPEN_STREET_MAP -> R.string.provider_osm_requirement
        MapProviderId.GOOGLE_MAPS -> R.string.provider_google_requirement
        MapProviderId.MAPBOX -> R.string.provider_mapbox_requirement
        MapProviderId.ARCGIS -> R.string.provider_arcgis_requirement
    },
)

@Composable
fun availabilityText(availability: MapAvailability): String = stringResource(
    when (availability) {
        MapAvailability.Ready -> R.string.availability_ready
        is MapAvailability.NotConfigured -> R.string.availability_not_configured
        is MapAvailability.Unavailable -> R.string.availability_unavailable
    },
)

@Composable
fun MapsScreen(viewModel: MapsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showProviders by remember { mutableStateOf(false) }
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    // Dialog states for recording and bike parking
    var showSaveDialog by remember { mutableStateOf(false) }
    var showParkBikeDialog by remember { mutableStateOf(false) }
    var trackNameInput by remember { mutableStateOf("") }
    var trackDescInput by remember { mutableStateOf("") }
    var bikeNoteInput by remember { mutableStateOf("Parked Bike") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startRecording()
        }
    }

    fun checkAndStartRecording() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    // Full screen container with NO top header
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.selectedAvailability is MapAvailability.Ready) {
            // Interactive Ready Map (Google Maps HD, Mapbox with Places, ArcGIS with Places, OpenStreetMap)
            LiveMapView(
                modifier = Modifier.fillMaxSize(),
                provider = state.selected,
                basemapId = state.basemap?.id,
                geometry = state.routeGeometry,
                recordedPoints = state.recordedPoints,
                parkedBike = state.parkedBike,
                preferences = state.preferences,
                onMapCreated = { activeMapView = it },
            )

            // --- Top Floating Header Bar (Provider selector + Google Maps Arrow Button) ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Provider & Basemap Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { showProviders = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "${providerName(state.selected)} · ${state.basemap?.name ?: "Default"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        FilledTonalButton(
                            onClick = { showProviders = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.size(height = 28.dp, width = androidx.compose.ui.unit.Dp.Unspecified),
                        ) {
                            Text(stringResource(R.string.change), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Top Right Arrow Button: Open in Google Maps
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(44.dp),
                ) {
                    IconButton(
                        onClick = {
                            if (state.route != null) {
                                GoogleMapsLauncher.openRoute(context, state.route!!.name, state.routeGeometry)
                            } else {
                                val center = activeMapView?.mapCenter
                                val lat = center?.latitude ?: 20.5937
                                val lon = center?.longitude ?: 78.9629
                                GoogleMapsLauncher.openCoordinates(context, lat, lon, "Location")
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.action_open_google_maps),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // --- Floating Parked Bike Reminder Banner (if set) ---
            state.parkedBike?.let { bike ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(bike.note, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                val distText = state.currentLocation?.let { cur ->
                                    val d = GeoMath.haversineMeters(cur.latitude, cur.longitude, bike.latitude, bike.longitude)
                                    "${Formatters.distance(d)} away"
                                } ?: "Location marked"
                                Text(distText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    GoogleMapsLauncher.openCoordinates(context, bike.latitude, bike.longitude, bike.note)
                                },
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = "Navigate to Bike", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = viewModel::clearParkedBike) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    }
                }
            }

            // --- Map Zoom Controls, My Location & Fit Route Controls ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Park Bike Quick Button
                SmallFloatingActionButton(
                    onClick = { showParkBikeDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(3.dp),
                ) {
                    Icon(Icons.Filled.DirectionsBike, contentDescription = "Park Bike")
                }

                // Current Location Button
                state.currentLocation?.let { loc ->
                    SmallFloatingActionButton(
                        onClick = {
                            activeMapView?.controller?.animateTo(OsmGeoPoint(loc.latitude, loc.longitude))
                            activeMapView?.controller?.setZoom(17.0)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "My Location")
                    }
                }

                if (state.routeGeometry != null) {
                    FloatingActionButton(
                        onClick = {
                            activeMapView?.let { map ->
                                fitRoute(map, state.routeGeometry)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = stringResource(R.string.fit_route), modifier = Modifier.size(20.dp))
                    }
                }

                SmallFloatingActionButton(
                    onClick = { activeMapView?.controller?.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom In")
                }

                SmallFloatingActionButton(
                    onClick = { activeMapView?.controller?.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom Out")
                }
            }

            // --- Bottom Recording Floating Action Button (Bottom Right) ---
            val isRecordingActive = state.recordingStatus != RecordingStatus.IDLE
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (isRecordingActive) 175.dp else if (state.route != null) 100.dp else 24.dp,
                    ),
            ) {
                if (!isRecordingActive) {
                    FloatingActionButton(
                        onClick = { checkAndStartRecording() },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.record_track_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- Bottom Floating Live Recording Dashboard (like Geo Tracker / MB Compass) ---
            if (isRecordingActive) {
                val stats = state.recordingStats
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Header: Recording Status Pill + Timer + Distance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (state.recordingStatus == RecordingStatus.RECORDING) Color.Red else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(10.dp),
                                ) {}
                                Text(
                                    text = if (state.recordingStatus == RecordingStatus.RECORDING) "RECORDING" else "PAUSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.recordingStatus == RecordingStatus.RECORDING) Color.Red else MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            Text(
                                text = Formatters.duration(stats.durationMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = Formatters.distance(stats.distanceMeters),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        HorizontalDivider()

                        // Metrics: Speed, Avg Speed, Altitude
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${String.format(Locale.US, "%.1f", stats.currentSpeedKmh)} km/h",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${String.format(Locale.US, "%.1f", stats.avgSpeedKmh)} km/h",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("Avg Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stats.currentElevationMeters?.let { Formatters.signedElevation(it, false) } ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("Elevation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Action Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Park Bike / Waypoint Pin
                            OutlinedButton(
                                onClick = { showParkBikeDialog = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Park Bike", style = MaterialTheme.typography.labelMedium)
                            }

                            // Pause / Resume
                            FilledTonalButton(
                                onClick = {
                                    if (state.recordingStatus == RecordingStatus.RECORDING) {
                                        viewModel.pauseRecording()
                                    } else {
                                        viewModel.resumeRecording()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    if (state.recordingStatus == RecordingStatus.RECORDING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(if (state.recordingStatus == RecordingStatus.RECORDING) "Pause" else "Resume", style = MaterialTheme.typography.labelMedium)
                            }

                            // Finish & Save
                            Button(
                                onClick = { showSaveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1.2f),
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Finish", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else if (state.route != null) {
                // Bottom Floating Route Summary Card (when viewing an existing route)
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                ) {
                    val s = state.route!!.stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = state.route!!.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    Formatters.distance(s.distanceMeters),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                s.durationMillis?.let {
                                    Text(
                                        "•  ${Formatters.duration(it)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                s.elevationGainMeters?.let {
                                    Text(
                                        "•  ${Formatters.signedElevation(it, true)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                GoogleMapsLauncher.openRoute(context, state.route!!.name, state.routeGeometry)
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(start = 12.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.google_maps_short), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            // Fallback (if provider reports not configured)
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Filled.Layers, contentDescription = null) },
                        overlineContent = { Text(stringResource(R.string.map_provider)) },
                        headlineContent = { Text(providerName(state.selected)) },
                        supportingContent = {
                            val base = availabilityText(state.selectedAvailability)
                            Text(state.basemap?.let { "$base · ${it.name}" } ?: base)
                        },
                        trailingContent = {
                            FilledTonalButton(onClick = { showProviders = true }) { Text(stringResource(R.string.change)) }
                        },
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(88.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Text(
                            stringResource(R.string.provider_not_configured_title, providerName(state.selected)),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(R.string.provider_not_configured_message, providerRequirement(state.selected)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = { viewModel.selectProvider(MapProviderId.GOOGLE_MAPS) },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Switch to Google Maps")
                        }
                    }
                }
            }
        }
    }

    // --- Provider and Basemap Switcher BottomSheet ---
    if (showProviders) {
        ModalBottomSheet(onDismissRequest = { showProviders = false }, sheetState = rememberModalBottomSheetState()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    stringResource(R.string.map_provider),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                state.providers.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option.id == state.selected,
                                role = Role.RadioButton,
                                onClick = {
                                    viewModel.selectProvider(option.id)
                                    showProviders = false
                                },
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option.id == state.selected, onClick = null)
                        Spacer(Modifier.size(16.dp))
                        Column {
                            Text(providerName(option.id), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                availabilityText(option.availability),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                if (state.availableBasemaps.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        stringResource(R.string.basemap_select),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                    state.availableBasemaps.forEach { style ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = style.id == state.basemap?.id,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.selectBasemap(style.id)
                                        showProviders = false
                                    },
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = style.id == state.basemap?.id, onClick = null)
                            Spacer(Modifier.size(16.dp))
                            Text(style.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // --- Save Track Dialog ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_track_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = trackNameInput,
                        onValueChange = { trackNameInput = it },
                        label = { Text(stringResource(R.string.track_name_label)) },
                        placeholder = { Text("e.g. Morning Ride") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = trackDescInput,
                        onValueChange = { trackDescInput = it },
                        label = { Text(stringResource(R.string.track_description_label)) },
                        placeholder = { Text("Optional notes") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.stopAndSave(trackNameInput, trackDescInput)
                            showSaveDialog = false
                            trackNameInput = ""
                            trackDescInput = ""
                        }
                    },
                ) {
                    Text("Save Track")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.stopAndDiscard()
                        showSaveDialog = false
                    }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
        )
    }

    // --- Park Bike / Waypoint Dialog ---
    if (showParkBikeDialog) {
        AlertDialog(
            onDismissRequest = { showParkBikeDialog = false },
            icon = { Icon(Icons.Filled.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.park_bike_reminder_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.park_bike_reminder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = bikeNoteInput,
                        onValueChange = { bikeNoteInput = it },
                        label = { Text("Note / Location Details") },
                        placeholder = { Text("e.g. Parked near Pillar B2") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setParkedBike(bikeNoteInput)
                        showParkBikeDialog = false
                    },
                ) {
                    Text("Set Reminder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParkBikeDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun LiveMapView(
    modifier: Modifier = Modifier,
    provider: MapProviderId,
    basemapId: String?,
    geometry: RouteGeometry?,
    recordedPoints: List<GeoPoint>,
    parkedBike: ParkedBikeReminder?,
    preferences: AppPreferences,
    onMapCreated: (MapView) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            controller.setZoom(5.0)
            controller.setCenter(OsmGeoPoint(20.5937, 78.9629))
            onMapCreated(this)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    LaunchedEffect(provider, basemapId) {
        val tileSource = MapTileSources.resolve(provider, basemapId)
        mapView.setTileSource(tileSource)
        mapView.invalidate()
    }

    LaunchedEffect(geometry, recordedPoints, parkedBike, preferences) {
        mapView.overlays.clear()
        val allSegments = geometry?.segments.orEmpty()
        val allPoints = allSegments.flatMap { it.points() }

        val density = context.resources.displayMetrics.density
        val lineWidthPx = preferences.routeLineWidth.dp * density

        // 1. Draw loaded route geometry (if any)
        if (allPoints.isNotEmpty()) {
            val strokeColor = android.graphics.Color.parseColor("#1E88E5")
            allSegments.forEach { segment ->
                val polyline = Polyline(mapView).apply {
                    outlinePaint.color = strokeColor
                    outlinePaint.strokeWidth = lineWidthPx
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    if (preferences.routeLineStyle == RouteLineStyle.DASHED) {
                        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(24f, 16f), 0f)
                    }
                    setPoints(segment.points().map { OsmGeoPoint(it.latitude, it.longitude) }.toList())
                }
                mapView.overlays.add(polyline)
            }

            if (preferences.showStartEndMarkers) {
                val startPt = allPoints.first()
                val endPt = allPoints.last()

                val startMarker = Marker(mapView).apply {
                    position = OsmGeoPoint(startPt.latitude, startPt.longitude)
                    title = "Start"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                val endMarker = Marker(mapView).apply {
                    position = OsmGeoPoint(endPt.latitude, endPt.longitude)
                    title = "Finish"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(startMarker)
                mapView.overlays.add(endMarker)
            }

            if (preferences.showWaypoints && geometry?.waypoints?.isNotEmpty() == true) {
                geometry.waypoints.forEach { wp ->
                    val wpMarker = Marker(mapView).apply {
                        position = OsmGeoPoint(wp.latitude, wp.longitude)
                        title = wp.name
                        snippet = wp.description
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(wpMarker)
                }
            }

            if (preferences.autoFitRoute) {
                fitRoute(mapView, geometry)
            }
        }

        // 2. Draw live recorded track polyline (in vibrant red/crimson)
        if (recordedPoints.size >= 2) {
            val recPolyline = Polyline(mapView).apply {
                outlinePaint.color = android.graphics.Color.parseColor("#FF1744")
                outlinePaint.strokeWidth = 6.dp * density
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                setPoints(recordedPoints.map { OsmGeoPoint(it.latitude, it.longitude) })
            }
            mapView.overlays.add(recPolyline)

            // Current head marker
            val last = recordedPoints.last()
            val currentHead = Marker(mapView).apply {
                position = OsmGeoPoint(last.latitude, last.longitude)
                title = "Current Position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(currentHead)
        }

        // 3. Draw Parked Bike Marker
        parkedBike?.let { bike ->
            val bikeMarker = Marker(mapView).apply {
                position = OsmGeoPoint(bike.latitude, bike.longitude)
                title = "Parked Bike"
                snippet = bike.note
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(bikeMarker)
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun fitRoute(mapView: MapView, geometry: RouteGeometry?) {
    val allPoints = geometry?.segments.orEmpty().flatMap { it.points() }
    if (allPoints.isEmpty()) return

    val minLat = allPoints.minOf { it.latitude }
    val maxLat = allPoints.maxOf { it.latitude }
    val minLon = allPoints.minOf { it.longitude }
    val maxLon = allPoints.maxOf { it.longitude }
    val delta = 0.002
    val box = BoundingBox(
        if (maxLat == minLat) maxLat + delta else maxLat,
        if (maxLon == minLon) maxLon + delta else maxLon,
        if (maxLat == minLat) minLat - delta else minLat,
        if (maxLon == minLon) minLon - delta else minLon,
    )
    mapView.post {
        mapView.zoomToBoundingBox(box, true, 90)
    }
}
