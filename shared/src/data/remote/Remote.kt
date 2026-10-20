package org.unesa.agc.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.unesa.agc.data.AlertDto
import org.unesa.agc.data.ApiException
import org.unesa.agc.data.DeviceDto
import org.unesa.agc.data.LoginRequest
import org.unesa.agc.data.LoginResponse
import org.unesa.agc.data.ProvisionDeviceRequest
import org.unesa.agc.data.ProvisionDeviceResponse
import org.unesa.agc.data.PushTokenRequest
import org.unesa.agc.data.ReadingDto
import org.unesa.agc.data.TokenStore
import org.unesa.agc.data.UnauthorizedException

internal fun statusOf(response: HttpResponse): Int = response.status.value

internal suspend fun bodyOf(response: HttpResponse): String = response.bodyAsText()

internal fun checkStatus(status: Int, body: String) {
    if (status == 401) throw UnauthorizedException()
    if (status >= 400) throw ApiException(status, body)
}

class AuthApi(
    private val baseUrl: String,
    private val client: HttpClient,
    private val json: Json,
    private val tokenStore: TokenStore,
) {
    suspend fun login(email: String, password: String): LoginResponse {
        val response = client.post("$baseUrl/api/v1/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest(email, password)))
        }
        val body = bodyOf(response)
        checkStatus(statusOf(response), body)
        val parsed = json.decodeFromString<LoginResponse>(body)
        tokenStore.save(parsed.token)
        return parsed
    }
}

class PushApi(
    private val baseUrl: String,
    private val client: HttpClient,
    private val json: Json,
    private val tokenStore: TokenStore,
) {
    suspend fun registerToken(token: String, platform: String) {
        val current = tokenStore.token() ?: throw UnauthorizedException()
        val response = client.post("$baseUrl/api/v1/push-tokens") {
            bearerAuth(current)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PushTokenRequest(token, platform)))
        }
        checkStatus(statusOf(response), bodyOf(response))
    }
}

class AgcClient(
    private val baseUrl: String,
    private val client: HttpClient,
    private val json: Json,
    private val tokenStore: TokenStore,
) {
    suspend fun devices(): List<DeviceDto> = get("devices")

    suspend fun readings(deviceId: String, limit: Int): List<ReadingDto> =
        get("devices/$deviceId/readings?limit=$limit")

    suspend fun alerts(limit: Int): List<AlertDto> = get("alerts?limit=$limit")

    suspend fun provisionDevice(deviceId: String): ProvisionDeviceResponse =
        post("devices", ProvisionDeviceRequest(deviceId))

    private suspend inline fun <reified T> get(path: String): T {
        val token = tokenStore.token() ?: throw UnauthorizedException()
        val response = client.get("$baseUrl/api/v1/$path") {
            bearerAuth(token)
        }
        val body = bodyOf(response)
        checkStatus(statusOf(response), body)
        return json.decodeFromString(body)
    }

    private suspend inline fun <reified TReq, reified TRes> post(path: String, payload: TReq): TRes {
        val token = tokenStore.token() ?: throw UnauthorizedException()
        val response = client.post("$baseUrl/api/v1/$path") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(payload))
        }
        val body = bodyOf(response)
        checkStatus(statusOf(response), body)
        return json.decodeFromString(body)
    }
}
