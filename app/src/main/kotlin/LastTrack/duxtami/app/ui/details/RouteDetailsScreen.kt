@file:OptIn(ExperimentalMaterial3Api::class)

package LastTrack.duxtami.app.ui.details

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.core.model.RouteSummary
import LastTrack.duxtami.app.ui.common.DeleteRouteDialog
import LastTrack.duxtami.app.ui.common.EmptyState
import LastTrack.duxtami.app.ui.common.FormatBadge
import LastTrack.duxtami.app.ui.common.Formatters
import LastTrack.duxtami.app.ui.common.GoogleMapsLauncher
import LastTrack.duxtami.app.ui.common.RenameRouteDialog
import LastTrack.duxtami.app.ui.common.UiMessage
import LastTrack.duxtami.app.ui.common.rememberRouteFileActions
import LastTrack.duxtami.app.ui.common.text
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun RouteDetailsScreen(
    onBack: () -> Unit,
    onOpenOnMap: (String) -> Unit,
    viewModel: RouteDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messageTexts = UiMessage.entries.associateWith { it.text() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(messageTexts.getValue(it)) } }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val fileActions = rememberRouteFileActions(
        prepareShare = { viewModel.prepareShare() },
        onExportToUri = { _, uri -> viewModel.export(uri) },
    )
    val loaded = state as? DetailsUiState.Loaded

    val handleOpenInGoogleMaps = {
        if (loaded != null) {
            scope.launch {
                val geometry = viewModel.getRouteGeometry()
                GoogleMapsLauncher.openRoute(context, loaded.route.name, geometry)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loaded?.route?.name ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (loaded != null) {
                        IconButton(onClick = { handleOpenInGoogleMaps() }) {
                            Icon(Icons.Filled.Navigation, contentDescription = stringResource(R.string.open_in_google_maps))
                        }
                        IconButton(onClick = { renaming = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_rename))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (val s = state) {
                DetailsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                DetailsUiState.NotFound -> EmptyState(
                    icon = Icons.Filled.Route,
                    title = stringResource(R.string.route_not_found_title),
                    description = stringResource(R.string.route_not_found_message),
                    actionLabel = stringResource(R.string.cd_back),
                    onAction = onBack,
                )
                is DetailsUiState.Loaded -> DetailsContent(
                    route = s.route,
                    onOpenOnMap = { onOpenOnMap(s.route.id) },
                    onOpenInGoogleMaps = handleOpenInGoogleMaps,
                    onExport = { fileActions.export(s.route) },
                    onShare = { fileActions.share(s.route) },
                    onDelete = { deleting = true },
                )
            }
        }
    }

    if (renaming && loaded != null) {
        RenameRouteDialog(
            initialName = loaded.route.name,
            onConfirm = { viewModel.rename(it); renaming = false },
            onDismiss = { renaming = false },
        )
    }
    if (deleting && loaded != null) {
        DeleteRouteDialog(
            routeName = loaded.route.name,
            onConfirm = { deleting = false; viewModel.delete(onDone = onBack) },
            onDismiss = { deleting = false },
        )
    }
}

private data class Metric(val icon: ImageVector, val label: Int, val value: String)

@Composable
private fun DetailsContent(
    route: RouteSummary,
    onOpenOnMap: () -> Unit,
    onOpenInGoogleMaps: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val stats = route.stats

    val metrics = buildList {
        add(Metric(Icons.Filled.Straighten, R.string.metric_distance, Formatters.distance(stats.distanceMeters)))
        stats.durationMillis?.let { add(Metric(Icons.Filled.Schedule, R.string.metric_duration, Formatters.duration(it))) }
        stats.averageSpeedMetersPerSecond?.let { add(Metric(Icons.Filled.Speed, R.string.metric_avg_speed, Formatters.speedKmh(it))) }
        stats.elevationGainMeters?.let { add(Metric(Icons.Filled.TrendingUp, R.string.metric_elevation_gain, Formatters.signedElevation(it, true))) }
        stats.elevationLossMeters?.let { add(Metric(Icons.Filled.TrendingDown, R.string.metric_elevation_loss, Formatters.signedElevation(it, false))) }
        stats.maxElevationMeters?.let { add(Metric(Icons.Filled.Terrain, R.string.metric_highest, Formatters.elevation(it))) }
        stats.minElevationMeters?.let { add(Metric(Icons.Filled.ArrowDownward, R.string.metric_lowest, Formatters.elevation(it))) }
        add(Metric(Icons.Filled.Timeline, R.string.metric_points, Formatters.count(stats.pointCount)))
        if (stats.waypointCount > 0) add(Metric(Icons.Filled.Place, R.string.metric_waypoints, Formatters.count(stats.waypointCount)))
    }

    Column(
        Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FormatBadge(route.format)
            Column {
                Text(route.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Text(
                    stringResource(R.string.imported_on, Formatters.dateTime(route.importedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        route.description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 4, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { MetricCard(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (!stats.hasElevation) {
            Text(
                stringResource(R.string.elevation_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.file_section), style = MaterialTheme.typography.titleMedium)
                Text(route.fileName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${route.format.displayName} · ${Formatter.formatShortFileSize(context, route.fileSizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(onClick = onOpenOnMap, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.open_on_map))
        }

        FilledTonalButton(onClick = onOpenInGoogleMaps, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.open_in_google_maps))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_export))
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_share))
            }
        }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun MetricCard(metric: Metric, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(metric.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(metric.value, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(metric.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
