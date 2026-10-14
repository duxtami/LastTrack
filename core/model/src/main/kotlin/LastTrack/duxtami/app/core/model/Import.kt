package LastTrack.duxtami.app.core.model

/** Why an import (or parse) failed. Mapped to user-facing text by the UI; never carries raw exception text. */
enum class ImportError {
    UNREADABLE_FILE,
    UNSUPPORTED_FORMAT,
    EMPTY_FILE,
    MALFORMED_FILE,
    NO_COORDINATES,
    INVALID_ARCHIVE,
    NO_KML_IN_ARCHIVE,
    FILE_TOO_LARGE,
    STORAGE_FAILURE,
    UNKNOWN,
}

/** Stages of an import, emitted through a Flow so the UI can render progress. */
sealed interface ImportProgress {
    data object Reading : ImportProgress
    data class Parsing(val pointsParsed: Long = 0) : ImportProgress
    data object Processing : ImportProgress
    data object CalculatingStatistics : ImportProgress
    data object Preparing : ImportProgress
    data class Completed(val routeId: String) : ImportProgress
    data class Failed(val error: ImportError) : ImportProgress
}
