package org.unesa.agc.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.unesa.agc.push.AndroidPushDefaults.bridge

class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        bridge.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val alertId = data["alertId"] ?: return
        bridge.alertCenter.deliver(
            LiveAlert(
                id = alertId,
                deviceId = data["deviceId"] ?: "unknown",
                severity = AlertSeverity.from(data["severity"] ?: ""),
                peakPpm = data["peakPpm"]?.toDoubleOrNull() ?: 0.0,
            ),
        )
    }
}

object AndroidPushDefaults {
    lateinit var bridge: AndroidPushBridge
        private set

    fun install(bridge: AndroidPushBridge) {
        this.bridge = bridge
    }
}
