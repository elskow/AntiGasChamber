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
import org.unesa.agc.ui.components.AgcGlassCircle
import org.unesa.agc.ui.components.AgcPrimaryButton
import org.unesa.agc.ui.components.AgcTextField
import org.unesa.agc.ui.icons.AppTabIcons
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun RegisterScreen(
    error: String?,
    submitting: Boolean,
    onRegister: (email: String, password: String, code: String) -> Unit,
    onBackToLogin: () -> Unit,
) {
    val colors = AgcThemeColors.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val valid = email.contains("@") && password.length >= 8 && code.isNotBlank()

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
            "Create account",
            style = AgcTypographyLight.title1,
            color = colors.label,
        )
        Spacer(Modifier.height(AgcSpacing.xs.dp))
        Text(
            "Ask your administrator for the provisioning code.",
            style = AgcTypographyLight.subheadline,
            color = colors.secondaryLabel,
        )
        Spacer(Modifier.height(AgcSpacing.sectionGap.dp))
        AgcTextField(value = email, onValueChange = { email = it }, label = "Email")
        Spacer(Modifier.height(AgcSpacing.md.dp))
        AgcTextField(value = password, onValueChange = { password = it }, label = "Password (min 8 characters)", secure = true)
        Spacer(Modifier.height(AgcSpacing.md.dp))
        AgcTextField(value = code, onValueChange = { code = it }, label = "Provisioning code", secure = true)
        if (error != null) {
            Spacer(Modifier.height(AgcSpacing.md.dp))
            Text(
                when {
                    error.startsWith("403:") -> "Wrong provisioning code."
                    error.startsWith("409:") -> "An account with this email already exists."
                    error.startsWith("400:") -> "Check your email and password (min 8 characters)."
                    else -> error?.substringAfter(':') ?: error
                },
                style = AgcTypographyLight.footnote,
                color = colors.statusAlarm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(AgcSpacing.lg.dp))
        AgcPrimaryButton(
            label = "Create account",
            onClick = { if (valid) onRegister(email.trim(), password, code.trim()) },
            enabled = valid,
            loading = submitting,
        )
        Spacer(Modifier.height(AgcSpacing.sm.dp))
        Text(
            "Already have an account? Sign in instead.",
            style = AgcTypographyLight.caption1,
            color = colors.tint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AgcSpacing.sm.dp)
                .let { it },
        )
        Spacer(Modifier.height(AgcSpacing.sm.dp))
        AgcPrimaryButton(
            label = "Back to sign in",
            onClick = onBackToLogin,
            enabled = true,
            loading = false,
        )
    }
}
