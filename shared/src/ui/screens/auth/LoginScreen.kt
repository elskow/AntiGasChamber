package org.unesa.agc.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.unesa.agc.DevCredentials
import org.unesa.agc.ui.components.AgcGlassCircle
import org.unesa.agc.ui.components.AgcPrimaryButton
import org.unesa.agc.ui.components.AgcTextField
import org.unesa.agc.ui.icons.AppTabIcons
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun LoginScreen(
    error: String?,
    submitting: Boolean,
    onLogin: (String, String) -> Unit,
) {
    val colors = AgcThemeColors.current
    var email by remember { mutableStateOf(DevCredentials.email) }
    var password by remember { mutableStateOf(DevCredentials.password) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = AgcSpacing.screenEdge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.9f))
        AgcGlassCircle(modifier = Modifier.size(76.dp)) {
            Icon(
                imageVector = AppTabIcons.nodes,
                contentDescription = null,
                tint = colors.tint,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(AgcSpacing.lg.dp))
        Text(
            "AntiGasChamber",
            style = AgcTypographyLight.title1,
            color = colors.label,
        )
        Spacer(Modifier.height(AgcSpacing.xs.dp))
        Text(
            "LPG leak alerts",
            style = AgcTypographyLight.subheadline,
            color = colors.secondaryLabel,
        )
        Spacer(Modifier.height(AgcSpacing.sectionGap.dp))
        AgcTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
        )
        Spacer(Modifier.height(AgcSpacing.md.dp))
        AgcTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            secure = true,
        )
        if (error != null) {
            Spacer(Modifier.height(AgcSpacing.md.dp))
            Text(
                if (error.contains("401") || error.contains("credential", ignoreCase = true)) {
                    "Wrong email or password."
                } else {
                    error
                },
                style = AgcTypographyLight.footnote,
                color = colors.statusAlarm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(AgcSpacing.lg.dp))
        AgcPrimaryButton(
            label = "Sign in",
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) onLogin(email.trim(), password)
            },
            enabled = email.isNotBlank() && password.isNotBlank(),
            loading = submitting,
        )
        Spacer(Modifier.height(AgcSpacing.sm.dp))
        Text(
            "Sign in with your AntiGasChamber account.",
            style = AgcTypographyLight.caption1,
            color = colors.secondaryLabel,
        )
        Spacer(Modifier.weight(1.1f))
    }
}
