@file:OptIn(ExperimentalMaterial3Api::class)

package LasTrack.duxtami.app.ui.routes

import LasTrack.duxtami.app.R
import LasTrack.duxtami.app.core.model.RouteSummary
import LasTrack.duxtami.app.core.model.SourceFormat
import LasTrack.duxtami.app.ui.common.DeleteRouteDialog
import LasTrack.duxtami.app.ui.common.EmptyState
import LasTrack.duxtami.app.ui.common.RenameRouteDialog
import LasTrack.duxtami.app.ui.common.RouteListItem
import LasTrack.duxtami.app.ui.common.RouteOverflowMenu
import LasTrack.duxtami.app.ui.common.UiMessage
import LasTrack.duxtami.app.ui.common.rememberRouteFileActions
import LasTrack.duxtami.app.ui.common.text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RoutesScreen(
    focusSearch: Boolean,
    onImportClick: () -> Unit,
    onOpenRoute: (String) -> Unit,
    viewModel: RoutesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    var renaming by remember { mutableStateOf<RouteSummary?>(null) }
    var deleting by remember { mutableStateOf<RouteSummary?>(null) }

    val fileActions = rememberRouteFileActions(
        prepareShare = viewModel::prepareShare,
        onExportToUri = viewModel::export,
    )

    LaunchedEffect(focusSearch) { if (focusSearch) focusRequester.requestFocus() }
    val messageTexts = UiMessage.entries.associateWith { it.text() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(messageTexts.getValue(it)) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_routes)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                icon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
                text = { Text(stringResource(R.string.import_route)) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(focusRequester),
                )
                FormatChips(selected = state.format, onSelect = viewModel::onFormatChange)

                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    state.totalCount == 0 -> EmptyState(
                        icon = Icons.Filled.Route,
                        title = stringResource(R.string.empty_routes_title),
                        description = stringResource(R.string.empty_routes_message),
                        actionLabel = stringResource(R.string.import_route),
                        onAction = onImportClick,
                    )
                    state.routes.isEmpty() -> EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.no_results_title),
                        description = stringResource(R.string.no_results_message),
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(state.routes, key = { it.id }) { route ->
                            RouteListItem(
                                route = route,
                                onClick = { onOpenRoute(route.id) },
                                trailing = {
                                    RouteOverflowMenu(
                                        onOpen = { onOpenRoute(route.id) },
                                        onRename = { renaming = route },
                                        onShare = { fileActions.share(route) },
                                        onExport = { fileActions.export(route) },
                                        onDelete = { deleting = route },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    renaming?.let { route ->
        RenameRouteDialog(
            initialName = route.name,
            onConfirm = { viewModel.rename(route.id, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { route ->
        DeleteRouteDialog(
            routeName = route.name,
            onConfirm = { viewModel.delete(route.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_routes_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
                }
            }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun FormatChips(selected: SourceFormat?, onSelect: (SourceFormat?) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringResource(R.string.filter_all)) })
        SourceFormat.entries.forEach { format ->
            FilterChip(selected = selected == format, onClick = { onSelect(format) }, label = { Text(format.displayName) })
        }
    }
}
