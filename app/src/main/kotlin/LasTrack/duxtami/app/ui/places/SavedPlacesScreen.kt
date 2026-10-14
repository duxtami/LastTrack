@file:OptIn(ExperimentalMaterial3Api::class)

package LasTrack.duxtami.app.ui.places

import LasTrack.duxtami.app.R
import LasTrack.duxtami.app.core.model.CoordinateParser
import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.SavedPlace
import LasTrack.duxtami.app.ui.common.GoogleMapsLauncher
import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedPlacesScreen(
    onOpenOnMap: (lat: Double, lon: Double, name: String) -> Unit,
    sharedPlaceInput: String? = null,
    onConsumeSharedPlace: () -> Unit = {},
    viewModel: SavedPlacesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showActionChoiceSheet by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    var initialCoords by remember { mutableStateOf<String?>(null) }
    var initialName by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchAndSetCurrentLocation(context) { lat, lon ->
                initialCoords = "$lat, $lon"
                initialName = "My Location (${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())})"
                showAddDialog = true
            }
        }
    }

    // Auto open when a link or coordinates are shared via Android Share Sheet
    LaunchedEffect(sharedPlaceInput) {
        if (!sharedPlaceInput.isNullOrBlank()) {
            initialCoords = sharedPlaceInput
            initialName = ""
            showAddDialog = true
            onConsumeSharedPlace()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text(stringResource(R.string.search_places_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    searchActive = false
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                            },
                        )
                    } else {
                        Text(stringResource(R.string.nav_saved_places))
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showActionChoiceSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Place")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text(stringResource(R.string.all_categories)) },
                )
                PlaceCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { viewModel.setCategoryFilter(if (state.selectedCategory == cat) null else cat) },
                        leadingIcon = {
                            Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(cat.displayName) },
                    )
                }
            }

            // Places List
            if (state.filteredPlaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(
                            text = if (state.searchQuery.isNotBlank() || state.selectedCategory != null) {
                                stringResource(R.string.no_matching_places)
                            } else {
                                stringResource(R.string.no_saved_places)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.add_coordinates_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 380.dp),
                        )
                        Button(
                            onClick = { showActionChoiceSheet = true },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.add_place_button))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.filteredPlaces, key = { it.id }) { place ->
                        SavedPlaceCard(
                            place = place,
                            onOpenOnMap = { onOpenOnMap(place.latitude, place.longitude, place.name) },
                            onOpenGoogleMaps = {
                                GoogleMapsLauncher.openCoordinates(context, place.latitude, place.longitude, place.name)
                            },
                            onDelete = { viewModel.deletePlace(place.id) },
                        )
                    }
                }
            }
        }
    }

    // --- Choice Sheet: Add Current Location vs Add Custom Place / Link ---
    if (showActionChoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionChoiceSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Add to Places",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // 1. Add Current Location Action
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showActionChoiceSheet = false
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                fetchAndSetCurrentLocation(context) { lat, lon ->
                                    initialCoords = "$lat, $lon"
                                    initialName = "Current Location (${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())})"
                                    showAddDialog = true
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        },
                ) {
                    ListItem(
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = { Text("Add Current Location", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Instantly save your current GPS location with category & note") },
                    )
                }

                // 2. Add Custom Place / Link Action
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showActionChoiceSheet = false
                            initialCoords = null
                            initialName = null
                            showAddDialog = true
                        },
                ) {
                    ListItem(
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(Icons.Filled.EditLocationAlt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        headlineContent = { Text("Add Coordinates / Google Maps Link", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Paste Google Maps URL, DMS, or lat/lon coordinates") },
                    )
                }
            }
        }
    }

    // --- Add Place Form Dialog ---
    if (showAddDialog) {
        AddPlaceBottomSheet(
            initialCoords = initialCoords,
            initialName = initialName,
            onDismiss = {
                showAddDialog = false
                initialCoords = null
                initialName = null
            },
            onRequestLocation = { onLocationReady ->
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) {
                    fetchAndSetCurrentLocation(context, onLocationReady)
                } else {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            },
            onSave = { name, coords, category, desc, imgUri ->
                scope.launch {
                    val ok = viewModel.addPlaceAsync(name, coords, category, desc, imgUri)
                    if (ok) {
                        Toast.makeText(context, R.string.place_saved_success, Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                        initialCoords = null
                        initialName = null
                    } else {
                        Toast.makeText(context, R.string.invalid_coordinates_error, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }
}

@SuppressLint("MissingPermission")
private fun fetchAndSetCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    try {
        val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val best = lastGps ?: lastNet
        if (best != null) {
            onLocation(best.latitude, best.longitude)
        } else {
            Toast.makeText(context, "Acquiring GPS fix… please try again in a moment", Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
    }
}

@Composable
fun categoryIcon(category: PlaceCategory): ImageVector = when (category) {
    PlaceCategory.PARKING -> Icons.Filled.DirectionsBike
    PlaceCategory.FAVORITE -> Icons.Filled.Star
    PlaceCategory.WORK -> Icons.Filled.Work
    PlaceCategory.HOME -> Icons.Filled.Home
    PlaceCategory.SCENIC -> Icons.Filled.Park
    PlaceCategory.FOOD -> Icons.Filled.Restaurant
    PlaceCategory.FUEL -> Icons.Filled.LocalGasStation
    PlaceCategory.CAMPING -> Icons.Filled.Park
    PlaceCategory.CUSTOM -> Icons.Filled.Place
}

@Composable
fun SavedPlaceCard(
    place: SavedPlace,
    onOpenOnMap: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val coordsText = String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude)

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category Icon + Title + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                categoryIcon(place.category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${place.category.displayName} · ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(place.createdAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Attached Photo thumbnail (if any)
            place.imagePath?.let { path ->
                val bitmap = remember(path) {
                    runCatching {
                        val file = File(path)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }.getOrNull()
                }
                bitmap?.let { b ->
                    Spacer(Modifier.height(10.dp))
                    Image(
                        bitmap = b.asImageBitmap(),
                        contentDescription = "Place Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
            }

            // Coordinates Row with Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.NearMe,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = coordsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", coordsText))
                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                }
            }

            // Description / Note if available
            val desc = place.description
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenOnMap,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.view_on_map), style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onOpenGoogleMaps,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.google_maps_short), style = MaterialTheme.typography.labelMedium)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AddPlaceBottomSheet(
    initialCoords: String? = null,
    initialName: String? = null,
    onDismiss: () -> Unit,
    onRequestLocation: ((latitude: Double, longitude: Double) -> Unit) -> Unit,
    onSave: (name: String, coords: String, category: PlaceCategory, desc: String?, imgUri: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName.orEmpty()) }
    var coordinatesInput by remember { mutableStateOf(initialCoords.orEmpty()) }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.PARKING) }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var isResolvingUrl by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    // If initialCoords has a Google Maps link or short URL, resolve it asynchronously
    LaunchedEffect(initialCoords) {
        if (!initialCoords.isNullOrBlank()) {
            isResolvingUrl = true
            scope.launch {
                val parsed = CoordinateParser.resolveAndParse(initialCoords)
                if (parsed != null) {
                    coordinatesInput = "${parsed.latitude}, ${parsed.longitude}"
                    if (name.isBlank() && !parsed.suggestedName.isNullOrBlank()) {
                        name = parsed.suggestedName.orEmpty()
                    }
                }
                isResolvingUrl = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.add_saved_place_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Quick Action: Fill Current GPS Location
            FilledTonalButton(
                onClick = {
                    onRequestLocation { lat, lon ->
                        coordinatesInput = "$lat, $lon"
                        if (name.isBlank()) {
                            name = "Current Location (${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())})"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Use Current GPS Location")
            }

            // Place Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.place_name_label)) },
                placeholder = { Text("e.g. Connaught Place, Hotel, My Bike") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Coordinates input with Paste & MyLocation button
            OutlinedTextField(
                value = coordinatesInput,
                onValueChange = { coordinatesInput = it },
                label = {
                    Text(
                        if (isResolvingUrl) "Resolving Google Maps link…" else stringResource(R.string.coordinates_or_link_label)
                    )
                },
                placeholder = { Text("28.6139, 77.2090 or Google Maps URL") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrBlank()) {
                                coordinatesInput = clip
                                scope.launch {
                                    val parsed = CoordinateParser.resolveAndParse(clip)
                                    if (parsed != null) {
                                        coordinatesInput = "${parsed.latitude}, ${parsed.longitude}"
                                        if (name.isBlank() && !parsed.suggestedName.isNullOrBlank()) {
                                            name = parsed.suggestedName.orEmpty()
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Category Selector
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        leadingIcon = {
                            Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(cat.displayName) },
                    )
                }
            }

            // Description / Note
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_reminder_label)) },
                placeholder = { Text("e.g. Parked near pillar B4, floor 2") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            // Image import attachment
            Text(
                text = stringResource(R.string.photo_attachment_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (selectedImageUri != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.photo_attached), style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { selectedImageUri = null }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Remove photo")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.import_photo_button))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    onSave(name, coordinatesInput, selectedCategory, description, selectedImageUri)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = coordinatesInput.isNotBlank(),
            ) {
                Text(stringResource(R.string.save_location_button))
            }
        }
    }
}
