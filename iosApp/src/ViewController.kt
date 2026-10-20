package org.unesa.agc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unesa.agc.ui.screens.fleet.AlertsScreen
import org.unesa.agc.ui.screens.fleet.FleetScreen
import org.unesa.agc.ui.screens.auth.LoginScreen
import org.unesa.agc.ui.screens.settings.SettingsScreen
import org.unesa.agc.ui.theme.AgcTheme

fun LoginViewController() = ComposeUIViewController {
    AgcTheme {
        val graph = remember { AppGraphHolder.graph }
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
}

fun FleetViewController() = ComposeUIViewController {
    AgcTheme {
        FleetScreen(AppGraphHolder.graph.fleetViewModel)
    }
}

fun AlertsViewController() = ComposeUIViewController {
    AgcTheme {
        AlertsScreen(AppGraphHolder.graph.fleetViewModel)
    }
}

fun SettingsViewController() = ComposeUIViewController {
    AgcTheme {
        SettingsScreen(AppGraphHolder.graph)
    }
}

fun watchSessionSignedIn(callback: (Boolean) -> Unit) {
    AppGraphHolder.graph.session.addListener { session ->
        callback(session.signedIn)
    }
}
