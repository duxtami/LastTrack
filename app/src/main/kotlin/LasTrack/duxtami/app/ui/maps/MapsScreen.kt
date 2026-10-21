@file:OptIn(ExperimentalMaterial3Api::class)

package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.R
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.model.AppPreferences
import LasTrack.duxtami.app.core.model.GeoPoint
import LasTrack.duxtami.app.core.model.MapProviderId
import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.RouteGeometry
import LasTrack.duxtami.app.core.model.RouteLineStyle
import LasTrack.duxtami.app.core.parser.GeoMath
import LasTrack.duxtami.app.ui.common.Formatters
import LasTrack.duxtami.app.ui.common.GoogleMapsLauncher
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
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

data class SelectedMapPlace(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val address: String,
)

@Composable
fun MapsScreen(
    targetLat: Double? = null,
    targetLon: Double? = null,
    targetTitle: String? = null,
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showProviders by remember { mutableStateOf(false) }
    var activeMapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Dialog states for recording and bike parking
    var showSaveDialog by remember { mutableStateOf(false) }
    var showParkBikeDialog by remember { mutableStateOf(false) }
    var trackNameInput by remember { mutableStateOf("") }
    var trackDescInput by remember { mutableStateOf("") }
    var bikeNoteInput by remember { mutableStateOf("Parked Bike") }

    // Search and selected place states
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<SelectedMapPlace?>(null) }

    // If navigated from Saved Places with target coordinates, auto-center and select
    LaunchedEffect(targetLat, targetLon, activeMapView) {
        if (targetLat != null && targetLon != null) {
            selectedPlace = SelectedMapPlace(targetLat, targetLon, targetTitle ?: "Selected Location", "")
            activeMapView?.controller?.let { ctrl ->
                ctrl.animateTo(OsmGeoPoint(targetLat, targetLon))
                ctrl.setZoom(17.5)
            }
        }
    }

    // Handle system back: if route is loaded or place is selected, clear it
    BackHandler(enabled = state.route != null || selectedPlace != null) {
        if (selectedPlace != null) {
            selectedPlace = null
        } else if (state.route != null) {
            viewModel.clearActiveRoute()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.recordingManager.startLocationUpdates()
            locationOverlayRef?.enableMyLocation()
        }
    }

    // Check location permission on enter
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.recordingManager.startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
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

    fun animateToCurrentLocation() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
            return
        }

        viewModel.recordingManager.startLocationUpdates()
        val loc = viewModel.recordingManager.getCurrentLocationDirectly() ?: state.currentLocation
        if (loc != null) {
            activeMapView?.controller?.animateTo(OsmGeoPoint(loc.latitude, loc.longitude))
            activeMapView?.controller?.setZoom(17.5)
        } else {
            locationOverlayRef?.myLocation?.let { myLoc ->
                activeMapView?.controller?.animateTo(myLoc)
                activeMapView?.controller?.setZoom(17.5)
            } ?: run {
                Toast.makeText(context, "Acquiring GPS location…", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.selectedAvailability is MapAvailability.Ready) {
            // Interactive Ready Map with multi-touch rotation, zoom, live location, and click-to-select
            LiveMapView(
                modifier = Modifier.fillMaxSize(),
                provider = state.selected,
                basemapId = state.basemap?.id,
                geometry = state.routeGeometry,
                recordedPoints = state.recordedPoints,
                parkedBike = state.parkedBike,
                selectedPlace = selectedPlace,
                preferences = state.preferences,
                onMapCreated = { activeMapView = it },
                onLocationOverlayCreated = { locationOverlayRef = it },
                onMapClicked = { lat, lon ->
                    val defaultTitle = String.format(Locale.US, "%.5f, %.5f", lat, lon)
                    selectedPlace = SelectedMapPlace(lat, lon, defaultTitle, "Fetching address…")
                    scope.launch {
                        val details = PlaceSearchManager.reverseGeocode(lat, lon)
                        selectedPlace = SelectedMapPlace(lat, lon, details.name, details.address)
                    }
                },
            )

            // --- Top Floating Header Bar ---
            // Left: Provider & Basemap Pill (flex weight to prevent overlap)
            // Right: Open in Google Maps Icon + Search Icon (with ideal spacing and dedicated surfaces)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Provider & Basemap Pill (Clean, whole pill is clickable, cannot overlap right buttons)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { showProviders = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Layers,
                            contentDescription = "Map Style",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "${providerName(state.selected)} · ${state.basemap?.name ?: "Default"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Top Right Action Buttons: Open in Google Maps + Search (No overlaps, ideal distance)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Open in Google Maps Button (Available on all selected maps)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                        shadowElevation = 5.dp,
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
                            modifier = Modifier.size(42.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.action_open_google_maps),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Search Button (Search any place)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                        shadowElevation = 5.dp,
                    ) {
                        IconButton(
                            onClick = { showSearchDialog = true },
                            modifier = Modifier.size(42.dp),
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search places",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // --- Top Loaded Route Chip / Dismiss Bar (if viewing an imported route) ---
            state.route?.let { route ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = route.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${Formatters.distance(route.stats.distanceMeters)} · Path active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = viewModel::clearActiveRoute) {
                            Icon(Icons.Filled.Close, contentDescription = "Close route", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                        .padding(top = if (state.route != null) 124.dp else 64.dp, start = 16.dp, end = 16.dp)
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

            // --- Floating Map Controls (Right Side) ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Reset North / Compass Button
                if (activeMapView != null && activeMapView!!.mapOrientation != 0.0f) {
                    SmallFloatingActionButton(
                        onClick = {
                            activeMapView?.setMapOrientation(0.0f, true)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    ) {
                        Icon(Icons.Filled.Explore, contentDescription = "Reset North", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Park Bike Quick Button
                SmallFloatingActionButton(
                    onClick = { showParkBikeDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(3.dp),
                ) {
                    Icon(Icons.Filled.DirectionsBike, contentDescription = "Park Bike")
                }

                // Live "My Location" Button (Always present, centers on current location)
                SmallFloatingActionButton(
                    onClick = { animateToCurrentLocation() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(3.dp),
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "My Live Location")
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

            // --- Selected Place Bottom Card (Google Maps style when clicking any place) ---
            selectedPlace?.let { place ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = if (state.recordingStatus != RecordingStatus.IDLE) 180.dp else 0.dp)
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = place.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { selectedPlace = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    viewModel.savePlace(
                                        name = place.title,
                                        latitude = place.latitude,
                                        longitude = place.longitude,
                                        category = PlaceCategory.FAVORITE,
                                        description = place.address,
                                    )
                                    Toast.makeText(context, "Added to Saved Places", Toast.LENGTH_SHORT).show()
                                    selectedPlace = null
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Add to Places", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    GoogleMapsLauncher.openCoordinates(context, place.latitude, place.longitude, place.title)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Google Maps", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // --- Bottom Recording Floating Action Button ---
            // Requirement: When idle, theme color with ONLY record icon. When recording, red with full text/dashboard!
            val isRecordingActive = state.recordingStatus != RecordingStatus.IDLE
            if (!isRecordingActive && selectedPlace == null) {
                FloatingActionButton(
                    onClick = { checkAndStartRecording() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp)
                        .size(56.dp),
                ) {
                    Icon(
                        Icons.Filled.FiberManualRecord,
                        contentDescription = "Start Recording",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // --- Bottom Floating Live Recording Dashboard ---
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
                        // Header: Red status pill + Timer + Distance
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
                            OutlinedButton(
                                onClick = { showParkBikeDialog = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Park Bike", style = MaterialTheme.typography.labelMedium)
                            }

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
            }
        } else {
            // Fallback screen
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
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().clickable { showProviders = true },
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Filled.Layers, contentDescription = null) },
                        overlineContent = { Text(stringResource(R.string.map_provider)) },
                        headlineContent = { Text(providerName(state.selected)) },
                        supportingContent = {
                            val base = availabilityText(state.selectedAvailability)
                            Text(state.basemap?.let { "$base · ${it.name}" } ?: base)
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

    // --- Search Places Dialog ---
    if (showSearchDialog) {
        var queryInput by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Places") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = {
                            queryInput = it
                            if (it.length >= 2) {
                                isSearching = true
                                scope.launch {
                                    searchResults = PlaceSearchManager.search(it)
                                    isSearching = false
                                }
                            } else {
                                searchResults = emptyList()
                            }
                        },
                        placeholder = { Text("Search cities, places, roads…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (queryInput.isNotEmpty()) {
                                IconButton(onClick = { queryInput = ""; searchResults = emptyList() }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                    ) {
                        items(searchResults) { result ->
                            ListItem(
                                headlineContent = { Text(result.title, fontWeight = FontWeight.Bold, maxLines = 1) },
                                supportingContent = { Text(result.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.clickable {
                                    showSearchDialog = false
                                    selectedPlace = SelectedMapPlace(
                                        latitude = result.latitude,
                                        longitude = result.longitude,
                                        title = result.title,
                                        address = result.subtitle,
                                    )
                                    activeMapView?.controller?.animateTo(OsmGeoPoint(result.latitude, result.longitude))
                                    activeMapView?.controller?.setZoom(17.0)
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Close")
                }
            },
        )
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
                            val styleDesc = when (option.id) {
                                MapProviderId.OPEN_STREET_MAP -> "Standard, White & Dark styles"
                                MapProviderId.GOOGLE_MAPS -> "Satellite Hybrid HD, Streets & Terrain"
                                MapProviderId.MAPBOX -> "Outdoors & Streets (Rich POIs)"
                                MapProviderId.ARCGIS -> "Official World Streets & Topo"
                            }
                            Text(
                                styleDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                if (state.availableBasemaps.size > 1) {
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
    selectedPlace: SelectedMapPlace?,
    preferences: AppPreferences,
    onMapCreated: (MapView) -> Unit,
    onLocationOverlayCreated: (MyLocationNewOverlay) -> Unit,
    onMapClicked: (latitude: Double, longitude: Double) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().apply {
            tileDownloadThreads = 8
            tileDownloadMaxQueueSize = 80
            cacheMapTileCount = 36
        }
    }

    val arrowBitmap = remember(context) { createNavigationArrowBitmap(context) }
    val dotBitmap = remember(context) { createPersonDotBitmap(context) }

    val dynamicOverlayFolder = remember { FolderOverlay() }
    var labelTilesOverlay by remember { mutableStateOf<TilesOverlay?>(null) }
    var locOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            controller.setZoom(5.0)
            controller.setCenter(OsmGeoPoint(20.5937, 78.9629))

            // 1. Multi-Touch Dynamic Rotation Overlay
            val rotationOverlay = RotationGestureOverlay(this).apply {
                isEnabled = true
            }
            overlays.add(rotationOverlay)

            // 2. Click / Tap Overlay (for place selection)
            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmGeoPoint): Boolean {
                    onMapClicked(p.latitude, p.longitude)
                    return true
                }
                override fun longPressHelper(p: OsmGeoPoint): Boolean {
                    onMapClicked(p.latitude, p.longitude)
                    return true
                }
            })
            overlays.add(eventsOverlay)

            // 3. Live MyLocation Overlay with compass heading rotation
            val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply {
                setDirectionIcon(arrowBitmap)
                setPersonIcon(dotBitmap)
                setDirectionAnchor(0.5f, 0.5f)
                setPersonAnchor(0.5f, 0.5f)
                enableMyLocation()
            }
            overlays.add(locOverlay)
            locOverlayRef = locOverlay
            onLocationOverlayCreated(locOverlay)

            // 4. Dynamic layers container (routes, tracks, pins)
            overlays.add(dynamicOverlayFolder)

            onMapCreated(this)
        }
    }

    val orientationListener = remember(context) {
        DeviceOrientationListener(context) { heading ->
            locOverlayRef?.let { overlay ->
                val last = overlay.lastFix
                if (last != null) {
                    val updated = android.location.Location(last).apply {
                        bearing = heading
                    }
                    overlay.onLocationChanged(updated, null)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    locOverlayRef?.enableMyLocation()
                    orientationListener.start()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    orientationListener.stop()
                    locOverlayRef?.disableMyLocation()
                    mapView.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            orientationListener.stop()
            labelTilesOverlay?.let {
                mapView.overlays.remove(it)
                it.onDetach(mapView)
            }
            labelTilesOverlay = null
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    LaunchedEffect(provider, basemapId) {
        val tileSource = MapTileSources.resolve(provider, basemapId)
        mapView.setTileSource(tileSource)
        // 512px HD tiles render at native retina sharpness (scale=false), 256px scale up for readability
        mapView.isTilesScaledToDpi = (tileSource.tileSizePixels == 256)

        labelTilesOverlay?.let {
            mapView.overlays.remove(it)
            it.onDetach(mapView)
        }
        labelTilesOverlay = null

        val overlaySource = MapTileSources.resolveOverlay(provider, basemapId)
        if (overlaySource != null) {
            val tileProvider = MapTileProviderBasic(context, overlaySource)
            val newOverlay = TilesOverlay(tileProvider, context).apply {
                loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                loadingLineColor = android.graphics.Color.TRANSPARENT
            }
            val insertIdx = mapView.overlays.indexOf(dynamicOverlayFolder).let { if (it >= 0) it else 0 }
            mapView.overlays.add(insertIdx, newOverlay)
            labelTilesOverlay = newOverlay
        }

        mapView.invalidate()
    }

    LaunchedEffect(geometry, recordedPoints, parkedBike, selectedPlace, preferences) {
        dynamicOverlayFolder.items.clear()
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
                dynamicOverlayFolder.add(polyline)
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
                dynamicOverlayFolder.add(startMarker)
                dynamicOverlayFolder.add(endMarker)
            }

            if (preferences.showWaypoints && geometry?.waypoints?.isNotEmpty() == true) {
                geometry.waypoints.forEach { wp ->
                    val wpMarker = Marker(mapView).apply {
                        position = OsmGeoPoint(wp.latitude, wp.longitude)
                        title = wp.name
                        snippet = wp.description
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    dynamicOverlayFolder.add(wpMarker)
                }
            }

            if (preferences.autoFitRoute) {
                fitRoute(mapView, geometry)
            }
        }

        // 2. Draw live recorded track polyline (in vibrant crimson)
        if (recordedPoints.size >= 2) {
            val recPolyline = Polyline(mapView).apply {
                outlinePaint.color = android.graphics.Color.parseColor("#FF1744")
                outlinePaint.strokeWidth = 6f * density
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                setPoints(recordedPoints.map { OsmGeoPoint(it.latitude, it.longitude) })
            }
            dynamicOverlayFolder.add(recPolyline)

            val last = recordedPoints.last()
            val currentHead = Marker(mapView).apply {
                position = OsmGeoPoint(last.latitude, last.longitude)
                title = "Recording Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            dynamicOverlayFolder.add(currentHead)
        }

        // 3. Draw Parked Bike Marker
        parkedBike?.let { bike ->
            val bikeMarker = Marker(mapView).apply {
                position = OsmGeoPoint(bike.latitude, bike.longitude)
                title = "Parked Bike"
                snippet = bike.note
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            dynamicOverlayFolder.add(bikeMarker)
        }

        // 4. Draw Selected Place Pin (from tap or search)
        selectedPlace?.let { sel ->
            val pin = Marker(mapView).apply {
                position = OsmGeoPoint(sel.latitude, sel.longitude)
                title = sel.title
                snippet = sel.address
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            dynamicOverlayFolder.add(pin)
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

private fun createNavigationArrowBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt().coerceAtLeast(36)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#1A73E8") // Google Maps navigation blue
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#33000000")
    }

    val shadowPath = Path().apply {
        moveTo(size / 2f, size * 0.12f)
        lineTo(size * 0.85f, size * 0.90f)
        lineTo(size / 2f, size * 0.70f)
        lineTo(size * 0.15f, size * 0.90f)
        close()
    }
    val path = Path().apply {
        moveTo(size / 2f, size * 0.09f)
        lineTo(size * 0.85f, size * 0.87f)
        lineTo(size / 2f, size * 0.67f)
        lineTo(size * 0.15f, size * 0.87f)
        close()
    }
    canvas.drawPath(shadowPath, shadowPaint)
    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
    return bitmap
}

private fun createPersonDotBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt().coerceAtLeast(28)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#334285F4")
    }
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#1A73E8")
    }

    canvas.drawCircle(center, center, center, auraPaint)
    canvas.drawCircle(center, center, center * 0.75f, whitePaint)
    canvas.drawCircle(center, center, center * 0.52f, bluePaint)
    return bitmap
}

/**
 * Listens to device hardware rotation/orientation sensors to provide real-time compass heading
 * for rotating the current location navigation arrow.
 */
class DeviceOrientationListener(
    context: Context,
    private val onHeadingChanged: (Float) -> Unit,
) : android.hardware.SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
    @Suppress("DEPRECATION")
    private val rotationSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ORIENTATION)

    fun start() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        if (event.sensor.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            android.hardware.SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val heading = (azimuthDeg + 360f) % 360f
            onHeadingChanged(heading)
        } else {
            val heading = (event.values[0] + 360f) % 360f
            onHeadingChanged(heading)
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
}
