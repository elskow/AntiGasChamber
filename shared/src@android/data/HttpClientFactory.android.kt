package org.unesa.agc.data

import android.net.http.HttpResponseCache.install
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual class HttpClientFactory {
    actual fun create(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
}
