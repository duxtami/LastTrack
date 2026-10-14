package LasTrack.duxtami.app.core.parser

import LasTrack.duxtami.app.core.model.ImportError
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CancellationException
import java.util.zip.ZipInputStream

data class ZipLimits(
    val maxEntries: Int = 5_000,
    val maxTotalBytes: Long = 512L shl 20,
    val maxKmlEntryBytes: Long = 256L shl 20,
    /** Inflated bytes / compressed bytes. Only enforced once [ratioCheckThresholdBytes] have been inflated. */
    val maxCompressionRatio: Long = 200,
    val ratioCheckThresholdBytes: Long = 16L shl 20,
)

object ZipEntryNames {
    /** Rejects absolute paths, drive letters, backslashes, NUL and any ".." path segment (zip slip). */
    fun isSafe(name: String): Boolean {
        if (name.isEmpty() || name.contains('\u0000') || name.contains('\\')) return false
        if (name.startsWith("/")) return false
        if (name.length >= 2 && name[1] == ':') return false
        return name.split('/').none { it == ".." }
    }
}

/**
 * Streams a KMZ (zip) archive and extracts only its `.kml` entries into [targetDir].
 *
 * Safety: entry names are validated, files are written under randomly generated names (never the entry name),
 * the canonical path is re-checked to be inside [targetDir], and entry count / total / per-entry size /
 * compression ratio are all bounded. Every other entry is inflated into the void (still counted) so a zip
 * bomb hiding in a non-KML entry is caught too.
 */
class SafeZipExtractor(private val limits: ZipLimits = ZipLimits()) {
    class Extracted(val entryName: String, val file: File)

    fun extractKmlEntries(
        input: InputStream,
        targetDir: File,
        context: ParseContext = ParseContext.None,
    ): List<Extracted> {
        targetDir.mkdirs()
        val dir = targetDir.canonicalFile
        val counting = CountingInputStream(input)
        val result = ArrayList<Extracted>()
        try {
            ZipInputStream(counting).use { zip ->
                var entries = 0
                var total = 0L
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    if (entries > limits.maxEntries) throw tooLarge("too many archive entries")
                    if (!ZipEntryNames.isSafe(entry.name)) {
                        throw RouteParseException(ImportError.INVALID_ARCHIVE, "Unsafe path in archive")
                    }
                    if (entry.isDirectory) continue
                    val isKml = entry.name.endsWith(".kml", ignoreCase = true)
                    if (entry.size > 0 && (entry.size > limits.maxTotalBytes || (isKml && entry.size > limits.maxKmlEntryBytes))) {
                        throw tooLarge("entry too large")
                    }
                    var out: OutputStream? = null
                    var file: File? = null
                    try {
                        if (isKml) {
                            file = File.createTempFile("kmz-", ".kml", dir)
                            if (!file.canonicalFile.parentFile.canonicalPath.equals(dir.path)) {
                                throw RouteParseException(ImportError.INVALID_ARCHIVE, "Unsafe extraction target")
                            }
                            out = BufferedOutputStream(FileOutputStream(file))
                        }
                        var entryBytes = 0L
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            entryBytes += n
                            total += n
                            if (isKml && entryBytes > limits.maxKmlEntryBytes) throw tooLarge("KML entry too large")
                            if (total > limits.maxTotalBytes) throw tooLarge("archive too large when extracted")
                            if (total > limits.ratioCheckThresholdBytes &&
                                total / maxOf(counting.count, 1L) > limits.maxCompressionRatio
                            ) throw tooLarge("suspicious compression ratio")
                            out?.write(buffer, 0, n)
                            context.checkCancelled()
                        }
                        out?.close()
                        out = null
                        if (file != null) {
                            result.add(Extracted(entry.name, file))
                            file = null
                        }
                    } finally {
                        try { out?.close() } catch (_: IOException) {}
                        file?.delete()
                    }
                }
                if (entries == 0) throw RouteParseException(ImportError.INVALID_ARCHIVE, "Not a valid archive")
                if (result.isEmpty()) throw RouteParseException(ImportError.NO_KML_IN_ARCHIVE, "No KML in archive")
            }
        } catch (e: RouteParseException) {
            result.forEach { it.file.delete() }
            throw e
        } catch (e: CancellationException) {
            result.forEach { it.file.delete() }
            throw e
        } catch (e: IOException) {
            result.forEach { it.file.delete() }
            throw RouteParseException(ImportError.INVALID_ARCHIVE, "Corrupt archive", e)
        } catch (e: IllegalArgumentException) {
            result.forEach { it.file.delete() }
            throw RouteParseException(ImportError.INVALID_ARCHIVE, "Corrupt archive", e)
        }
        return result
    }

    private fun tooLarge(why: String) = RouteParseException(ImportError.FILE_TOO_LARGE, "Archive rejected: $why")

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var count = 0L
            private set

        override fun read(): Int = super.read().also { if (it >= 0) count++ }
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            super.read(b, off, len).also { if (it > 0) count += it }
    }
}

/** KMZ = zip containing a KML (usually `doc.kml`). Extracts safely, then streams the KML through [KmlParser]. */
class KmzReader(
    private val workDir: File,
    private val kmlParser: KmlParser = KmlParser(),
    private val limits: ZipLimits = ZipLimits(),
) : RouteParser {
    override val format get() = LasTrack.duxtami.app.core.model.SourceFormat.KMZ

    override fun parse(input: InputStream, context: ParseContext): ParsedRoute {
        val extracted = SafeZipExtractor(limits).extractKmlEntries(requireNonEmpty(input), workDir, context)
        try {
            val ordered = extracted.sortedWith(
                compareByDescending<SafeZipExtractor.Extracted> { it.entryName.equals("doc.kml", ignoreCase = true) }
                    .thenBy { it.entryName.count { c -> c == '/' } },
            )
            var firstError: RouteParseException? = null
            for (candidate in ordered) {
                try {
                    return FileInputStream(candidate.file).buffered(64 * 1024).use { kmlParser.parse(it, context) }
                } catch (e: RouteParseException) {
                    if (firstError == null) firstError = e
                }
            }
            throw firstError ?: RouteParseException(ImportError.NO_KML_IN_ARCHIVE, "No KML in archive")
        } finally {
            extracted.forEach { it.file.delete() }
        }
    }
}
