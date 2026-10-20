package org.unesa.agc.push


interface PushTokenProvider {
    suspend fun currentToken(): String?
}

interface AlertPresenter {
    fun present(alertId: String, title: String, body: String, severity: String)
}

class PushRegistrar(
    private val provider: PushTokenProvider,
    private val register: suspend (token: String, platform: String) -> Unit,
) {
    suspend fun registerIfNeeded(platform: String) {
        val token = provider.currentToken() ?: return
        register(token, platform)
    }
}

class AlertDeduper {
    private val seen = LinkedHashSet<String>()

    fun shouldPresent(alertId: String): Boolean {
        if (seen.contains(alertId)) return false
        seen.add(alertId)
        if (seen.size > MAX_TRACKED) {
            seen.remove(seen.first())
        }
        return true
    }

    private companion object {
        const val MAX_TRACKED = 200
    }
}
