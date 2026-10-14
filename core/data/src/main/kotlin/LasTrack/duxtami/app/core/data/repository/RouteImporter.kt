package LasTrack.duxtami.app.core.data.repository

import LasTrack.duxtami.app.core.data.db.RouteDao
import LasTrack.duxtami.app.core.data.db.RouteEntity
import LasTrack.duxtami.app.core.data.di.IoDispatcher
import LasTrack.duxtami.app.core.data.storage.RouteStorage
import LasTrack.duxtami.app.core.model.ImportError
import LasTrack.duxtami.app.core.model.ImportProgress
import LasTrack.duxtami.app.core.model.SourceFormat
import LasTrack.duxtami.app.core.parser.FormatDetector
import LasTrack.duxtami.app.core.parser.GpxParser
import LasTrack.duxtami.app.core.parser.KmlParser
import LasTrack.duxtami.app.core.parser.KmzReader
import LasTrack.duxtami.app.core.parser.ParseContext
import LasTrack.duxtami.app.core.parser.RouteBinaryCodec
import LasTrack.duxtami.app.core.parser.RouteParseException
import LasTrack.duxtami.app.core.parser.RouteParser
import LasTrack.duxtami.app.core.parser.RouteStatsCalculator
import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * The whole import pipeline: copy -> detect -> stream-parse -> encode coordinates -> statistics -> persist.
 * Everything blocking runs on the IO dispatcher; progress is reported through the returned Flow.
 */
@Singleton
class RouteImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: RouteStorage,
    private val dao: RouteDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    fun import(uriString: String): Flow<ImportProgress> = channelFlow {
        val id = UUID.randomUUID().toString()
        send(ImportProgress.Reading)
        val failure: ImportError? = try {
            runImport(uriString, id)
            null
        } catch (e: CancellationException) {
            storage.deleteRoute(id)
            throw e
        } catch (e: RouteParseException) {
            storage.deleteRoute(id)
            e.error
        } catch (e: SQLiteException) {
            storage.deleteRoute(id)
            ImportError.STORAGE_FAILURE
        } catch (e: IOException) {
            storage.deleteRoute(id)
            ImportError.UNREADABLE_FILE
        } catch (e: SecurityException) {
            storage.deleteRoute(id)
            ImportError.UNREADABLE_FILE
        } catch (e: OutOfMemoryError) {
            storage.deleteRoute(id)
            ImportError.FILE_TOO_LARGE
        } catch (e: Exception) {
            storage.deleteRoute(id)
            ImportError.UNKNOWN
        }
        if (failure != null) send(ImportProgress.Failed(failure))
    }.flowOn(io)

    private suspend fun ProducerScope<ImportProgress>.runImport(uriString: String, id: String) {
        val job = coroutineContext.job
        val parseContext = object : ParseContext {
            override fun checkCancelled() = job.ensureActive()
            override fun onPointsParsed(total: Long) {
                trySend(ImportProgress.Parsing(total))
            }
        }

        val uri = Uri.parse(uriString)
        val (displayName, declaredSize) = queryNameAndSize(uri)
        val fileName = displayName ?: uri.lastPathSegment?.substringAfterLast('/') ?: "route"
        if (declaredSize != null && declaredSize > MAX_IMPORT_BYTES) {
            throw RouteParseException(ImportError.FILE_TOO_LARGE, "File too large")
        }

        // 1. Reading: copy the picked file into app-private storage (also becomes the preserved original).
        val dir = storage.routeDir(id).also { it.mkdirs() }
        val temp = storage.tempSourceFile(id)
        val size = copyWithLimit(uri, temp, job)
        if (size == 0L) throw RouteParseException(ImportError.EMPTY_FILE, "File is empty")

        val head = FileInputStream(temp).use { it.readNBytesCompat(FormatDetector.HEAD_BYTES) }
        val format = FormatDetector.detect(fileName, head)
            ?: throw RouteParseException(ImportError.UNSUPPORTED_FORMAT, "Unsupported file type")
        val original = storage.originalFile(id, format)
        if (!temp.renameTo(original)) throw IOException("Could not store original file")

        // 2. Parsing (streaming).
        send(ImportProgress.Parsing(0))
        val parsed = BufferedInputStream(FileInputStream(original), 64 * 1024).use {
            parserFor(format).parse(it, parseContext)
        }

        // 3. Processing: compact binary coordinates.
        send(ImportProgress.Processing)
        job.ensureActive()
        val coordinates = storage.coordinatesFile(id)
        BufferedOutputStream(FileOutputStream(coordinates), 64 * 1024).use {
            RouteBinaryCodec.write(it, parsed.segments, parsed.waypoints)
        }

        // 4. Statistics.
        send(ImportProgress.CalculatingStatistics)
        val stats = RouteStatsCalculator.calculate(parsed.segments, parsed.waypoints.size)

        // 5. Persist metadata.
        send(ImportProgress.Preparing)
        job.ensureActive()
        dao.insert(
            RouteEntity(
                id = id,
                name = parsed.name?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.', fileName),
                fileName = fileName,
                format = format.name,
                fileSizeBytes = size,
                distanceMeters = stats.distanceMeters,
                durationMillis = stats.durationMillis,
                elevationGainMeters = stats.elevationGainMeters,
                elevationLossMeters = stats.elevationLossMeters,
                maxElevationMeters = stats.maxElevationMeters,
                minElevationMeters = stats.minElevationMeters,
                averageSpeedMps = stats.averageSpeedMetersPerSecond,
                pointCount = stats.pointCount,
                segmentCount = stats.segmentCount,
                waypointCount = stats.waypointCount,
                description = parsed.description,
                importedAt = System.currentTimeMillis(),
                coordinatesPath = coordinates.absolutePath,
                originalPath = original.absolutePath,
            ),
        )
        check(dir.exists())
        send(ImportProgress.Completed(id))
    }

    private fun parserFor(format: SourceFormat): RouteParser = when (format) {
        SourceFormat.GPX -> GpxParser()
        SourceFormat.KML -> KmlParser()
        SourceFormat.KMZ -> KmzReader(storage.importWorkDir())
    }

    private fun queryNameAndSize(uri: Uri): Pair<String?, Long?> {
        if (uri.scheme != "content") return uri.lastPathSegment to null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { c.getString(it) }
                    val s = c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { c.getLong(it) }
                    return n to s
                }
            }
        return null to null
    }

    private fun copyWithLimit(uri: Uri, target: File, job: kotlinx.coroutines.Job): Long {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
        var total = 0L
        input.use { src ->
            BufferedOutputStream(FileOutputStream(target), 64 * 1024).use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = src.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_IMPORT_BYTES) throw RouteParseException(ImportError.FILE_TOO_LARGE, "File too large")
                    out.write(buffer, 0, n)
                    job.ensureActive()
                }
            }
        }
        return total
    }

    private fun FileInputStream.readNBytesCompat(max: Int): ByteArray {
        val buf = ByteArray(max)
        var read = 0
        while (read < max) {
            val n = read(buf, read, max - read)
            if (n < 0) break
            read += n
        }
        return buf.copyOf(read)
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 512L * 1024 * 1024
    }
}
