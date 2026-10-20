package org.unesa.agc.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.unesa.agc.AppGraph
import org.unesa.agc.ui.components.AgcGroupedSection
import org.unesa.agc.ui.components.AgcScreenHeader
import org.unesa.agc.ui.components.AgcListRow
import org.unesa.agc.ui.components.AgcRowSeparator
import org.unesa.agc.ui.components.clickableNoRipple
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun SettingsScreen(graph: AppGraph) {
    val colors = AgcThemeColors.current
    val session = graph.session.state
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        AgcScreenHeader(title = "Settings", scrollState = null)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = AgcSpacing.xs.dp, bottom = AgcSpacing.tabBarClearance.dp),
            verticalArrangement = Arrangement.spacedBy(AgcSpacing.sectionGap.dp),
        ) {
            item {
                AgcGroupedSection(
                    header = "ACCOUNT",
                    footer = "Signing out clears this device's session.",
                ) {
                    AgcListRow(
                        title = session.email ?: "Signed in",
                        subtitle = "Session on this device",
                    )
                    AgcRowSeparator()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = AgcSpacing.listRowMinHeight.dp)
                            .clickableNoRipple(onClick = { scope.launch { graph.signOut() } })
                            .padding(horizontal = AgcSpacing.cardInset.dp, vertical = AgcSpacing.md.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Sign out",
                            style = AgcTypographyLight.body,
                            color = colors.statusAlarm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                AgcGroupedSection(
                    header = "ABOUT",
                    footer = null,
                ) {
                    AgcListRow(
                        title = "Version",
                        subtitle = "1.0 (1)",
                    )
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = AgcSpacing.sm.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "AntiGasChamber",
                        style = AgcTypographyLight.footnote,
                        color = colors.secondaryLabel,
                    )
                    Spacer(Modifier.padding(top = AgcSpacing.xs.dp))
                    Text(
                        "LPG leak alerts",
                        style = AgcTypographyLight.caption1,
                        color = colors.secondaryLabel,
                    )
                }
            }
        }
    }
}
