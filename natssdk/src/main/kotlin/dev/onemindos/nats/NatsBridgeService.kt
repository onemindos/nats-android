package dev.onemindos.nats

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class NatsBridgeService : Service() {

    private var conn: NatsLeafConnection? = null

    private val binder = object : INatsLeafService.Stub() {
        override fun isConnected(): Boolean = conn?.isConnected == true

        override fun publish(subject: String, payload: ByteArray) {
            conn?.publish(subject, payload)
        }

        override fun subscribe(subject: String, callback: INatsMessageCallback) {
            conn?.subscribe(subject) { subj, data ->
                runCatching { callback.onMessage(subj, data) }
            }
        }

        override fun unsubscribe(subject: String) {
            conn?.unsubscribe(subject)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = intent?.getParcelableExtra<NatsLeafConfigParcel>(EXTRA_CONFIG)
            ?.toConfig()
            ?: NatsLeafConfig()

        ensureNotificationChannel(config.notificationChannelId)
        startForeground(NOTIFICATION_ID, buildNotification(config))

        if (conn == null) {
            conn = NatsLeafConnection(config).also { it.connect() }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        conn?.close()
        conn = null
        super.onDestroy()
    }

    private fun ensureNotificationChannel(channelId: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(channelId) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "NATS Bridge", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(config: NatsLeafConfig): Notification =
        NotificationCompat.Builder(this, config.notificationChannelId)
            .setContentTitle(config.notificationTitle)
            .setContentText(config.serverUrl)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()

    companion object {
        const val EXTRA_CONFIG = "dev.onemindos.nats.CONFIG"
        private const val NOTIFICATION_ID = 7437
    }
}
