package LastTrack.duxtami.app.ui.importflow

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.ui.common.Formatters
import LastTrack.duxtami.app.ui.common.importErrorText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun ImportHost(
    state: ImportUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        ImportUiState.Idle -> Unit
        is ImportUiState.InProgress -> ImportProgressDialog(state, onCancel)
        is ImportUiState.Failed -> AlertDialog(
            onDismissRequest = onCancel,
            icon = { Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.import_failed_title)) },
            text = { Text(importErrorText(state.error)) },
            confirmButton = { TextButton(onClick = onRetry) { Text(stringResource(R.string.try_again)) } },
            dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

@Composable
private fun ImportProgressDialog(state: ImportUiState.InProgress, onCancel: () -> Unit) {
    val stages = listOf(
        ImportStage.READING to R.string.stage_reading,
        ImportStage.PARSING to R.string.stage_parsing,
        ImportStage.PROCESSING to R.string.stage_processing,
        ImportStage.STATISTICS to R.string.stage_statistics,
        ImportStage.PREPARING to R.string.stage_preparing,
    )
    AlertDialog(
        onDismissRequest = {}, // not dismissable by tapping outside; use Cancel
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.importing_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { (state.stage.ordinal + 1) / stages.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                stages.forEach { (stage, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            stage.ordinal < state.stage.ordinal ->
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            stage == state.stage ->
                                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                            else ->
                                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (stage.ordinal > state.stage.ordinal) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (state.stage == ImportStage.PARSING && state.pointsParsed > 0) {
                    Text(
                        stringResource(R.string.points_read, Formatters.count(state.pointsParsed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}
