package LastTrack.duxtami.app.ui.common

import LastTrack.duxtami.app.core.model.RouteGeometry
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object GoogleMapsLauncher {
    /**
     * Opens the route directly in the official Google Maps app on the device.
     * Uses directions (origin -> destination) if available, or location search.
     */
    fun openRoute(context: Context, routeName: String, geometry: RouteGeometry?) {
        val allPoints = geometry?.segments.orEmpty().flatMap { it.points() }
        val uri = if (allPoints.isNotEmpty()) {
            val start = allPoints.first()
            val end = allPoints.last()
            if (allPoints.size > 1 && (start.latitude != end.latitude || start.longitude != end.longitude)) {
                val waypoints = geometry?.waypoints.orEmpty().take(8)
                val waypointsParam = if (waypoints.isNotEmpty()) {
                    "&waypoints=" + waypoints.joinToString("%7C") { "${it.latitude},${it.longitude}" }
                } else ""
                Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}$waypointsParam")
            } else {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${start.latitude},${start.longitude}")
            }
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(routeName))
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }
}
