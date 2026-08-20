package dev.onemindos.nats.demo

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.onemindos.nats.NatsLeafClient
import dev.onemindos.nats.NatsLeafConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private lateinit var client: NatsLeafClient
    private lateinit var logView: TextView
    private var collectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        client = NatsLeafClient(
            context = this,
            config = NatsLeafConfig(serverUrl = "wss://ws.onemindos.dev"),
        )

        logView = TextView(this).apply { setPadding(16, 16, 16, 16) }

        val connectBtn = Button(this).apply {
            text = "Connect"
            setOnClickListener {
                client.connect { log("Connected — isConnected=${client.isConnected}") }
                startCollecting()
            }
        }

        val publishBtn = Button(this).apply {
            text = "Publish ping"
            setOnClickListener {
                client.publish("onemind.ping", "ping from android")
                log("→ onemind.ping")
            }
        }

        val disconnectBtn = Button(this).apply {
            text = "Disconnect"
            setOnClickListener {
                collectJob?.cancel()
                client.disconnect()
                log("Disconnected")
            }
        }

        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                addView(connectBtn)
                addView(publishBtn)
                addView(disconnectBtn)
                addView(logView)
            })
        })
    }

    private fun startCollecting() {
        collectJob = scope.launch {
            client.messages("onemind.>").collect { (subject, payload) ->
                log("← $subject: ${payload.decodeToString().take(80)}")
            }
        }
    }

    private fun log(msg: String) {
        logView.append("$msg\n")
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onDestroy() {
        collectJob?.cancel()
        super.onDestroy()
    }
}
