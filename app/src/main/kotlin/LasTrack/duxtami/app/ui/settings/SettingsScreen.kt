@file:OptIn(ExperimentalMaterial3Api::class)

package LasTrack.duxtami.app.ui.settings

import LasTrack.duxtami.app.R
import LasTrack.duxtami.app.core.model.AppPreferences
import LasTrack.duxtami.app.core.model.MapProviderId
import LasTrack.duxtami.app.core.model.RouteLineStyle
import LasTrack.duxtami.app.core.model.RouteLineWidth
import LasTrack.duxtami.app.core.model.ThemeMode
import LasTrack.duxtami.app.ui.common.ChoiceDialog
import LasTrack.duxtami.app.ui.maps.providerName
import LasTrack.duxtami.app.ui.settings.backup.BackupRestoreScreen
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class SettingsDialog { PROVIDER, BASEMAP, LINE_WIDTH, LINE_STYLE, PRIVACY, LICENSES }

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var showBackupRestore by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }

    if (showBackupRestore) {
        BackHandler { showBackupRestore = false }
        BackupRestoreScreen(
            backupManager = viewModel.backupManager,
            onBack = { showBackupRestore = false },
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Configure application preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. App Theme Card (Matching Screenshot_20261022-091311.png)
                ThemeSelectorCard(
                    title = "App Theme",
                    subtitle = "Choose whether the application uses light, dark, or system theme.",
                    icon = Icons.Filled.DarkMode,
                    selected = prefs.themeMode,
                    onSelect = { mode -> viewModel.update { it.copy(themeMode = mode) } },
                )

                // 2. Map Theme Card (Matching Screenshot_20261022-091311.png)
                ThemeSelectorCard(
                    title = "Map Theme",
                    subtitle = "Choose whether the map uses light, dark, or system theme independently.",
                    icon = Icons.Filled.Map,
                    selected = if (prefs.basemapId.contains("dark")) ThemeMode.DARK else if (prefs.basemapId.contains("white") || prefs.basemapId.contains("positron")) ThemeMode.LIGHT else ThemeMode.SYSTEM,
                    onSelect = { mode ->
                        viewModel.update { current ->
                            val newBasemap = when (mode) {
                                ThemeMode.DARK -> if (current.mapProvider == MapProviderId.GOOGLE_MAPS) "google_dark" else if (current.mapProvider == MapProviderId.GEOAPIFY) "geoapify_dark" else "osm_dark"
                                ThemeMode.LIGHT -> if (current.mapProvider == MapProviderId.GEOAPIFY) "geoapify_positron" else "osm_white"
                                ThemeMode.SYSTEM -> "default"
                            }
                            current.copy(basemapId = newBasemap)
                        }
                    },
                )

                // 3. Backup & Restore Card (Matching Screenshot_20261022-090620.png entry)
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBackupRestore = true },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Backup,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Backup & Restore",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Back up or restore your data to your device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 4. Map Provider & Basemap Configuration
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Map Provider & Styles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        ValueRow(R.string.setting_default_provider, providerName(prefs.mapProvider)) { dialog = SettingsDialog.PROVIDER }
                        ValueRow(R.string.setting_default_basemap, state.selectedBasemap?.name ?: "-") { dialog = SettingsDialog.BASEMAP }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SwitchRow(R.string.setting_auto_fit, R.string.setting_auto_fit_desc, prefs.autoFitRoute) { v ->
                            viewModel.update { it.copy(autoFitRoute = v) }
                        }
                        SwitchRow(R.string.setting_start_end, null, prefs.showStartEndMarkers) { v ->
                            viewModel.update { it.copy(showStartEndMarkers = v) }
                        }
                        SwitchRow(R.string.setting_waypoints, null, prefs.showWaypoints) { v ->
                            viewModel.update { it.copy(showWaypoints = v) }
                        }
                        SwitchRow(R.string.setting_compass, null, prefs.showCompass) { v ->
                            viewModel.update { it.copy(showCompass = v) }
                        }
                        SwitchRow(R.string.setting_scale, null, prefs.showScale) { v ->
                            viewModel.update { it.copy(showScale = v) }
                        }
                    }
                }

                // 5. Routes Styling & Import Settings
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Route Styling & Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        SwitchRow(R.string.setting_auto_open, R.string.setting_auto_open_desc, prefs.autoOpenImported) { v ->
                            viewModel.update { it.copy(autoOpenImported = v) }
                        }
                        ValueRow(R.string.setting_line_width, lineWidthLabel(prefs.routeLineWidth)) { dialog = SettingsDialog.LINE_WIDTH }
                        ValueRow(R.string.setting_line_style, lineStyleLabel(prefs.routeLineStyle)) { dialog = SettingsDialog.LINE_STYLE }
                    }
                }

                // 6. Storage & About
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Storage & About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(stringResource(R.string.storage_saved_routes)) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string.storage_summary,
                                        state.routeCount,
                                        Formatter.formatShortFileSize(context, state.importedBytes),
                                    ),
                                )
                            },
                        )
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.app_name)) },
                            supportingContent = { Text(stringResource(R.string.about_version, versionName)) },
                        )
                        IconRow(Icons.Filled.Lock, R.string.about_privacy) { dialog = SettingsDialog.PRIVACY }
                        IconRow(Icons.Filled.Description, R.string.about_licenses) { dialog = SettingsDialog.LICENSES }
                    }
                }
            }
        }
    }

    when (dialog) {
        SettingsDialog.PROVIDER -> ChoiceDialog(
            title = stringResource(R.string.setting_default_provider),
            options = MapProviderId.entries.map { it to providerName(it) },
            selected = prefs.mapProvider,
            onSelect = { p ->
                viewModel.update { it.copy(mapProvider = p, basemapId = LasTrack.duxtami.app.core.model.DEFAULT_BASEMAP_ID) }
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.BASEMAP -> ChoiceDialog(
            title = stringResource(R.string.setting_default_basemap),
            options = state.basemaps.map { it.id to it.name },
            selected = state.selectedBasemap?.id ?: "",
            onSelect = { id -> viewModel.update { it.copy(basemapId = id) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.LINE_WIDTH -> ChoiceDialog(
            title = stringResource(R.string.setting_line_width),
            options = RouteLineWidth.entries.map { it to lineWidthLabel(it) },
            selected = prefs.routeLineWidth,
            onSelect = { w -> viewModel.update { it.copy(routeLineWidth = w) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.LINE_STYLE -> ChoiceDialog(
            title = stringResource(R.string.setting_line_style),
            options = RouteLineStyle.entries.map { it to lineStyleLabel(it) },
            selected = prefs.routeLineStyle,
            onSelect = { s -> viewModel.update { it.copy(routeLineStyle = s) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PRIVACY -> InfoDialog(R.string.about_privacy, R.string.about_privacy_text) { dialog = null }
        SettingsDialog.LICENSES -> InfoDialog(R.string.about_licenses, R.string.about_licenses_text) { dialog = null }
        null -> Unit
    }
}

/**
 * Modern Segmented Theme Selector Card matching Screenshot_20261022-091311.png.
 */
@Composable
private fun ThemeSelectorCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Segmented Pill Buttons: System | Light | Dark
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val activeColor = Color(0xFF9FD7A4)
                    val activeTextColor = Color(0xFF142416)

                    ThemeMode.entries.forEach { mode ->
                        val isSelected = (mode == selected)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) activeColor else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { onSelect(mode) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "System"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeTextColor else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun lineWidthLabel(w: RouteLineWidth): String = stringResource(
    when (w) {
        RouteLineWidth.THIN -> R.string.width_thin
        RouteLineWidth.MEDIUM -> R.string.width_medium
        RouteLineWidth.THICK -> R.string.width_thick
    },
)

@Composable
private fun lineStyleLabel(s: RouteLineStyle): String = stringResource(
    when (s) {
        RouteLineStyle.SOLID -> R.string.style_solid
        RouteLineStyle.DASHED -> R.string.style_dashed
    },
)

@Composable
private fun ValueRow(label: Int, value: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(stringResource(label)) },
        supportingContent = { Text(value) },
    )
}

@Composable
private fun IconRow(icon: ImageVector, label: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(label)) },
    )
}

@Composable
private fun SwitchRow(label: Int, description: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(stringResource(label)) },
        supportingContent = description?.let { { Text(stringResource(it)) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun InfoDialog(title: Int, message: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(message), modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
