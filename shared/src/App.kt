package org.unesa.agc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.unesa.agc.data.AgcApi
import org.unesa.agc.data.HttpClientFactory
import org.unesa.agc.data.TokenStore
import org.unesa.agc.ui.screens.fleet.AppHomeScreen
import org.unesa.agc.data.remote.LiveSocket
import org.unesa.agc.ui.screens.fleet.FleetViewModel
import org.unesa.agc.push.AlertCenter
import org.unesa.agc.push.AlertPresenter
import org.unesa.agc.ui.screens.auth.LoginScreen
import org.unesa.agc.ui.theme.AgcTheme

private object InMemoryTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun token(): String? = token
    override suspend fun save(token: String) { this.token = token }
    override suspend fun clear() { this.token = null }
}

private object SilentAlertPresenter : AlertPresenter {
    override fun present(alertId: String, title: String, body: String, severity: String) {}
}

class AppGraph(private val baseUrl: String) {
    val tokenStore: TokenStore = InMemoryTokenStore
    val session = SessionController(tokenStore)
    val api = AgcApi(baseUrl, tokenStore, HttpClientFactory())
    val alertCenter = AlertCenter(presenter = SilentAlertPresenter)

    val fleetViewModel: FleetViewModel by lazy {
        FleetViewModel(api, LiveSocket(baseUrl, tokenStore), alertCenter)
    }

    suspend fun signIn(email: String, password: String) {
        val response = api.login(email, password)
        session.signIn(email)
    }

    suspend fun signOut() {
        alertCenter.dismiss()
        session.signOut()
    }
}

object AppGraphHolder {
    val graph: AppGraph by lazy { AppGraph(BackendBaseUrl) }
}

@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    AgcTheme {
        val graph = remember { AppGraphHolder.graph }
        val session = graph.session.state
        LaunchedEffect(Unit) { graph.session.restore() }
        if (session.signedIn) {
            AppHomeScreen(graph)
        } else {
            LoginGate(graph)
        }
    }
}

@Composable
private fun LoginGate(graph: AppGraph) {
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LoginScreen(
        error = error,
        submitting = submitting,
        onLogin = { email, password ->
            submitting = true
            error = null
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    graph.signIn(email, password)
                } catch (e: Exception) {
                    error = e.message ?: "login failed"
                } finally {
                    submitting = false
                }
            }
        },
    )
}
