package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateDpAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.horizontalScroll
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.poly.SvgImage
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme

const val THEMES_ID = "themes.json"

@Composable
fun Themes(
    initialCategory: String? = null,
    searchQuery: String = "",
    onCategory: ((from: String?, to: String) -> Unit)? = null,
) {
    val revision = ConfigRegistry.revision
    val tree = ConfigRegistry.findTree(THEMES_ID)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = GUTTER.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            ThemeRegistry.registry.forEach {
                key(it.name) { ThemeCard(it) }
            }
        }
        if (tree == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No preferences available.", color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(revision, tree) {
                ConfigScreen(tree, initialCategory, pageKey = THEMES_ID, searchQuery = searchQuery, onCategory = onCategory)
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: UITheme) {
    val interactionSource = rememberInteractionSource()
    val isSelected = theme == ThemeRegistry.activeTheme

    val borderColor = animateColorAsState(
        if (isSelected) Accent else LocalTheme.current.borderColor
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 3.dp else 1.dp
    )

    Column(
        modifier = Modifier
            .background(LocalTheme.current.modCardBackground, LocalTheme.current.backgroundShape)
            .border(borderWidth, borderColor, LocalTheme.current.backgroundShape)
            .clickable(interactionSource, indication = null) {
                // Use clickable because onClick always plays CLICK. ThemeRegistry.activate chooses CLICK or OPEN
                ThemeRegistry.activate(theme)
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SvgImage(
            "/assets/oneconfig/images/${theme.previewImage}.svg",
            168f,
            108f,
            LocalDensity.current.density,
        )
        Text(theme.name, color = LocalTheme.current.textColor)
    }
}
