package org.unesa.agc.data

import org.unesa.agc.data.remote.AuthApi
import org.unesa.agc.data.DeviceDto
import org.unesa.agc.data.ProvisionDeviceResponse

class UnauthorizedException : Exception("session expired")

class ApiException(val status: Int, message: String) : Exception(message)

fun friendlyError(e: Exception): String = when (e) {
    is UnauthorizedException -> "Session expired. Please sign in again."
    is ApiException -> e.message ?: "Something went wrong."
    is io.ktor.client.plugins.HttpRequestTimeoutException -> "The server took too long to respond."
    else -> when {
        e.message?.contains("Failed to connect", ignoreCase = true) == true ||
            e.message?.contains("Connection refused", ignoreCase = true) == true ||
            e.message?.contains("connect", ignoreCase = true) == true -> "Can't reach the server. Check your connection or try again later."
        e.message?.contains("resolve", ignoreCase = true) == true ||
            e.message?.contains("UnknownHost", ignoreCase = true) == true -> "Can't find the server. Check your internet connection."
        e.message?.contains("JSON", ignoreCase = true) == true ||
            e.message?.contains("serializ", ignoreCase = true) == true -> "The server sent an unexpected response."
        else -> e.message ?: "Something went wrong. Try again."
    }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatIso(iso: String?): String? {
    if (iso == null || iso.length < 16) return null
    val date = iso.substring(0, 10).split("-")
    val month = date.getOrNull(1)?.toIntOrNull()?.let { MONTHS.getOrNull(it - 1) } ?: return null
    return "${date[2]} $month ${iso.substring(11, 16)} UTC"
}

expect class HttpClientFactory() {
    fun create(): io.ktor.client.HttpClient
}

class AgcApi(
    baseUrl: String,
    private val tokenStore: TokenStore,
    httpClientFactory: HttpClientFactory,
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val client = httpClientFactory.create()
    private val base = baseUrl

    private val authApi = AuthApi(baseUrl, client, json, tokenStore)
    private val pushApi = org.unesa.agc.data.remote.PushApi(baseUrl, client, json, tokenStore)
    private val nodes = org.unesa.agc.data.remote.AgcClient(baseUrl, client, json, tokenStore)

    suspend fun login(email: String, password: String): LoginResponse = authApi.login(email, password)

    suspend fun register(email: String, password: String, code: String): LoginResponse =
        authApi.register(email, password, code)

    suspend fun registerPushToken(token: String, platform: String) = pushApi.registerToken(token, platform)

    suspend fun devices(): List<DeviceDto> = nodes.devices()

    suspend fun readings(deviceId: String, limit: Int = 100, before: Long? = null): List<ReadingDto> =
        nodes.readings(deviceId, limit, before)

    suspend fun alerts(limit: Int = 50, before: String? = null): PagedAlerts = nodes.alerts(limit, before)

    suspend fun markAlertRead(alertId: String) = nodes.markAlertRead(alertId)

    suspend fun provisionDevice(deviceId: String): ProvisionDeviceResponse = nodes.provisionDevice(deviceId)

    suspend fun deleteDevice(deviceId: String) = nodes.deleteDevice(deviceId)

    suspend fun rotateDevice(deviceId: String): ProvisionDeviceResponse = nodes.rotateDevice(deviceId)

    suspend fun users(): UsersResponse = nodes.users()

    suspend fun deleteUser(userId: Long) = nodes.deleteUser(userId)

    suspend fun changeUserPassword(userId: Long, password: String) = nodes.changeUserPassword(userId, password)
}
