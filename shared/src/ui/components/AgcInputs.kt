package org.unesa.agc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun AgcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    secure: Boolean = false,
) {
    val colors = AgcThemeColors.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label, style = AgcTypographyLight.footnote, color = colors.secondaryLabel)
        },
        singleLine = true,
        visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.elevatedBackground,
            unfocusedContainerColor = colors.elevatedBackground,
            focusedTextColor = colors.label,
            unfocusedTextColor = colors.label,
            cursorColor = colors.tint,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = colors.secondaryLabel,
            unfocusedLabelColor = colors.secondaryLabel,
        ),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
fun AgcPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = AgcThemeColors.current
    val fill = if (enabled && !loading) colors.tint else colors.tint.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .background(fill, RoundedCornerShape(percent = 50))
            .clickableNoRipple(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.height(22.dp).width(22.dp),
            )
        } else {
            Text(
                label,
                style = AgcTypographyLight.headline,
                color = Color.White,
            )
        }
    }
}
