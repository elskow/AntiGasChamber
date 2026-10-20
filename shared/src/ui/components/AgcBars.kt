package org.unesa.agc.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import org.unesa.agc.ui.theme.AgcRadius
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

class AgcTab(val label: String, val icon: ImageVector)

@Composable
fun AgcPlusButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AgcThemeColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f)
    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .background(colors.tint, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            style = AgcTypographyLight.title2,
            color = Color.White,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
fun AgcScreenHeader(
    title: String,
    scrollState: LazyListState?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AgcThemeColors.current
    val collapsed = scrollState != null && scrollState.canScrollForward && (
        scrollState.firstVisibleItemIndex > 0 ||
            scrollState.firstVisibleItemScrollOffset > 40
        )
    val collapseFraction by animateFloatAsState(if (collapsed) 1f else 0f)

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barHeight = statusBarPadding + (86 - 34 * collapseFraction).dp

    Box(modifier = modifier.fillMaxWidth()) {
        if (collapseFraction > 0.01f) {
            AgcGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight),
                shape = RectangleShape,
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.separator),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .padding(horizontal = AgcSpacing.screenEdge.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AnimatedContent(
                targetState = collapseFraction > 0.5f,
                transitionSpec = {
                    (fadeIn(tween(140)) togetherWith fadeOut(tween(140)))
                },
                label = "headerTitle",
            ) { isCollapsed ->
                if (isCollapsed) {
                    Text(
                        text = title,
                        style = AgcTypographyLight.headline,
                        color = colors.label,
                        modifier = Modifier
                            .padding(top = statusBarPadding)
                            .padding(bottom = 16.dp),
                    )
                } else {
                    Text(
                        text = title,
                        style = AgcTypographyLight.largeTitle,
                        color = colors.label,
                        modifier = Modifier
                            .padding(top = statusBarPadding)
                            .padding(bottom = 14.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (trailing != null) {
                Box(Modifier.padding(bottom = 14.dp)) {
                    trailing()
                }
            }
        }
    }
}

@Composable
fun AgcGlassTabBar(
    tabs: List<AgcTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    input: HazeInput?,
    modifier: Modifier = Modifier,
) {
    val colors = AgcThemeColors.current
    AgcGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(AgcSpacing.tabBarHeight.dp),
        shape = RoundedCornerShape(AgcRadius.bar.dp),
        input = input,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                val iconColor by animateColorAsState(
                    if (isSelected) colors.tint else colors.secondaryLabel,
                )
                val thumbColor by animateColorAsState(
                    if (isSelected) colors.glassHighlight else Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickableNoRipple { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .background(thumbColor, RoundedCornerShape(AgcRadius.sheet.dp)),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(24.dp),
                            tint = iconColor,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = AgcTypographyLight.caption2,
                            color = iconColor,
                        )
                    }
                }
            }
        }
    }
}
