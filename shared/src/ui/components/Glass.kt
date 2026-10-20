package org.unesa.agc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import org.unesa.agc.ui.theme.AgcRadius
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors

@Composable
private fun Modifier.glassSurface(shape: Shape, input: HazeInput?): Modifier {
    val colors = AgcThemeColors.current
    val litTop = colors.glassFill.copy(alpha = (colors.glassFill.alpha * 1.15f).coerceAtMost(1f))
    val fill = Brush.verticalGradient(listOf(litTop, colors.glassFill))
    return this
        .shadow(
            elevation = 18.dp,
            shape = shape,
            ambientColor = colors.glassShadow,
            spotColor = colors.glassShadow,
        )
        .clip(shape)
        .then(
            if (input != null) {
                Modifier.hazeBlur(input = input, style = HazeMaterials.thin())
            } else {
                Modifier
            }
        )
        .background(fill)
        .border(1.dp, colors.glassStroke, shape)
}

@Composable
fun AgcGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AgcRadius.card.dp),
    input: HazeInput? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glassSurface(shape, input), content = content)
}

@Composable
fun AgcGlassPill(
    input: HazeInput? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .glassSurface(RoundedCornerShape(percent = 50), input)
            .padding(horizontal = AgcSpacing.lg.dp, vertical = AgcSpacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun AgcGlassCircle(
    modifier: Modifier = Modifier,
    input: HazeInput? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .glassSurface(CircleShape, input),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
