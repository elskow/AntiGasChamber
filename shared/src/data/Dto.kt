package org.unesa.agc.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("userId") val userId: Long,
)

@Serializable
data class PushTokenRequest(val token: String, val platform: String)

@Serializable
data class DeviceDto(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("status") val status: String,
    @SerialName("lastSeen") val lastSeen: String? = null,
    @SerialName("lastPpm") val lastPpm: Double? = null,
)

@Serializable
data class ReadingDto(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("ppm") val ppm: Double,
    @SerialName("temp") val temperature: Double? = null,
    @SerialName("rawRatio") val rawRatio: Double? = null,
    @SerialName("receivedAt") val receivedAt: String? = null,
)

@Serializable
data class AlertDto(
    @SerialName("alertId") val alertId: String,
    @SerialName("deviceId") val deviceId: String,
    @SerialName("severity") val severity: String,
    @SerialName("peakPpm") val peakPpm: Double,
    @SerialName("triggeredAt") val triggeredAt: String,
)

@Serializable
data class ProvisionDeviceRequest(
    @SerialName("deviceId") val deviceId: String,
)

@Serializable
data class MqttInfo(
    @SerialName("username") val username: String,
    @SerialName("topic") val topic: String,
)

@Serializable
data class ProvisionDeviceResponse(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("secret") val secret: String,
    @SerialName("mqtt") val mqtt: MqttInfo,
)
