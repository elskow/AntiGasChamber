package org.unesa.agc.ui.screens.fleet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.unesa.agc.AppGraph
import org.unesa.agc.data.AlertDto
import org.unesa.agc.ui.components.AgcGlassPanel
import org.unesa.agc.ui.components.AgcGlassTabBar
import org.unesa.agc.ui.components.AgcScreenHeader
import org.unesa.agc.ui.components.AgcStatus
import org.unesa.agc.ui.components.AgcStatusChip
import org.unesa.agc.ui.components.AgcTab
import org.unesa.agc.ui.icons.AppTabIcons
import org.unesa.agc.ui.components.GlassStateCard
import org.unesa.agc.ui.screens.settings.SettingsScreen
import org.unesa.agc.ui.theme.AgcRadius
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun AppHomeScreen(graph: AppGraph) {
    var selectedTab by remember { mutableStateOf(0) }
    val hazeState = rememberHazeState()
    val colors = AgcThemeColors.current

    Box(Modifier.fillMaxSize().background(colors.groupedBackground)) {
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            when (selectedTab) {
                0 -> FleetScreen(graph.fleetViewModel)
                1 -> AlertsScreen(graph.fleetViewModel)
                else -> SettingsScreen(graph)
            }
        }
        AgcGlassTabBar(
            tabs = listOf(
                AgcTab("Fleet", AppTabIcons.nodes),
                AgcTab("Alerts", AppTabIcons.history),
                AgcTab("Settings", AppTabIcons.settings),
            ),
            selected = selectedTab,
            onSelect = { selectedTab = it },
            input = HazeInput.Backdrop(hazeState),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = AgcSpacing.sm.dp),
        )
    }
}

private fun severityOf(raw: String): AgcStatus = when (raw) {
    "alarm" -> AgcStatus.ALARM
    "warning" -> AgcStatus.WARNING
    else -> AgcStatus.OFFLINE
}

@Composable
fun AlertsScreen(viewModel: FleetViewModel) {
    val colors = AgcThemeColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.refreshAlerts() }

    Column(Modifier.fillMaxSize()) {
        AgcScreenHeader(title = "Alerts", scrollState = listState)
        when {
            viewModel.alerts.isEmpty() -> GlassStateCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                title = "No alerts yet",
                body = "Alerts will appear here when a node detects LPG.",
                actionLabel = "Refresh",
                onAction = { viewModel.refreshAlerts() },
            )
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = AgcSpacing.sm.dp, bottom = AgcSpacing.tabBarClearance.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(viewModel.alerts.size) { index ->
                    AlertCard(viewModel.alerts[index])
                }
                item {
                    Text(
                        "Showing the most recent ${viewModel.alerts.size} alerts.",
                        style = AgcTypographyLight.footnote,
                        color = colors.secondaryLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AgcSpacing.screenEdge.dp)
                            .padding(top = AgcSpacing.sm.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertDto) {
    val colors = AgcThemeColors.current
    val status = severityOf(alert.severity)
    val barColor = when (status) {
        AgcStatus.ALARM -> colors.statusAlarm
        AgcStatus.WARNING -> colors.statusWarning
        else -> colors.statusOffline
    }
    AgcGlassPanel(modifier = Modifier.padding(horizontal = AgcSpacing.screenEdge.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(barColor))
            Row(
                Modifier
                    .weight(1f)
                    .padding(horizontal = AgcSpacing.cardInset.dp, vertical = AgcSpacing.md.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${alert.severity.replaceFirstChar { it.uppercase() }} at ${alert.deviceId}",
                        style = AgcTypographyLight.headline,
                        color = colors.label,
                    )
                    Spacer(Modifier.height(AgcSpacing.xs.dp))
                    Text(
                        "peak ${alert.peakPpm.toInt()} ppm · ${alert.triggeredAt}",
                        style = AgcTypographyLight.footnote,
                        color = colors.secondaryLabel,
                    )
                }
                Spacer(Modifier.width(AgcSpacing.md.dp))
                AgcStatusChip(status)
            }
        }
    }
}
