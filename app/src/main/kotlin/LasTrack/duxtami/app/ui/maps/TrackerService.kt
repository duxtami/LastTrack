package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.MainActivity
import LasTrack.duxtami.app.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service inspired by MBCompass TrackerService.
 * Keeps GPS tracking alive reliably even when the device is locked or the app is backgrounded.
 */
@AndroidEntryPoint
class TrackerService : Service() {

    @Inject
    lateinit var recordingManager: TrackRecordingManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

    companion object {
        const val CHANNEL_ID = "lastrack_tracking_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "LasTrack.action.START_TRACKING"
        const val ACTION_STOP = "LasTrack.action.STOP_TRACKING"

        fun start(context: Context) {
            val intent = Intent(context, TrackerService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackerService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification("Recording started...", "0.0 m • 00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                },
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startNotificationUpdates()
        return START_STICKY
    }

    private fun startNotificationUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val status = recordingManager.status.value
                if (status == RecordingStatus.IDLE) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                val stats = recordingManager.stats.value
                val distStr = if (stats.distanceMeters < 1000.0) {
                    String.format(java.util.Locale.US, "%.0f m", stats.distanceMeters)
                } else {
                    String.format(java.util.Locale.US, "%.2f km", stats.distanceMeters / 1000.0)
                }
                val durSec = stats.durationMillis / 1000
                val timeStr = String.format(java.util.Locale.US, "%02d:%02d", durSec / 60, durSec % 60)
                val title = if (status == RecordingStatus.RECORDING) "Recording Track..." else "Recording Paused"
                val content = "$distStr • $timeStr"
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(title, content))
            }
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Track Recording",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows ongoing track recording progress and stats"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
