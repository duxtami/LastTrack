package LastTrack.duxtami.app.navigation

import LastTrack.duxtami.app.R
import LastTrack.duxtami.app.ui.details.RouteDetailsScreen
import LastTrack.duxtami.app.ui.home.HomeScreen
import LastTrack.duxtami.app.ui.importflow.ImportEvent
import LastTrack.duxtami.app.ui.importflow.ImportHost
import LastTrack.duxtami.app.ui.importflow.ImportViewModel
import LastTrack.duxtami.app.ui.maps.MapsScreen
import LastTrack.duxtami.app.ui.routes.RoutesScreen
import LastTrack.duxtami.app.ui.settings.SettingsScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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

object Routes {
    const val HOME = "home"
    const val ROUTES_PATTERN = "routes?search={search}"
    const val ROUTES = "routes"
    const val MAPS_PATTERN = "maps?routeId={routeId}"
    const val MAPS = "maps"
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

private val topLevelDestinations = listOf(
    TopLevel(Routes.HOME, Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(Routes.ROUTES_PATTERN, Routes.ROUTES, R.string.nav_routes, Icons.Outlined.Route, Icons.Filled.Route),
    TopLevel(Routes.MAPS_PATTERN, Routes.MAPS, R.string.nav_maps, Icons.Outlined.Map, Icons.Filled.Map),
    TopLevel(Routes.SETTINGS, Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

private val importMimeTypes = arrayOf(
    "application/gpx+xml",
    "application/vnd.google-earth.kml+xml",
    "application/vnd.google-earth.kmz",
    "application/xml",
    "text/xml",
    "application/zip",
    // Many providers report GPX/KML/KMZ files as generic binary; the real format is verified after picking.
    "application/octet-stream",
)

private fun NavHostController.navigateTopLevel(target: String) {
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun LastTrackApp(importViewModel: ImportViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val importState by importViewModel.state.collectAsStateWithLifecycle()

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
            // Top-level screens consume system insets themselves (top bar) or via the bottom bar. With a rail
            // there is no bottom bar, so the gesture/navigation bar inset is applied here instead.
            val insetModifier = when {
                wide && showNav -> Modifier.navigationBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                wide -> Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                else -> Modifier
            }
            Box(Modifier.fillMaxSize().padding(padding).then(insetModifier)) {
                AppNavHost(navController = navController, onImportClick = launchImport)
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
private fun AppNavHost(navController: NavHostController, onImportClick: () -> Unit) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onImportClick = onImportClick,
                onOpenRoute = { navController.navigate(Routes.details(it)) },
                onOpenRoutes = { navController.navigateTopLevel(Routes.ROUTES) },
                onOpenSearch = { navController.navigate(Routes.routesSearch()) { launchSingleTop = true } },
                onOpenSettings = { navController.navigateTopLevel(Routes.SETTINGS) },
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
            arguments = listOf(navArgument("routeId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { MapsScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
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
