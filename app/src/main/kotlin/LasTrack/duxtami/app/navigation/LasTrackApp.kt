@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package LasTrack.duxtami.app.navigation

import LasTrack.duxtami.app.R
import LasTrack.duxtami.app.core.model.CoordinateParser
import LasTrack.duxtami.app.ui.details.RouteDetailsScreen
import LasTrack.duxtami.app.ui.home.HomeScreen
import LasTrack.duxtami.app.ui.importflow.ImportEvent
import LasTrack.duxtami.app.ui.importflow.ImportHost
import LasTrack.duxtami.app.ui.importflow.ImportViewModel
import LasTrack.duxtami.app.ui.maps.MapsScreen
import LasTrack.duxtami.app.ui.places.SavedPlacesScreen
import LasTrack.duxtami.app.ui.routes.RoutesScreen
import LasTrack.duxtami.app.ui.settings.SettingsScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val ROUTES_PATTERN = "routes?search={search}"
    const val ROUTES = "routes"
    const val MAPS_PATTERN = "maps?routeId={routeId}&lat={lat}&lon={lon}&title={title}"
    const val MAPS = "maps"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val DETAILS_PATTERN = "details/{routeId}"

    fun details(id: String) = "details/$id"
    fun mapsFor(id: String) = "maps?routeId=$id"
    fun routesSearch() = "routes?search=true"
}

private data class TopLevel(
    val pattern: String,
    val target: String,
    val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

// Exactly 3 bottom navigation tabs: Home, Maps, and Saved Places
private val topLevelDestinations = listOf(
    TopLevel(Routes.HOME, Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(Routes.MAPS_PATTERN, Routes.MAPS, R.string.nav_maps, Icons.Outlined.Map, Icons.Filled.Map),
    TopLevel(Routes.SAVED, Routes.SAVED, R.string.nav_saved_places, Icons.Outlined.Place, Icons.Filled.Place),
)

private val importMimeTypes = arrayOf(
    "application/gpx+xml",
    "application/vnd.google-earth.kml+xml",
    "application/vnd.google-earth.kmz",
    "application/xml",
    "text/xml",
    "application/zip",
    "application/octet-stream",
)

private fun NavHostController.navigateTopLevel(target: String) {
    if (currentDestination?.route == target) return
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}

@Composable
fun LasTrackApp(
    sharedPlaceInput: String? = null,
    onConsumeSharedPlace: () -> Unit = {},
    sharedFileUri: String? = null,
    onConsumeSharedFile: () -> Unit = {},
    importViewModel: ImportViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var pendingSharedLocation by remember { mutableStateOf<String?>(null) }
    var activePlaceForSavedTab by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sharedPlaceInput) {
        if (!sharedPlaceInput.isNullOrBlank()) {
            pendingSharedLocation = sharedPlaceInput
        }
    }

    LaunchedEffect(sharedFileUri) {
        if (!sharedFileUri.isNullOrBlank()) {
            importViewModel.import(sharedFileUri)
            onConsumeSharedFile()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importViewModel.import(uri.toString())
    }
    val launchImport = remember(picker) { { picker.launch(importMimeTypes) } }

    val importedMessage = stringResource(R.string.route_imported)
    val openLabel = stringResource(R.string.action_open)
    LaunchedEffect(importViewModel) {
        importViewModel.events.collect { event ->
            when (event) {
                is ImportEvent.OpenRoute -> navController.navigate(Routes.details(event.routeId))
                is ImportEvent.Imported -> {
                    val result = snackbar.showSnackbar(importedMessage, actionLabel = openLabel)
                    if (result == SnackbarResult.ActionPerformed) navController.navigate(Routes.details(event.routeId))
                }
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showNav = destination?.hierarchy?.any { d -> topLevelDestinations.any { it.pattern == d.route } } == true
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Row(Modifier.fillMaxSize()) {
        if (wide && showNav) {
            NavigationRail {
                topLevelDestinations.forEach { item ->
                    val selected = destination?.hierarchy?.any { it.route == item.pattern } == true
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navController.navigateTopLevel(item.target) },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                        label = { androidx.compose.material3.Text(stringResource(item.label)) },
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!wide && showNav) {
                    NavigationBar {
                        topLevelDestinations.forEach { item ->
                            val selected = destination?.hierarchy?.any { it.route == item.pattern } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateTopLevel(item.target) },
                                icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                                label = { androidx.compose.material3.Text(stringResource(item.label)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            val insetModifier = when {
                wide && showNav -> Modifier.navigationBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                wide -> Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                else -> Modifier
            }
            Box(Modifier.fillMaxSize().padding(padding).then(insetModifier)) {
                AppNavHost(
                    navController = navController,
                    onImportClick = launchImport,
                    sharedPlaceInput = activePlaceForSavedTab,
                    onConsumeSharedPlace = { activePlaceForSavedTab = null },
                )
            }
        }
    }

    // Modal Choice Sheet when a Google Maps link or location is shared
    pendingSharedLocation?.let { input ->
        ModalBottomSheet(
            onDismissRequest = {
                pendingSharedLocation = null
                onConsumeSharedPlace()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Shared Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = input,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.size(4.dp))

                // 1. Save to Places
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activePlaceForSavedTab = input
                            pendingSharedLocation = null
                            onConsumeSharedPlace()
                            navController.navigateTopLevel(Routes.SAVED)
                        },
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = { Text("Save to Places", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Save with custom name, M3 icon, category & notes") },
                    )
                }

                // 2. View on Map
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val raw = input
                            pendingSharedLocation = null
                            onConsumeSharedPlace()
                            scope.launch {
                                val parsed = CoordinateParser.resolveAndParse(raw)
                                if (parsed != null) {
                                    val encTitle = URLEncoder.encode(parsed.suggestedName ?: "Shared Location", "UTF-8")
                                    navController.navigate("maps?lat=${parsed.latitude}&lon=${parsed.longitude}&title=$encTitle") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigateTopLevel(Routes.MAPS)
                                }
                            }
                        },
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        headlineContent = { Text("View / Inspect on Map", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Pinpoint coordinates directly on the active map") },
                    )
                }
            }
        }
    }

    ImportHost(
        state = importState,
        onCancel = importViewModel::dismiss,
        onRetry = {
            importViewModel.dismiss()
            launchImport()
        },
    )
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    onImportClick: () -> Unit,
    sharedPlaceInput: String? = null,
    onConsumeSharedPlace: () -> Unit = {},
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onImportClick = onImportClick,
                onOpenRoute = { navController.navigate(Routes.details(it)) },
                onOpenRoutes = { navController.navigate(Routes.ROUTES) },
                onOpenSearch = { navController.navigate(Routes.routesSearch()) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.ROUTES_PATTERN,
            arguments = listOf(navArgument("search") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            RoutesScreen(
                focusSearch = entry.arguments?.getBoolean("search") ?: false,
                onImportClick = onImportClick,
                onOpenRoute = { navController.navigate(Routes.details(it)) },
            )
        }
        composable(
            Routes.MAPS_PATTERN,
            arguments = listOf(
                navArgument("routeId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val lat = entry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = entry.arguments?.getString("lon")?.toDoubleOrNull()
            val title = entry.arguments?.getString("title")?.let {
                runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
            }
            MapsScreen(targetLat = lat, targetLon = lon, targetTitle = title)
        }
        composable(Routes.SAVED) {
            SavedPlacesScreen(
                onOpenOnMap = { lat, lon, name ->
                    val encodedTitle = runCatching { java.net.URLEncoder.encode(name, "UTF-8") }.getOrDefault(name)
                    navController.navigate("maps?lat=$lat&lon=$lon&title=$encodedTitle") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                sharedPlaceInput = sharedPlaceInput,
                onConsumeSharedPlace = onConsumeSharedPlace,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.DETAILS_PATTERN,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
        ) {
            RouteDetailsScreen(
                onBack = { navController.popBackStack() },
                onOpenOnMap = { navController.navigate(Routes.mapsFor(it)) },
            )
        }
    }
}
