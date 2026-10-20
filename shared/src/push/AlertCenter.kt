package org.unesa.agc.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlertSeverity { WARNING, ALARM, OFFLINE, UNKNOWN;

    companion object {
        fun from(raw: String): AlertSeverity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

data class LiveAlert(
    val id: String,
    val deviceId: String,
    val severity: AlertSeverity,
    val peakPpm: Double,
)

class AlertCenter(
    private val deduper: AlertDeduper = AlertDeduper(),
    private val presenter: AlertPresenter,
) {
    var presenterOverride: AlertPresenter? = null

    private val _current = MutableStateFlow<LiveAlert?>(null)
    val current: StateFlow<LiveAlert?> = _current.asStateFlow()

    fun deliver(alert: LiveAlert): Boolean {
        if (!deduper.shouldPresent(alert.id)) return false
        _current.value = alert
        (presenterOverride ?: presenter).present(
            alertId = alert.id,
            title = if (alert.severity == AlertSeverity.ALARM) "GAS LEAK ALARM" else "Gas warning",
            body = "Node ${alert.deviceId}: peak ${alert.peakPpm.toInt()} ppm",
            severity = alert.severity.name,
        )
        return true
    }

    fun dismiss() {
        _current.value = null
    }
}
