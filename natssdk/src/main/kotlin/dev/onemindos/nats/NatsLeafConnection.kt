package dev.onemindos.nats

import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.authhandler.NKeyAuthHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.StringReader
import java.time.Duration

internal class NatsLeafConnection(private val config: NatsLeafConfig) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connection: Connection? = null
    private val dispatchers = mutableMapOf<String, Dispatcher>()

    val isConnected: Boolean
        get() = connection?.status == Connection.Status.CONNECTED

    fun connect() {
        scope.launch {
            val opts = buildOptions()
            connection = Nats.connect(opts)
        }
    }

    fun publish(subject: String, payload: ByteArray) {
        connection?.publish(subject, payload)
    }

    fun subscribe(subject: String, callback: (subject: String, payload: ByteArray) -> Unit) {
        val conn = connection ?: return
        val dispatcher = conn.createDispatcher { msg ->
            callback(msg.subject, msg.data ?: ByteArray(0))
        }
        dispatcher.subscribe(subject)
        dispatchers[subject] = dispatcher
    }

    fun unsubscribe(subject: String) {
        dispatchers.remove(subject)?.unsubscribe(subject)
    }

    fun close() {
        dispatchers.clear()
        connection?.close()
        connection = null
        scope.cancel()
    }

    private fun buildOptions(): Options {
        val builder = Options.Builder()
            .server(config.serverUrl)
            .reconnectWait(Duration.ofMillis(config.reconnectWaitMs))
            .connectionListener { _, _ -> }

        if (config.maxReconnects >= 0) {
            builder.maxReconnects(config.maxReconnects)
        } else {
            builder.maxReconnects(-1)
        }

        when {
            config.jwtCredentials != null -> {
                builder.authHandler(Nats.credentials(StringReader(config.jwtCredentials)))
            }
            config.nkeySeed != null -> {
                builder.authHandler(NKeyAuthHandler(config.nkeySeed.toCharArray()))
            }
        }

        return builder.build()
    }
}
