package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class ModCategory(
    val title: String,
    val icon: String?,
    val configCategory: Config.Category?
) {
    All("All", null, null),
    Combat("Combat", "combat", Config.Category.COMBAT),
    QoL("Quality of Life", "qol", Config.Category.QOL),
    Hypixel("Hypixel", "hypixel", Config.Category.HYPIXEL),
    Other("Other", null, Config.Category.OTHER);
}

@Composable
fun Mods() {
    var activeCategory by remember { mutableStateOf(ModCategory.All) }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModCategory.entries.forEach {
                Chip(
                    label = it.title,
                    selected = activeCategory == it,
                    icon = it.icon,
                    onClick = { activeCategory = it }
                )
            }
        }
        ModsGrid(activeCategory)
    }
}

@Composable
fun ColumnScope.ModsGrid(category: ModCategory) {
    val filtered = ConfigRegistry.modCardConfigs
        .let { items ->
            if (category.configCategory == null) items
            else items.filter { it.category == category.configCategory }
        }

    val gridState = rememberLazyGridState()
    Box(modifier = Modifier.weight(1f)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(19.dp),
            horizontalArrangement = Arrangement.spacedBy(19.dp),
            modifier = Modifier.padding(end = 8.dp),
        ) {
            items(filtered) { ModCard(it) }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(gridState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

private val ModCardFooterHeight = 36.dp

@Composable
fun ModCard(mod: ConfigData) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current

    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(theme.borderColor, theme.borderColor.copy(0f))
                ), theme.modCardShape
            )
            .onClick(interactionSource) {
                val onOpen = mod.onOpen
                when {
                    onOpen != null -> onOpen()
                    mod.source == ConfigSource.OC -> LocalNavController.wrapper.navigate(ModConfigRoute(mod.id))
                }
            }
            .clip(theme.modCardShape)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val icon = mod.icon?.takeIf(::canRenderIcon)
                if (icon != null) {
                    Icon(icon, color = theme.textColor, modifier = Modifier.size(48.dp))
                } else {
                    Text(
                        mod.title.asRenderText(),
                        color = theme.textColor,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ModCardFooterHeight)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mod.title,
                    color = LocalTheme.current.accentTextColor,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        if (LocalTheme.current.shadowEnabled) {
            val vignetteColor = theme.textColor
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val gradient = Brush.radialGradient(
                        colors = listOf(
                            vignetteColor.copy(alpha = 0f),
                            vignetteColor.copy(alpha = 0.04f),
                            vignetteColor.copy(alpha = 0.08f)
                        ),
                        center = size.center,
                        radius = size.minDimension * 0.9f
                    )
                    onDrawBehind { drawRect(gradient) }
                }
            )
            val gradient = Brush.verticalGradient(
                0f to Accent.copy(0f),
                0.4f to Accent.copy(0.2f),
                1f to Accent.copy(0.4f),
            )

            Box(
                Modifier.align(Alignment.BottomCenter).height(50.dp).fillMaxWidth().drawWithCache {
                    onDrawBehind { drawRect(gradient, size = Size(size.width, 50f)) }
                }
            )
        }
    }
}
