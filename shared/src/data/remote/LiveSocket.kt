package org.unesa.agc.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.unesa.agc.data.TokenStore

@Serializable
private class WsHello(val token: String)

@Serializable
private class WsMessage(
    val type: String = "",
    val alertId: String? = null,
    val deviceId: String? = null,
    val severity: String? = null,
    val peakPpm: Double? = null,
    val cleared: Boolean = false,
    val ppm: Double? = null,
)

class LiveSocket(
    baseUrl: String,
    private val tokenStore: TokenStore,
) {
    private val url = baseUrl
        .replace("https://", "wss://")
        .replace("http://", "ws://")
        .trimEnd('/') + "/ws"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient { install(WebSockets) }

    var onAlert: ((alertId: String, deviceId: String, severity: String, peakPpm: Double, cleared: Boolean) -> Unit)? = null
    var onReading: ((deviceId: String, ppm: Double) -> Unit)? = null

    private var job: Job? = null
    private var running = false

    fun start(scope: CoroutineScope) {
        if (running) return
        running = true
        job = scope.launch { loop() }
    }

    fun stop() {
        running = false
        job?.cancel()
        job = null
    }

    private suspend fun loop() {
        while (running) {
            try {
                connect()
            } catch (_: Exception) {
            }
            delay(5000)
        }
    }

    private suspend fun connect() {
        val token = tokenStore.token() ?: return
        client.webSocket(urlString = url) {
            send(Frame.Text(json.encodeToString(WsHello.serializer(), WsHello(token))))
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val message = json.decodeFromString(WsMessage.serializer(), frame.readText())
                when (message.type) {
                    "alert" -> onAlert?.invoke(
                        message.alertId.orEmpty(),
                        message.deviceId.orEmpty(),
                        message.severity.orEmpty(),
                        message.peakPpm ?: 0.0,
                        message.cleared,
                    )
                    "reading" -> onReading?.invoke(message.deviceId.orEmpty(), message.ppm ?: 0.0)
                }
            }
        }
    }
}
