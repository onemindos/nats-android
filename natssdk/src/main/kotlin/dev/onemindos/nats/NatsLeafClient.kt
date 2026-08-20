package dev.onemindos.nats

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * High-level client for the OneMind NATS leaf node background service.
 *
 * Usage:
 * ```kotlin
 * val client = NatsLeafClient(context, NatsLeafConfig(serverUrl = "wss://ws.onemindos.dev"))
 * client.connect()
 * client.publish("onemind.cot", cotBytes)
 * client.messages("onemind.>").collect { (subject, payload) -> ... }
 * client.disconnect()
 * ```
 */
class NatsLeafClient(
    private val context: Context,
    private val config: NatsLeafConfig = NatsLeafConfig(),
) {
    private var service: INatsLeafService? = null
    private var serviceConn: ServiceConnection? = null

    val isConnected: Boolean get() = service?.isConnected == true

    fun connect(onReady: (() -> Unit)? = null) {
        val intent = Intent(context, NatsBridgeService::class.java).apply {
            putExtra(NatsBridgeService.EXTRA_CONFIG, NatsLeafConfigParcel.from(config))
        }
        context.startForegroundService(intent)

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = INatsLeafService.Stub.asInterface(binder)
                onReady?.invoke()
            }
            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        }
        serviceConn = conn
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    fun disconnect() {
        serviceConn?.let { context.unbindService(it) }
        serviceConn = null
        service = null
        context.stopService(Intent(context, NatsBridgeService::class.java))
    }

    fun publish(subject: String, payload: ByteArray) {
        service?.publish(subject, payload)
    }

    fun publish(subject: String, payload: String) {
        publish(subject, payload.toByteArray())
    }

    /** Returns a cold Flow that subscribes to [subject] and emits (subject, payload) pairs. */
    fun messages(subject: String): Flow<Pair<String, ByteArray>> = callbackFlow {
        val callback = object : INatsMessageCallback.Stub() {
            override fun onMessage(subject: String, payload: ByteArray) {
                trySend(subject to payload)
            }
        }
        service?.subscribe(subject, callback)
        awaitClose { service?.unsubscribe(subject) }
    }
}
