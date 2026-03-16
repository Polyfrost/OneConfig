package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
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
    val configs = ConfigRegistry.configs
    val filtered = configs
        .filter { it.id != PREFERENCES_ID }
        .let { items ->
            if (category.configCategory == null) items
            else items.filter { it.category == category.configCategory }
        }

    LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(19.dp),
        horizontalArrangement = Arrangement.spacedBy(19.dp),
    ) {
        items(filtered) { ModCard(it) }
    }
}

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
                when (mod.source) {
                    ConfigSource.OC -> LocalNavController.wrapper.navigate(ModConfigRoute(mod.id))
                    else -> mod.onOpen?.invoke()
                }
            }
            .clip(theme.modCardShape)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(mod.icon, color = theme.textColor, modifier = Modifier.size(48.dp))
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
        Box(
            modifier = Modifier.fillMaxWidth().height(36.dp)
                .background(Accent)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(mod.title, color = LocalTheme.current.accentTextColor, fontSize = 16.sp)
        }
    }
}
