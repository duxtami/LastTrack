package LastTrack.duxtami.app.core.data.repository

import LastTrack.duxtami.app.core.data.db.RouteDao
import LastTrack.duxtami.app.core.data.db.RouteEntity
import LastTrack.duxtami.app.core.data.db.sourceFormat
import LastTrack.duxtami.app.core.data.db.toStats
import LastTrack.duxtami.app.core.data.db.toSummary
import LastTrack.duxtami.app.core.data.di.IoDispatcher
import LastTrack.duxtami.app.core.data.storage.RouteStorage
import LastTrack.duxtami.app.core.model.GeoRoute
import LastTrack.duxtami.app.core.model.ImportProgress
import LastTrack.duxtami.app.core.model.RouteFilter
import LastTrack.duxtami.app.core.model.RouteGeometry
import LastTrack.duxtami.app.core.model.RouteMetadata
import LastTrack.duxtami.app.core.model.RouteNotFoundException
import LastTrack.duxtami.app.core.model.RouteRepository
import LastTrack.duxtami.app.core.model.RouteSummary
import LastTrack.duxtami.app.core.model.ShareRequest
import LastTrack.duxtami.app.core.model.SourceFormat
import LastTrack.duxtami.app.core.parser.RouteBinaryCodec
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: RouteDao,
    private val storage: RouteStorage,
    private val importer: RouteImporter,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RouteRepository {

    override fun importRoute(uri: String): Flow<ImportProgress> = importer.import(uri)

    override suspend fun saveRoute(route: GeoRoute, originalFilePath: String): RouteSummary = withContext(io) {
        val format = route.sourceFormat
        val dir = storage.routeDir(route.id).also { it.mkdirs() }
        val original = storage.originalFile(route.id, format)
        File(originalFilePath).copyTo(original, overwrite = true)
        val coordinates = storage.coordinatesFile(route.id)
        coordinates.outputStream().buffered(64 * 1024).use { RouteBinaryCodec.write(it, route.segments, route.waypoints) }
        val s = route.stats
        val entity = RouteEntity(
            id = route.id, name = route.name, fileName = route.sourceFile ?: original.name, format = format.name,
            fileSizeBytes = original.length(), distanceMeters = s.distanceMeters, durationMillis = s.durationMillis,
            elevationGainMeters = s.elevationGainMeters, elevationLossMeters = s.elevationLossMeters,
            maxElevationMeters = s.maxElevationMeters, minElevationMeters = s.minElevationMeters,
            averageSpeedMps = s.averageSpeedMetersPerSecond, pointCount = s.pointCount, segmentCount = s.segmentCount,
            waypointCount = s.waypointCount, description = route.metadata.description, importedAt = route.createdAt,
            coordinatesPath = coordinates.absolutePath, originalPath = original.absolutePath,
        )
        check(dir.exists())
        dao.insert(entity)
        entity.toSummary()
    }

    override fun observeRoutes(): Flow<List<RouteSummary>> = dao.observeAll().map { list -> list.map { it.toSummary() } }

    override fun observeRoute(id: String): Flow<RouteSummary?> = dao.observe(id).map { it?.toSummary() }

    override fun searchRoutes(query: String, format: SourceFormat?): Flow<List<RouteSummary>> =
        observeRoutes().map { RouteFilter.filter(it, query, format) }

    override suspend fun getRoutePoints(id: String): RouteGeometry? = withContext(io) {
        val entity = dao.get(id) ?: return@withContext null
        readGeometry(entity)
    }

    override suspend fun getRoute(id: String): GeoRoute? = withContext(io) {
        val entity = dao.get(id) ?: return@withContext null
        val geometry = readGeometry(entity)
        GeoRoute(
            id = entity.id, name = entity.name, sourceFile = entity.fileName, sourceFormat = entity.sourceFormat(),
            segments = geometry.segments, waypoints = geometry.waypoints,
            metadata = RouteMetadata(entity.description), stats = entity.toStats(), createdAt = entity.importedAt,
        )
    }

    override suspend fun renameRoute(id: String, newName: String) {
        val name = newName.trim()
        require(name.isNotEmpty()) { "Name must not be blank" }
        withContext(io) { if (dao.rename(id, name) == 0) throw RouteNotFoundException(id) }
    }

    override suspend fun deleteRoute(id: String) = withContext(io) {
        dao.delete(id)
        storage.deleteRoute(id)
        Unit
    }

    override suspend fun exportRoute(id: String, destinationUri: String) = withContext(io) {
        val entity = dao.get(id) ?: throw RouteNotFoundException(id)
        val out = context.contentResolver.openOutputStream(Uri.parse(destinationUri), "w")
            ?: throw IOException("Cannot open destination")
        out.use { stream -> FileInputStream(entity.originalPath).use { it.copyTo(stream) } }
        Unit
    }

    override suspend fun prepareShare(id: String): ShareRequest = withContext(io) {
        val entity = dao.get(id) ?: throw RouteNotFoundException(id)
        val format = entity.sourceFormat()
        val shareDir = storage.shareDir()
        shareDir.listFiles()?.forEach { it.delete() } // keep the cache tidy: one share at a time
        val target = File(shareDir, sanitizeFileName(entity.name) + "." + format.extension)
        File(entity.originalPath).copyTo(target, overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        ShareRequest(uri.toString(), format.mimeType, target.name)
    }

    private fun readGeometry(entity: RouteEntity): RouteGeometry {
        val file = File(entity.coordinatesPath)
        return BufferedInputStream(FileInputStream(file), 64 * 1024).use { RouteBinaryCodec.read(it) }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.map { if (it.isLetterOrDigit() || it in " ._-") it else '_' }.joinToString("").trim().take(80)
        return cleaned.ifEmpty { "route" }
    }
}
