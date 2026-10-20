package org.unesa.agc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

fun isAuthError(message: String?): Boolean {
    if (message == null) return false
    return message.contains("401") || message.contains("Unauthorized", ignoreCase = true)
}

@Composable
fun CenteredLoading(text: String, modifier: Modifier = Modifier) {
    val colors = AgcThemeColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = colors.tint)
        Spacer(Modifier.height(AgcSpacing.lg.dp))
        Text(text, style = AgcTypographyLight.subheadline, color = colors.secondaryLabel)
    }
}

@Composable
fun GlassStateCard(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val colors = AgcThemeColors.current
    Column(
        modifier.padding(bottom = AgcSpacing.tabBarClearance.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AgcGlassPanel(modifier = Modifier.padding(horizontal = AgcSpacing.screenEdge.dp)) {
            Column(Modifier.fillMaxWidth().padding(AgcSpacing.lg.dp)) {
                Text(title, style = AgcTypographyLight.headline, color = colors.label)
                Spacer(Modifier.height(AgcSpacing.xs.dp))
                Text(body, style = AgcTypographyLight.footnote, color = colors.secondaryLabel)
                Spacer(Modifier.height(AgcSpacing.md.dp))
                AgcGlassPill(modifier = Modifier.clickableNoRipple(onClick = onAction)) {
                    Text(actionLabel, style = AgcTypographyLight.callout, color = colors.tint)
                }
            }
        }
    }
}
