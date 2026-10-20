package org.unesa.agc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AgcColors(
    val groupedBackground: Color,
    val cardBackground: Color,
    val elevatedBackground: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val tint: Color,
    val statusOk: Color,
    val statusWarning: Color,
    val statusAlarm: Color,
    val statusOffline: Color,
    val alarmBackground: Color,
    val glassFill: Color,
    val glassStroke: Color,
    val glassShadow: Color,
    val glassHighlight: Color,
)

private val LightColors = AgcColors(
    groupedBackground = Color(0xFFF2F2F7),
    cardBackground = Color(0xFFFFFFFF),
    elevatedBackground = Color(0xFFF7F7FA),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF6C6C70),
    tertiaryLabel = Color(0xFFAEAEB2),
    separator = Color(0xFFE5E5EA),
    tint = Color(0xFF007AFF),
    statusOk = Color(0xFF34C759),
    statusWarning = Color(0xFFFF9500),
    statusAlarm = Color(0xFFFF3B30),
    statusOffline = Color(0xFF8E8E93),
    alarmBackground = Color(0xFF1C0A0A),
    glassFill = Color.White.copy(alpha = 0.60f),
    glassStroke = Color.Black.copy(alpha = 0.05f),
    glassShadow = Color.Black.copy(alpha = 0.10f),
    glassHighlight = Color.Black.copy(alpha = 0.06f),
)

private val DarkColors = AgcColors(
    groupedBackground = Color(0xFF000000),
    cardBackground = Color(0xFF1C1C1E),
    elevatedBackground = Color(0xFF2C2C2E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF55555A),
    separator = Color(0xFF38383A),
    tint = Color(0xFF0A84FF),
    statusOk = Color(0xFF30D158),
    statusWarning = Color(0xFFFF9F0A),
    statusAlarm = Color(0xFFFF453A),
    statusOffline = Color(0xFF8E8E93),
    alarmBackground = Color(0xFF1C0A0A),
    glassFill = Color.White.copy(alpha = 0.07f),
    glassStroke = Color.White.copy(alpha = 0.16f),
    glassShadow = Color.Black.copy(alpha = 0.55f),
    glassHighlight = Color.White.copy(alpha = 0.12f),
)

val LocalAgcColors = staticCompositionLocalOf { LightColors }

@Composable
fun AgcTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    androidx.compose.runtime.CompositionLocalProvider(LocalAgcColors provides colors) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = if (darkTheme) androidx.compose.material3.darkColorScheme()
            else androidx.compose.material3.lightColorScheme(),
            content = content,
        )
    }
}

object AgcThemeColors {
    val current: AgcColors @Composable get() = LocalAgcColors.current
}
