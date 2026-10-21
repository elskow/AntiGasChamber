package org.unesa.agc.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.patch
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.unesa.agc.data.AlertDto
import org.unesa.agc.data.ApiException
import org.unesa.agc.data.DeviceDto
import org.unesa.agc.data.LoginRequest
import org.unesa.agc.data.LoginResponse
import org.unesa.agc.data.PasswordChangeRequest
import org.unesa.agc.data.PagedAlerts
import org.unesa.agc.data.StatusResponse
import org.unesa.agc.data.UsersResponse
import org.unesa.agc.data.ProvisionDeviceRequest
import org.unesa.agc.data.ProvisionDeviceResponse
import org.unesa.agc.data.RegisterRequest
import org.unesa.agc.data.PushTokenRequest
import org.unesa.agc.data.ReadingDto
import org.unesa.agc.data.TokenStore
import org.unesa.agc.data.UnauthorizedException

internal fun statusOf(response: HttpResponse): Int = response.status.value

internal suspend fun bodyOf(response: HttpResponse): String = response.bodyAsText()

internal fun checkStatus(status: Int, body: String) {
    if (status == 401) throw UnauthorizedException()
    if (status >= 400) throw ApiException(status, parseProblem(body))
}

internal fun parseProblem(body: String): String {
    return try {
        val obj = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
        val err = obj["error"]
        if (err is JsonObject) {
            (err["message"]?.jsonPrimitive?.contentOrNull ?: body)
        } else {
            body
        }
    } catch (_: Exception) {
        body
    }
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

    suspend fun register(email: String, password: String, code: String): LoginResponse {
        val response = client.post("$baseUrl/api/v1/users") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest(email, password, code)))
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

    suspend fun readings(deviceId: String, limit: Int, before: Long? = null): List<ReadingDto> {
        val cursor = before?.let { "&before=$it" } ?: ""
        return get("devices/$deviceId/readings?limit=$limit$cursor")
    }

    suspend fun alerts(limit: Int, before: String? = null): PagedAlerts {
        val cursor = before?.let { "&before=$it" } ?: ""
        return get("alerts?limit=$limit$cursor")
    }

    suspend fun provisionDevice(deviceId: String): ProvisionDeviceResponse =
        post("devices", ProvisionDeviceRequest(deviceId))

    suspend fun deleteDevice(deviceId: String): StatusResponse =
        delete("devices/$deviceId")

    suspend fun rotateDevice(deviceId: String): ProvisionDeviceResponse =
        post("devices/$deviceId/rotate", Unit)

    suspend fun markAlertRead(alertId: String): StatusResponse =
        post("alerts/$alertId/read", Unit)

    suspend fun users(): UsersResponse = get("users")

    suspend fun deleteUser(userId: Long): StatusResponse = delete("users/$userId")

    suspend fun changeUserPassword(userId: Long, password: String): StatusResponse =
        patch("users/$userId/password", PasswordChangeRequest(password))

    private suspend inline fun <reified T> delete(path: String): T {
        val token = tokenStore.token() ?: throw UnauthorizedException()
        val response = client.delete("$baseUrl/api/v1/$path") {
            bearerAuth(token)
        }
        val body = bodyOf(response)
        checkStatus(statusOf(response), body)
        return json.decodeFromString(body)
    }

    private suspend inline fun <reified TReq, reified TRes> patch(path: String, payload: TReq): TRes {
        val token = tokenStore.token() ?: throw UnauthorizedException()
        val response = client.patch("$baseUrl/api/v1/$path") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(payload))
        }
        val body = bodyOf(response)
        checkStatus(statusOf(response), body)
        return json.decodeFromString(body)
    }

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
