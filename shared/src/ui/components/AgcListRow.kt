package org.unesa.agc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

enum class AgcStatus { OK, WARNING, ALARM, OFFLINE }

@Composable
fun AgcStatusChip(status: AgcStatus, modifier: Modifier = Modifier, labelOverride: String? = null) {
    val colors = AgcThemeColors.current
    val color = when (status) {
        AgcStatus.OK -> colors.statusOk
        AgcStatus.WARNING -> colors.statusWarning
        AgcStatus.ALARM -> colors.statusAlarm
        AgcStatus.OFFLINE -> colors.statusOffline
    }
    val label = labelOverride ?: when (status) {
        AgcStatus.OK -> "OK"
        AgcStatus.WARNING -> "WARNING"
        AgcStatus.ALARM -> "ALARM"
        AgcStatus.OFFLINE -> "OFFLINE"
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(percent = 50))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label.uppercase(),
            style = AgcTypographyLight.caption2.copy(
                fontWeight = FontWeight.SemiBold,
                color = color,
            ),
        )
    }
}

@Composable
fun AgcListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AgcThemeColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AgcSpacing.listRowMinHeight.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(horizontal = AgcSpacing.cardInset.dp, vertical = AgcSpacing.md.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(AgcSpacing.lg.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AgcTypographyLight.headline, color = colors.label)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = AgcTypographyLight.footnote,
                    color = colors.secondaryLabel,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(AgcSpacing.sm.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(AgcSpacing.sm.dp))
            Canvas(Modifier.size(width = 8.dp, height = 14.dp)) {
                val chevron = Path()
                chevron.moveTo(0f, 0f)
                chevron.lineTo(size.width, size.height / 2f)
                chevron.lineTo(0f, size.height)
                drawPath(
                    chevron,
                    color = colors.tertiaryLabel,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

@Composable
fun AgcRowSeparator(modifier: Modifier = Modifier) {
    val colors = AgcThemeColors.current
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = AgcSpacing.cardInset.dp)
            .height(0.5.dp)
            .background(colors.separator),
    )
}
