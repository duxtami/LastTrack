@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package LasTrack.duxtami.app.ui.places

import LasTrack.duxtami.app.R
import LasTrack.duxtami.app.core.model.CoordinateParser
import LasTrack.duxtami.app.core.model.GeoPoint
import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.SavedPlace
import LasTrack.duxtami.app.core.parser.GeoMath
import LasTrack.duxtami.app.ui.common.GoogleMapsLauncher
import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    var editingPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var viewingPhoto by remember { mutableStateOf<Pair<String, String>?>(null) } // path to title
    var showActionChoiceSheet by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var actionPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var placeToDelete by remember { mutableStateOf<SavedPlace?>(null) }

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
            // Category & Custom Label Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedFilter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text(stringResource(R.string.all_categories)) },
                )
                PlaceCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = state.selectedFilter == cat.name || state.selectedFilter == cat.displayName,
                        onClick = {
                            val active = state.selectedFilter == cat.name || state.selectedFilter == cat.displayName
                            viewModel.setFilter(if (active) null else cat.name)
                        },
                        leadingIcon = {
                            Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(cat.displayName) },
                    )
                }
                state.availableCustomLabels.forEach { customLabel ->
                    FilterChip(
                        selected = state.selectedFilter.equals(customLabel, ignoreCase = true),
                        onClick = {
                            val active = state.selectedFilter.equals(customLabel, ignoreCase = true)
                            viewModel.setFilter(if (active) null else customLabel)
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(customLabel) },
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
                            text = if (state.searchQuery.isNotBlank() || state.selectedFilter != null) {
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
                            currentLocation = state.currentLocation,
                            onOpenOnMap = { onOpenOnMap(place.latitude, place.longitude, place.name) },
                            onOpenGoogleMaps = {
                                GoogleMapsLauncher.openCoordinates(context, place.latitude, place.longitude, place.name)
                            },
                            onOpenActions = { actionPlace = place },
                            onViewImage = { path -> viewingPhoto = path to place.name },
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
        PlaceFormBottomSheet(
            title = stringResource(R.string.add_saved_place_title),
            submitLabel = stringResource(R.string.save_location_button),
            initialCoords = initialCoords,
            initialName = initialName,
            initialCategory = PlaceCategory.PARKING,
            initialCustomLabel = null,
            initialDesc = "",
            initialImageUri = null,
            initialIconKey = null,
            availableCustomLabels = state.availableCustomLabels,
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
            onAddCustomLabel = viewModel::addCustomLabel,
            onSubmit = { name, coords, category, customLabel, desc, imgUri, _, iconKey ->
                scope.launch {
                    val ok = viewModel.addPlaceAsync(name, coords, category, customLabel, desc, imgUri, iconKey)
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

    // --- Edit Place Form Dialog ---
    editingPlace?.let { place ->
        PlaceFormBottomSheet(
            title = "Edit Place",
            submitLabel = "Save Changes",
            initialCoords = "${place.latitude}, ${place.longitude}",
            initialName = place.name,
            initialCategory = place.category,
            initialCustomLabel = place.customLabel,
            initialDesc = place.description.orEmpty(),
            initialImageUri = place.imagePath,
            initialIconKey = place.iconKey,
            availableCustomLabels = state.availableCustomLabels,
            onDismiss = { editingPlace = null },
            onRequestLocation = { onLocationReady ->
                fetchAndSetCurrentLocation(context, onLocationReady)
            },
            onAddCustomLabel = viewModel::addCustomLabel,
            onSubmit = { name, coords, category, customLabel, desc, imgUri, removeImg, iconKey ->
                scope.launch {
                    val ok = viewModel.updatePlaceAsync(
                        id = place.id,
                        name = name,
                        rawCoordinates = coords,
                        category = category,
                        customLabel = customLabel,
                        description = desc,
                        imageUri = imgUri,
                        removeImage = removeImg,
                        iconKey = iconKey,
                    )
                    if (ok) {
                        Toast.makeText(context, "Place updated successfully", Toast.LENGTH_SHORT).show()
                        editingPlace = null
                    } else {
                        Toast.makeText(context, R.string.invalid_coordinates_error, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }

    // --- Action Sheet on Long Press (Edit, Delete, Copy, Share) ---
    actionPlace?.let { place ->
        PlaceActionBottomSheet(
            place = place,
            onDismiss = { actionPlace = null },
            onEdit = {
                editingPlace = place
                actionPlace = null
            },
            onDelete = {
                placeToDelete = place
                actionPlace = null
            },
            onCopyCoordinates = {
                val coordsText = String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", coordsText))
                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val coordsText = String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude)
                val shareText = "${place.name}\n$coordsText\nhttps://maps.google.com/?q=${place.latitude},${place.longitude}"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Location"))
            },
        )
    }

    // --- Confirm Delete Dialog ---
    placeToDelete?.let { place ->
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text("Delete Place") },
            text = { Text("Are you sure you want to delete \"${place.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlace(place.id)
                        placeToDelete = null
                        Toast.makeText(context, "Place deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { placeToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // --- Full Image Viewer Dialog ---
    viewingPhoto?.let { (imagePath, title) ->
        PhotoViewerDialog(
            imagePath = imagePath,
            title = title,
            onDismiss = { viewingPhoto = null },
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
    currentLocation: GeoPoint?,
    onOpenOnMap: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onOpenActions: () -> Unit,
    onViewImage: (path: String) -> Unit,
) {
    val context = LocalContext.current
    val coordsText = String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude)

    val distanceMeters = remember(place.latitude, place.longitude, currentLocation) {
        currentLocation?.let { cur ->
            GeoMath.haversineMeters(cur.latitude, cur.longitude, place.latitude, place.longitude)
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenOnMap,
                onLongClick = onOpenActions,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category Icon + Title + Date + Live Distance Badge
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
                                PlaceIcons.resolve(place.iconKey, place.category),
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
                            text = "${place.displayCategory} · ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(place.createdAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Live meter / kilometer distance badge
                if (distanceMeters != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Filled.NearMe,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            val distFormatted = if (distanceMeters < 1000) {
                                "${distanceMeters.roundToInt()} m"
                            } else {
                                String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
                            }
                            Text(
                                text = distFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Attached Photo thumbnail (clickable to view full screen!)
            place.imagePath?.let { path ->
                val bitmap = remember(path) {
                    runCatching {
                        val file = File(path)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }.getOrNull()
                }
                bitmap?.let { b ->
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onViewImage(path) },
                    ) {
                        Image(
                            bitmap = b.asImageBitmap(),
                            contentDescription = "Place Photo (Tap to view full screen)",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.ZoomIn,
                                    contentDescription = "View photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
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

            Spacer(Modifier.height(12.dp))

            // Action Buttons: View on Map, Google Maps (Edit and Delete moved to long-press sheet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onOpenOnMap,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.view_on_map), style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onOpenGoogleMaps,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.google_maps_short), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Bottom sheet displayed when user long-presses a saved place card.
 * Provides actions to Edit, Copy coordinates, Share, or Delete.
 */
@Composable
fun PlaceActionBottomSheet(
    place: SavedPlace,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyCoordinates: () -> Unit,
    onShare: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            PlaceIcons.resolve(place.iconKey, place.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        String.format(Locale.US, "%.5f, %.5f · %s", place.latitude, place.longitude, place.displayCategory),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onEdit()
                    },
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Edit Place", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Modify name, coordinates, category, icon, or note") },
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onCopyCoordinates()
                    },
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    headlineContent = { Text("Copy Coordinates", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(String.format(Locale.US, "%.6f, %.6f", place.latitude, place.longitude)) },
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onShare()
                    },
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                    headlineContent = { Text("Share Location", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Share coordinates & Google Maps link to other apps") },
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onDelete()
                    },
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    headlineContent = { Text("Delete Place", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Remove this place permanently from saved list") },
                )
            }
        }
    }
}

/**
 * Dialog to view an attached place image full-screen with high clarity.
 */
@Composable
fun PhotoViewerDialog(
    imagePath: String,
    title: String,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(imagePath) {
        runCatching {
            val file = File(imagePath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 80.dp),
                )
            } else {
                Text("Image could not be loaded", color = Color.White)
            }
        }
    }
}

/**
 * Universal Place Form Sheet (Used for both Adding new place and Editing existing place).
 */
@Composable
fun PlaceFormBottomSheet(
    title: String,
    submitLabel: String,
    initialCoords: String? = null,
    initialName: String? = null,
    initialCategory: PlaceCategory = PlaceCategory.PARKING,
    initialCustomLabel: String? = null,
    initialDesc: String = "",
    initialImageUri: String? = null,
    initialIconKey: String? = null,
    availableCustomLabels: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onRequestLocation: ((latitude: Double, longitude: Double) -> Unit) -> Unit,
    onAddCustomLabel: (String) -> Unit,
    onSubmit: (
        name: String,
        coords: String,
        category: PlaceCategory,
        customLabel: String?,
        desc: String?,
        imgUri: String?,
        removeImage: Boolean,
        iconKey: String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName.orEmpty()) }
    var coordinatesInput by remember { mutableStateOf(initialCoords.orEmpty()) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var customLabelInput by remember { mutableStateOf(initialCustomLabel.orEmpty()) }
    var isCustomSelected by remember { mutableStateOf(!initialCustomLabel.isNullOrBlank()) }
    var selectedIconKey by remember { mutableStateOf(initialIconKey) }
    var description by remember { mutableStateOf(initialDesc) }
    var selectedImageUri by remember { mutableStateOf(initialImageUri) }
    var removeExistingImage by remember { mutableStateOf(false) }
    var isResolvingUrl by remember { mutableStateOf(false) }

    // Dialog state for creating a new custom label
    var showCreateLabelDialog by remember { mutableStateOf(false) }
    var newLabelInput by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            removeExistingImage = false
        }
    }

    // Resolve URL asynchronously if needed
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
                text = title,
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

            // Place Name (Custom Name)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.place_name_label)) },
                placeholder = { Text("e.g. Connaught Place, Hotel, My Bike") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Coordinates input with Paste button
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

            // Category & Custom Label Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Category & Label",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { showCreateLabelDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("+ New Label")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Standard categories
                PlaceCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = !isCustomSelected && selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            isCustomSelected = false
                            customLabelInput = ""
                        },
                        leadingIcon = {
                            Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(cat.displayName) },
                    )
                }

                // Custom labels created by user
                availableCustomLabels.forEach { customLbl ->
                    FilterChip(
                        selected = isCustomSelected && customLabelInput.equals(customLbl, ignoreCase = true),
                        onClick = {
                            selectedCategory = PlaceCategory.CUSTOM
                            isCustomSelected = true
                            customLabelInput = customLbl
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(customLbl) },
                    )
                }
            }

            // Modern Material 3 Expressive Icon Selector
            Text(
                text = "Map Icon Style (Material 3)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceIcons.allIcons.forEach { item ->
                    val isSelected = selectedIconKey == item.key
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { selectedIconKey = if (isSelected) null else item.key },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
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
            if (selectedImageUri != null && !removeExistingImage) {
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
                    Row {
                        TextButton(onClick = { imagePicker.launch("image/*") }) {
                            Text("Change")
                        }
                        IconButton(onClick = {
                            selectedImageUri = null
                            removeExistingImage = true
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Remove photo")
                        }
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

            // Submit Button
            Button(
                onClick = {
                    val finalCustomLabel = if (isCustomSelected) customLabelInput.trim().ifEmpty { null } else null
                    onSubmit(
                        name,
                        coordinatesInput,
                        selectedCategory,
                        finalCustomLabel,
                        description,
                        selectedImageUri,
                        removeExistingImage,
                        selectedIconKey,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = coordinatesInput.isNotBlank(),
            ) {
                Text(submitLabel)
            }
        }
    }

    // Modal prompt to create new custom label
    if (showCreateLabelDialog) {
        AlertDialog(
            onDismissRequest = { showCreateLabelDialog = false },
            title = { Text("Create Custom Label") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a unique label for organizing your places (e.g. Gym, Viewpoint, Secret Spot):")
                    OutlinedTextField(
                        value = newLabelInput,
                        onValueChange = { newLabelInput = it },
                        label = { Text("Label Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newLabelInput.trim()
                        if (trimmed.isNotBlank()) {
                            onAddCustomLabel(trimmed)
                            selectedCategory = PlaceCategory.CUSTOM
                            isCustomSelected = true
                            customLabelInput = trimmed
                            showCreateLabelDialog = false
                            newLabelInput = ""
                        }
                    },
                    enabled = newLabelInput.isNotBlank(),
                ) {
                    Text("Add Label")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateLabelDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
