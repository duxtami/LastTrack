package LasTrack.duxtami.app.ui.common

import LasTrack.duxtami.app.core.model.RouteGeometry
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

        launchIntent(context, uri)
    }

    /**
     * Opens specific coordinates directly in official Google Maps app.
     */
    fun openCoordinates(context: Context, latitude: Double, longitude: Double, label: String? = null) {
        val query = if (!label.isNullOrBlank()) {
            "$latitude,$longitude(${Uri.encode(label)})"
        } else {
            "$latitude,$longitude"
        }
        val uri = Uri.parse("geo:$latitude,$longitude?q=$query")
        val webFallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        launchIntent(context, uri, webFallback)
    }

    private fun launchIntent(context: Context, primaryUri: Uri, fallbackUri: Uri = primaryUri) {
        val mapIntent = Intent(Intent.ACTION_VIEW, primaryUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }
}
