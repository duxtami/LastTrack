@file:OptIn(ExperimentalMaterial3Api::class)

package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.model.AppPreferences
import LastTrack.duxtami.app.core.model.MapProviderId
import LastTrack.duxtami.app.core.model.RouteGeometry
import LastTrack.duxtami.app.core.model.RouteLineStyle
import LastTrack.duxtami.app.ui.common.Formatters
import LastTrack.duxtami.app.ui.common.GoogleMapsLauncher
import android.graphics.DashPathEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showProviders by remember { mutableStateOf(false) }
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_maps)) }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.selectedAvailability is MapAvailability.Ready) {
                // Interactive Ready Map (Google Maps, Mapbox, ArcGIS, OpenStreetMap)
                LiveMapView(
                    modifier = Modifier.fillMaxSize(),
                    provider = state.selected,
                    basemapId = state.basemap?.id,
                    geometry = state.routeGeometry,
                    preferences = state.preferences,
                    onMapCreated = { activeMapView = it },
                )

                // Top Floating Provider Bar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { showProviders = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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

                // Map Zoom Controls & Fit Route FAB
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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

                // Bottom Floating Route Summary Card or Explore Hint
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
                    if (state.route != null) {
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
                                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.google_maps_short), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                stringResource(R.string.no_route_loaded_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.no_route_loaded_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                // Fallback (if any provider ever reports not configured)
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
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
    }

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
}

@Composable
fun LiveMapView(
    modifier: Modifier = Modifier,
    provider: MapProviderId,
    basemapId: String?,
    geometry: RouteGeometry?,
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
            controller.setCenter(GeoPoint(20.5937, 78.9629))
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
            mapView.onDetach()
        }
    }

    LaunchedEffect(provider, basemapId) {
        val tileSource = MapTileSources.resolve(provider, basemapId)
        mapView.setTileSource(tileSource)
        mapView.invalidate()
    }

    LaunchedEffect(geometry, preferences) {
        mapView.overlays.clear()
        val allSegments = geometry?.segments.orEmpty()
        val allPoints = allSegments.flatMap { it.points() }

        if (allPoints.isNotEmpty()) {
            val density = context.resources.displayMetrics.density
            val lineWidthPx = preferences.routeLineWidth.dp * density
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
                    setPoints(segment.points().map { GeoPoint(it.latitude, it.longitude) }.toList())
                }
                mapView.overlays.add(polyline)
            }

            if (preferences.showStartEndMarkers) {
                val startPt = allPoints.first()
                val endPt = allPoints.last()

                val startMarker = Marker(mapView).apply {
                    position = GeoPoint(startPt.latitude, startPt.longitude)
                    title = "Start"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                val endMarker = Marker(mapView).apply {
                    position = GeoPoint(endPt.latitude, endPt.longitude)
                    title = "Finish"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(startMarker)
                mapView.overlays.add(endMarker)
            }

            if (preferences.showWaypoints && geometry?.waypoints?.isNotEmpty() == true) {
                geometry.waypoints.forEach { wp ->
                    val wpMarker = Marker(mapView).apply {
                        position = GeoPoint(wp.latitude, wp.longitude)
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
