package org.unesa.agc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unesa.agc.data.TokenStore

data class SessionState(
    val signedIn: Boolean,
    val email: String?,
)

class SessionController(private val tokenStore: TokenStore) {
    var state by mutableStateOf(SessionState(signedIn = false, email = null))
        private set

    private val listeners = mutableListOf<(SessionState) -> Unit>()

    fun addListener(listener: (SessionState) -> Unit) {
        listeners.add(listener)
        notifyListeners()
    }

    private fun notifyListeners() {
        val snapshot = listeners.toList()
        CoroutineScope(Dispatchers.Main).launch {
            snapshot.forEach { it(state) }
        }
    }

    suspend fun restore() {
        val token = tokenStore.token() ?: return
        val email = EmailTokenStore.lastEmail ?: return
        state = SessionState(signedIn = true, email = email)
        notifyListeners()
    }

    suspend fun signIn(email: String) {
        EmailTokenStore.lastEmail = email
        state = SessionState(signedIn = true, email = email)
        notifyListeners()
    }

    suspend fun signOut() {
        tokenStore.clear()
        EmailTokenStore.lastEmail = null
        state = SessionState(signedIn = false, email = null)
        notifyListeners()
    }
}

object EmailTokenStore {
    var lastEmail: String? = null
}
