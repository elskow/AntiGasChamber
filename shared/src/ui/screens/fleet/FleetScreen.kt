package org.unesa.agc.ui.screens.fleet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.unesa.agc.data.DeviceDto
import org.unesa.agc.data.ProvisionDeviceResponse
import org.unesa.agc.ui.components.AgcGlassPanel
import org.unesa.agc.ui.components.AgcGroupedSection
import org.unesa.agc.ui.components.AgcPlusButton
import org.unesa.agc.ui.components.AgcScreenHeader
import org.unesa.agc.ui.components.AgcListRow
import org.unesa.agc.ui.components.AgcPrimaryButton
import org.unesa.agc.ui.components.AgcRowSeparator
import org.unesa.agc.ui.components.AgcStatus
import org.unesa.agc.ui.components.AgcStatusChip
import org.unesa.agc.ui.components.AgcTextField
import org.unesa.agc.ui.components.GlassStateCard
import org.unesa.agc.ui.components.clickableNoRipple
import org.unesa.agc.ui.theme.AgcRadius
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetScreen(viewModel: FleetViewModel) {
    val colors = AgcThemeColors.current
    var showProvision by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(Modifier.fillMaxSize()) {
        AgcScreenHeader(
            title = "Fleet",
            scrollState = listState,
            trailing = {
                AgcPlusButton(onClick = { showProvision = true })
            },
        )
        when {
            viewModel.error != null -> GlassStateCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                title = "Couldn't load fleet",
                body = viewModel.error ?: "Something went wrong.",
                actionLabel = "Try again",
                onAction = { viewModel.refresh() },
            )
            viewModel.loading && viewModel.fleet.isEmpty() -> CenteredLoading(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = AgcSpacing.sm.dp, bottom = AgcSpacing.tabBarClearance.dp),
                verticalArrangement = Arrangement.spacedBy(AgcSpacing.sectionGap.dp),
            ) {
                item { FleetSection(viewModel.fleet) }
                viewModel.actionError?.let { message ->
                    item {
                        Text(
                            message,
                            style = AgcTypographyLight.footnote,
                            color = colors.statusAlarm,
                            modifier = Modifier.padding(horizontal = AgcSpacing.screenEdge.dp),
                        )
                    }
                }
            }
        }
    }

    if (showProvision) {
        ModalBottomSheet(
            onDismissRequest = { showProvision = false },
            containerColor = colors.cardBackground,
        ) {
            ProvisionForm(
                viewModel = viewModel,
                onDone = {
                    showProvision = false
                    viewModel.refresh()
                },
            )
        }
    }

    viewModel.provisioned?.let { response ->
        SecretDialog(response) { viewModel.dismissSecret() }
    }
}

@Composable
private fun FleetSection(fleet: List<DeviceDto>) {
    val colors = AgcThemeColors.current
    AgcGroupedSection(
        header = "FLEET",
        footer = "Every LPG sensor node in the system.",
    ) {
        if (fleet.isEmpty()) {
            Text(
                "No nodes provisioned yet. Add one with + Node.",
                style = AgcTypographyLight.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(AgcSpacing.cardInset.dp),
            )
        }
        fleet.forEachIndexed { index, node ->
            AgcListRow(
                title = node.deviceId,
                subtitle = buildString {
                    append("last seen ${node.lastSeen ?: "never"}")
                    node.lastPpm?.let { append(" · ${it.toInt()} ppm") }
                },
                trailing = {
                    AgcStatusChip(
                        when (node.status) {
                            "online" -> AgcStatus.OK
                            "alarm" -> AgcStatus.ALARM
                            else -> AgcStatus.OFFLINE
                        }
                    )
                },
            )
            if (index < fleet.lastIndex) AgcRowSeparator()
        }
    }
}

@Composable
private fun ProvisionForm(viewModel: FleetViewModel, onDone: () -> Unit) {
    val colors = AgcThemeColors.current
    var deviceId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgcSpacing.lg.dp)
            .padding(bottom = AgcSpacing.xl.dp),
    ) {
        Text(
            "Provision node",
            style = AgcTypographyLight.title2,
            color = colors.label,
        )
        Spacer(Modifier.height(AgcSpacing.md.dp))
        AgcTextField(
            value = deviceId,
            onValueChange = { deviceId = it },
            label = "Device ID (e.g. AGC-003)",
        )
        Spacer(Modifier.height(AgcSpacing.sm.dp))
        Spacer(Modifier.height(AgcSpacing.lg.dp))
        AgcPrimaryButton(
            label = "Provision",
            onClick = {
                viewModel.provisionDevice(deviceId, onDone)
            },
            enabled = deviceId.isNotBlank(),
            loading = viewModel.provisioning,
        )
    }
}

@Composable
private fun SecretDialog(response: ProvisionDeviceResponse, onDismiss: () -> Unit) {
    val colors = AgcThemeColors.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Text(
                "Done",
                style = AgcTypographyLight.headline,
                color = colors.tint,
                modifier = Modifier.clickableNoRipple(onClick = onDismiss),
            )
        },
        title = {
            Text(
                "${response.deviceId} provisioned",
                style = AgcTypographyLight.headline,
                color = colors.label,
            )
        },
        text = {
            Column {
                Text(
                    "Give these credentials to the IoT team for this node:",
                    style = AgcTypographyLight.footnote,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(AgcSpacing.md.dp))
                SecretRow("MQTT username", response.mqtt.username)
                SecretRow("MQTT password", response.secret)
                SecretRow("Topic", response.mqtt.topic)
            }
        },
        containerColor = colors.cardBackground,
    )
}

@Composable
private fun SecretRow(label: String, value: String) {
    val colors = AgcThemeColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = AgcSpacing.xs.dp)) {
        Text(
            label,
            style = AgcTypographyLight.caption1,
            color = colors.secondaryLabel,
        )
        Text(
            value,
            style = AgcTypographyLight.monoValue.copy(fontWeight = FontWeight.SemiBold, fontSize = AgcTypographyLight.footnote.fontSize),
            color = colors.label,
        )
    }
}

@Composable
private fun CenteredLoading(modifier: Modifier = Modifier) {
    val colors = AgcThemeColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(color = colors.tint)
        Spacer(Modifier.height(AgcSpacing.lg.dp))
        Text(
            "Loading fleet…",
            style = AgcTypographyLight.subheadline,
            color = colors.secondaryLabel,
        )
    }
}
