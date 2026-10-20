package org.unesa.agc.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Radio
import compose.icons.feathericons.Settings

data class TabIconSet(
    val nodes: ImageVector,
    val history: ImageVector,
    val settings: ImageVector,
)

val AppTabIcons = TabIconSet(
    nodes = FeatherIcons.Radio,
    history = FeatherIcons.Clock,
    settings = FeatherIcons.Settings,
)
