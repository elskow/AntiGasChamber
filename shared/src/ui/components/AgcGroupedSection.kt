package org.unesa.agc.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.unesa.agc.ui.theme.AgcRadius
import org.unesa.agc.ui.theme.AgcSpacing
import org.unesa.agc.ui.theme.AgcThemeColors
import org.unesa.agc.ui.theme.AgcTypographyLight

@Composable
fun AgcGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AgcThemeColors.current
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        if (header != null) {
            Text(
                text = header,
                style = AgcTypographyLight.caption1,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(
                    start = (AgcSpacing.screenEdge + AgcSpacing.cardInset).dp,
                    bottom = 6.dp,
                ),
            )
        }
        AgcGlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AgcSpacing.screenEdge.dp),
            shape = RoundedCornerShape(AgcRadius.card.dp),
        ) {
            Column(content = content)
        }
        if (footer != null) {
            Text(
                text = footer,
                style = AgcTypographyLight.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(
                    start = (AgcSpacing.screenEdge + AgcSpacing.cardInset).dp,
                    top = 8.dp,
                ),
            )
        }
    }
}

@Composable
fun AgcScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        content = content,
    )
}
