package org.unesa.agc.ui.screens.fleet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.unesa.agc.push.AlertCenter
import org.unesa.agc.data.AgcApi
import org.unesa.agc.data.AlertDto
import org.unesa.agc.data.DeviceDto
import org.unesa.agc.data.ProvisionDeviceResponse
import org.unesa.agc.data.remote.LiveSocket
import org.unesa.agc.push.AlertSeverity
import org.unesa.agc.push.LiveAlert

class FleetViewModel(
    private val api: AgcApi,
    private val live: LiveSocket,
    private val alertCenter: AlertCenter? = null,
) : ViewModel() {

    var fleet by mutableStateOf<List<DeviceDto>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var provisioning by mutableStateOf(false)
        private set
    var provisioned by mutableStateOf<ProvisionDeviceResponse?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var alerts by mutableStateOf<List<AlertDto>>(emptyList())
        private set

    init {
        live.onAlert = { alertId, deviceId, severity, peakPpm ->
            updateDevice(deviceId) { it.copy(status = severity) }
            alertCenter?.deliver(
                LiveAlert(
                    id = alertId,
                    deviceId = deviceId,
                    severity = AlertSeverity.from(severity),
                    peakPpm = peakPpm,
                ),
            )
            refreshAlerts()
        }
        live.onReading = { deviceId, ppm ->
            updateDevice(deviceId) { it.copy(lastPpm = ppm) }
        }
        live.start(viewModelScope)
    }

    override fun onCleared() {
        live.stop()
    }

    private fun updateDevice(deviceId: String, transform: (DeviceDto) -> DeviceDto) {
        fleet = fleet.map { if (it.deviceId == deviceId) transform(it) else it }
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                fleet = api.devices()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun refreshAlerts() {
        viewModelScope.launch {
            try {
                alerts = api.alerts(limit = 50)
            } catch (_: Exception) {
            }
        }
    }

    fun provisionDevice(deviceId: String, onDone: () -> Unit = {}) {
        if (deviceId.isBlank()) return
        viewModelScope.launch {
            provisioning = true
            actionError = null
            try {
                provisioned = api.provisionDevice(deviceId.trim())
                fleet = api.devices()
                onDone()
            } catch (e: Exception) {
                actionError = e.message
            } finally {
                provisioning = false
            }
        }
    }

    fun dismissSecret() {
        provisioned = null
    }
}
