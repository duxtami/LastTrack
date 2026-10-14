package LastTrack.duxtami.app.core.model

import java.util.Locale

/** File formats LastTrack can import. The core model itself is format-independent. */
enum class SourceFormat(
    val extension: String,
    val displayName: String,
    val mimeType: String,
) {
    GPX("gpx", "GPX", "application/gpx+xml"),
    KML("kml", "KML", "application/vnd.google-earth.kml+xml"),
    KMZ("kmz", "KMZ", "application/vnd.google-earth.kmz");

    companion object {
        fun fromExtension(extension: String?): SourceFormat? {
            val e = extension?.trim()?.removePrefix(".")?.lowercase(Locale.ROOT) ?: return null
            return entries.firstOrNull { it.extension == e }
        }

        fun fromFileName(fileName: String?): SourceFormat? {
            if (fileName == null || !fileName.contains('.')) return null
            return fromExtension(fileName.substringAfterLast('.'))
        }
    }
}
