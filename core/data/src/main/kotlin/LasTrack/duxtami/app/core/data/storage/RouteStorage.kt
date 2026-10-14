package LasTrack.duxtami.app.core.data.storage

import LasTrack.duxtami.app.core.model.SourceFormat
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the on-disk layout: filesDir/routes/<id>/{original.<ext>, coordinates.ltk}. */
@Singleton
class RouteStorage @Inject constructor(@ApplicationContext private val context: Context) {
    private val root: File get() = File(context.filesDir, "routes")

    fun routeDir(id: String): File {
        require(id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '-' }) { "Invalid route id" }
        return File(root, id)
    }

    fun coordinatesFile(id: String) = File(routeDir(id), "coordinates.ltk")
    fun originalFile(id: String, format: SourceFormat) = File(routeDir(id), "original.${format.extension}")
    fun tempSourceFile(id: String) = File(routeDir(id), "source.tmp")

    /** Scratch space for safe KMZ extraction. Never user-controlled. */
    fun importWorkDir(): File = File(context.cacheDir, "import-work").apply { mkdirs() }

    fun shareDir(): File = File(context.cacheDir, "share").apply { mkdirs() }

    fun deleteRoute(id: String): Boolean = routeDir(id).deleteRecursively()
}
