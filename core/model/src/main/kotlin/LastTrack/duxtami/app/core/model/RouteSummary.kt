package LastTrack.duxtami.app.core.model

import java.util.Locale

/** Lightweight, metadata-only view of a saved route. Safe to hold in lists; contains no coordinates. */
data class RouteSummary(
    val id: String,
    val name: String,
    val fileName: String,
    val format: SourceFormat,
    val fileSizeBytes: Long,
    val stats: RouteStats,
    val description: String?,
    /** Epoch millis. */
    val importedAt: Long,
)

/** Pure search/filter logic shared by every screen that lists routes. */
object RouteFilter {
    /**
     * Every whitespace-separated token of [query] must appear (case-insensitively) in the route name,
     * the file name or the format name. A null [format] means "all formats".
     */
    fun filter(routes: List<RouteSummary>, query: String, format: SourceFormat?): List<RouteSummary> {
        val tokens = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }
        return routes.filter { route ->
            (format == null || route.format == format) && tokens.all { token ->
                route.name.lowercase(Locale.ROOT).contains(token) ||
                    route.fileName.lowercase(Locale.ROOT).contains(token) ||
                    route.format.displayName.lowercase(Locale.ROOT).contains(token)
            }
        }
    }
}
