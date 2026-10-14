package LastTrack.duxtami.app.core.parser

import LastTrack.duxtami.app.core.model.SourceFormat
import java.util.Locale

/** Decides the real format of a picked file. Content wins over the extension (providers often lie about both). */
object FormatDetector {
    const val HEAD_BYTES = 2048

    fun detect(fileName: String?, head: ByteArray): SourceFormat? {
        if (head.size >= 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() &&
            (head[2].toInt() == 3 || head[2].toInt() == 5 || head[2].toInt() == 7)
        ) return SourceFormat.KMZ

        val text = String(head, Charsets.UTF_8).lowercase(Locale.ROOT)
        if (text.contains("<gpx")) return SourceFormat.GPX
        if (text.contains("<kml")) return SourceFormat.KML

        return SourceFormat.fromFileName(fileName)
    }
}
