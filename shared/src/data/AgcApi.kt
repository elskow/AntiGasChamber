package org.unesa.agc.data

import org.unesa.agc.data.remote.AuthApi
import org.unesa.agc.data.DeviceDto
import org.unesa.agc.data.ProvisionDeviceResponse

class UnauthorizedException : Exception("401")

class ApiException(val status: Int, message: String) : Exception(message)

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

    suspend fun registerPushToken(token: String, platform: String) = pushApi.registerToken(token, platform)

    suspend fun devices(): List<DeviceDto> = nodes.devices()

    suspend fun readings(deviceId: String, limit: Int = 100): List<ReadingDto> = nodes.readings(deviceId, limit)

    suspend fun alerts(limit: Int = 50): List<AlertDto> = nodes.alerts(limit)

    suspend fun provisionDevice(deviceId: String): ProvisionDeviceResponse = nodes.provisionDevice(deviceId)
}
