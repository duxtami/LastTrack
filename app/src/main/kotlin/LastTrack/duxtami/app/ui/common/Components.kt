@file:OptIn(ExperimentalMaterial3Api::class)

package LastTrack.duxtami.app.ui.common

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.core.model.ImportError
import LastTrack.duxtami.app.core.model.RouteSummary
import LastTrack.duxtami.app.core.model.ShareRequest
import LastTrack.duxtami.app.core.model.SourceFormat
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun FormatBadge(format: SourceFormat, isRecorded: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isRecorded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isRecorded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (isRecorded) "REC" else format.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }
    }
}

@Composable
fun RouteListItem(
    route: RouteSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val points = stringResource(R.string.points_count, Formatters.count(route.stats.pointCount))
    val isRecorded = route.name.startsWith("Track ") || route.fileName.startsWith("Track ") || route.fileName.startsWith("rec_")
    ListItem(
        modifier = modifier.selectable(selected = false, role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { FormatBadge(route.format, isRecorded = isRecorded) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(route.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isRecorded) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ) {
                        Text(
                            "Recorded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                "${Formatters.distance(route.stats.distanceMeters)} · $points · ${Formatters.date(route.importedAt)}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = trailing,
    )
}

/** Overflow menu with the per-route actions required by the Routes screen. */
@Composable
fun RouteOverflowMenu(
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_route_actions))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuEntry(R.string.action_open, Icons.Filled.FileOpen) { expanded = false; onOpen() }
            MenuEntry(R.string.action_rename, Icons.Filled.Edit) { expanded = false; onRename() }
            MenuEntry(R.string.action_share, Icons.Filled.Share) { expanded = false; onShare() }
            MenuEntry(R.string.action_export, Icons.Filled.Download) { expanded = false; onExport() }
            MenuEntry(R.string.action_delete, Icons.Filled.Delete) { expanded = false; onDelete() }
        }
    }
}

@Composable
private fun MenuEntry(label: Int, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
fun RenameRouteDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_route_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.route_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun DeleteRouteDialog(routeName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_route_title)) },
        text = { Text(stringResource(R.string.delete_route_message, routeName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Radio-button list dialog used for theme, provider, basemap and line settings. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value); onDismiss() })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun importErrorText(error: ImportError): String = stringResource(
    when (error) {
        ImportError.UNREADABLE_FILE -> R.string.error_unreadable
        ImportError.UNSUPPORTED_FORMAT -> R.string.error_unsupported
        ImportError.EMPTY_FILE -> R.string.error_empty
        ImportError.MALFORMED_FILE -> R.string.error_malformed
        ImportError.NO_COORDINATES -> R.string.error_no_coordinates
        ImportError.INVALID_ARCHIVE -> R.string.error_invalid_archive
        ImportError.NO_KML_IN_ARCHIVE -> R.string.error_no_kml
        ImportError.FILE_TOO_LARGE -> R.string.error_too_large
        ImportError.STORAGE_FAILURE -> R.string.error_storage
        ImportError.UNKNOWN -> R.string.error_unknown
    },
)

@Composable
fun UiMessage.text(): String = stringResource(
    when (this) {
        UiMessage.RENAMED -> R.string.msg_renamed
        UiMessage.DELETED -> R.string.msg_deleted
        UiMessage.EXPORTED -> R.string.msg_exported
        UiMessage.EXPORT_FAILED -> R.string.msg_export_failed
        UiMessage.SHARE_FAILED -> R.string.msg_share_failed
        UiMessage.ACTION_FAILED -> R.string.msg_action_failed
    },
)

/** Handle for the two actions that need Android activity results / intents. */
class RouteFileActions(val share: (RouteSummary) -> Unit, val export: (RouteSummary) -> Unit)

/**
 * Wires the share sheet and the system "save file" picker. Business logic stays in the ViewModel: this only
 * launches intents with data the ViewModel prepared.
 */
@Composable
fun rememberRouteFileActions(
    prepareShare: suspend (routeId: String) -> ShareRequest?,
    onExportToUri: (routeId: String, uri: String) -> Unit,
): RouteFileActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<RouteSummary?>(null) }

    fun onPicked(uri: Uri?) {
        val route = pending
        pending = null
        if (uri != null && route != null) onExportToUri(route.id, uri.toString())
    }
    val gpx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SourceFormat.GPX.mimeType)) { onPicked(it) }
    val kml = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SourceFormat.KML.mimeType)) { onPicked(it) }
    val kmz = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SourceFormat.KMZ.mimeType)) { onPicked(it) }

    return remember(prepareShare, onExportToUri) {
        RouteFileActions(
            share = { route ->
                scope.launch {
                    val request = prepareShare(route.id) ?: return@launch
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = request.mimeType
                        val uri = Uri.parse(request.uri)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TITLE, request.displayName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = android.content.ClipData.newRawUri(request.displayName, uri)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
            export = { route ->
                pending = route
                val name = route.name.replace(Regex("[\\\\/:*?\"<>|]"), "_") + "." + route.format.extension
                when (route.format) {
                    SourceFormat.GPX -> gpx.launch(name)
                    SourceFormat.KML -> kml.launch(name)
                    SourceFormat.KMZ -> kmz.launch(name)
                }
            },
        )
    }
}
