@file:OptIn(ExperimentalMaterial3Api::class)

package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.model.MapProviderId
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun providerName(id: MapProviderId): String = stringResource(
    when (id) {
        MapProviderId.GOOGLE_MAPS -> R.string.provider_google
        MapProviderId.MAPBOX -> R.string.provider_mapbox
        MapProviderId.ARCGIS -> R.string.provider_arcgis
    },
)

@Composable
fun providerRequirement(id: MapProviderId): String = stringResource(
    when (id) {
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showProviders by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_maps)) }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Provider selector.
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

                // No map is drawn: Phase 1-2 ships no map SDK, so we say so honestly.
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        state.route?.let {
                            Text(
                                stringResource(R.string.map_pending_route, it.name),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProviders) {
        ModalBottomSheet(onDismissRequest = { showProviders = false }, sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.padding(bottom = 24.dp)) {
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
                                onClick = { viewModel.selectProvider(option.id); showProviders = false },
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option.id == state.selected, onClick = null)
                        Spacer(Modifier.size(16.dp))
                        Column {
                            Text(providerName(option.id), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                availabilityText(option.availability),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
