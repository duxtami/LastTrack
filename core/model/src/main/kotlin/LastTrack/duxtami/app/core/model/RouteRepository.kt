package LastTrack.duxtami.app.core.model

import kotlinx.coroutines.flow.Flow

/** A file prepared for the Android share sheet. [uri] is a content:// URI string. */
data class ShareRequest(
    val uri: String,
    val mimeType: String,
    val displayName: String,
)

class RouteNotFoundException(val routeId: String) : Exception("Route not found")

/**
 * Domain contract for route storage. Implemented in `:core:data`; Android-free so it can be used and
 * faked in plain JVM tests. URIs are passed as strings to keep this layer free of android.net.Uri.
 */
interface RouteRepository {
    /** Copies the picked file into app storage, parses it, persists it and reports progress. */
    fun importRoute(uri: String): Flow<ImportProgress>

    /** Persists a fully built route together with the original file it came from. */
    suspend fun saveRoute(route: GeoRoute, originalFilePath: String): RouteSummary

    fun observeRoutes(): Flow<List<RouteSummary>>
    fun observeRoute(id: String): Flow<RouteSummary?>
    fun searchRoutes(query: String, format: SourceFormat?): Flow<List<RouteSummary>>

    suspend fun getRoute(id: String): GeoRoute?
    suspend fun getRoutePoints(id: String): RouteGeometry?

    suspend fun renameRoute(id: String, newName: String)
    suspend fun deleteRoute(id: String)

    /** Writes the original imported file (byte-for-byte, so nothing is lost) to a user-chosen content:// URI. */
    suspend fun exportRoute(id: String, destinationUri: String)

    /** Prepares a share-sheet-ready copy of the original file. */
    suspend fun prepareShare(id: String): ShareRequest
}
