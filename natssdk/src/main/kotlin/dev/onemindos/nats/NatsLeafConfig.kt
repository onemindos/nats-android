package dev.onemindos.nats

/**
 * Configuration for connecting to the OneMind NATS leaf node.
 *
 * @param serverUrl WebSocket URL of the NATS server, e.g. "wss://ws.onemindos.dev"
 * @param nkeySeed NKey seed string for authentication (starts with "SU")
 * @param jwtCredentials Full .creds file content (JWT + NKey) for authentication
 * @param caCertPem PEM-encoded CA certificate for TLS verification (null = system trust)
 * @param reconnectWaitMs Delay between reconnect attempts in milliseconds
 * @param maxReconnects Maximum reconnect attempts (-1 = infinite)
 * @param notificationChannelId Android notification channel ID for the foreground notification
 * @param notificationTitle Title shown in the persistent foreground notification
 */
data class NatsLeafConfig(
    val serverUrl: String = "wss://ws.onemindos.dev",
    val nkeySeed: String? = null,
    val jwtCredentials: String? = null,
    val caCertPem: String? = null,
    val reconnectWaitMs: Long = 2_000L,
    val maxReconnects: Int = -1,
    val notificationChannelId: String = "nats_bridge",
    val notificationTitle: String = "OneMind NATS",
)
