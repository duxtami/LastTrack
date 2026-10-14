@file:OptIn(ExperimentalMaterial3Api::class)

package LastTrack.duxtami.app.ui.settings

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.core.model.AppPreferences
import LastTrack.duxtami.app.core.model.MapProviderId
import LastTrack.duxtami.app.core.model.RouteLineStyle
import LastTrack.duxtami.app.core.model.RouteLineWidth
import LastTrack.duxtami.app.core.model.ThemeMode
import LastTrack.duxtami.app.ui.common.ChoiceDialog
import LastTrack.duxtami.app.ui.maps.providerName
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class SettingsDialog { THEME, PROVIDER, BASEMAP, LINE_WIDTH, LINE_STYLE, PRIVACY, LICENSES }

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Section(R.string.settings_appearance) {
                    ValueRow(R.string.setting_theme, themeLabel(prefs.themeMode)) { dialog = SettingsDialog.THEME }
                    SwitchRow(R.string.setting_dynamic_color, R.string.setting_dynamic_color_desc, prefs.dynamicColor) { v ->
                        viewModel.update { it.copy(dynamicColor = v) }
                    }
                    SwitchRow(R.string.setting_amoled, R.string.setting_amoled_desc, prefs.amoled) { v ->
                        viewModel.update { it.copy(amoled = v) }
                    }
                }
                Section(R.string.settings_maps) {
                    ValueRow(R.string.setting_default_provider, providerName(prefs.mapProvider)) { dialog = SettingsDialog.PROVIDER }
                    ValueRow(R.string.setting_default_basemap, state.selectedBasemap?.name ?: "-") { dialog = SettingsDialog.BASEMAP }
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
                Section(R.string.settings_routes) {
                    SwitchRow(R.string.setting_auto_open, R.string.setting_auto_open_desc, prefs.autoOpenImported) { v ->
                        viewModel.update { it.copy(autoOpenImported = v) }
                    }
                    ValueRow(R.string.setting_line_width, lineWidthLabel(prefs.routeLineWidth)) { dialog = SettingsDialog.LINE_WIDTH }
                    ValueRow(R.string.setting_line_style, lineStyleLabel(prefs.routeLineStyle)) { dialog = SettingsDialog.LINE_STYLE }
                }
                Section(R.string.settings_storage) {
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
                        headlineContent = { Text(stringResource(R.string.storage_local_only)) },
                        supportingContent = { Text(stringResource(R.string.storage_local_only_desc)) },
                    )
                }
                Section(R.string.settings_about) {
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

    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = prefs.themeMode,
            onSelect = { m -> viewModel.update { it.copy(themeMode = m) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PROVIDER -> ChoiceDialog(
            title = stringResource(R.string.setting_default_provider),
            options = MapProviderId.entries.map { it to providerName(it) },
            selected = prefs.mapProvider,
            onSelect = { p ->
                viewModel.update { it.copy(mapProvider = p, basemapId = LastTrack.duxtami.app.core.model.DEFAULT_BASEMAP_ID) }
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

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

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
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
        )
        content()
        HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

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
private fun IconRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, onClick: () -> Unit) {
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
