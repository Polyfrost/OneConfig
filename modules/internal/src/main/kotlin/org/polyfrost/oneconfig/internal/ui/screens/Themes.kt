package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.glassHover
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.keepSearchFocus
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.wheelScroll
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

const val THEMES_ID = "themes.json"

private val ThemeCardGap = 20.dp

private val ThemeCardMinWidth = 290.dp

private const val DARK_SUFFIX = " Dark"

private const val LIGHT_SUFFIX = " Light"

private val UITheme.family: String get() = name.removeSuffix(DARK_SUFFIX).removeSuffix(LIGHT_SUFFIX)

private val UITheme.isLight: Boolean get() = name.endsWith(LIGHT_SUFFIX)

@Composable
fun Themes() {
    val revision = ConfigRegistry.revision
    val tree = ConfigRegistry.findTree(THEMES_ID)
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val families = ThemeRegistry.registry.groupBy { it.family }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val count = families.size.coerceAtLeast(1)
            val available = maxWidth - PageGutter * 2
            val cardWidth = maxOf(ThemeCardMinWidth, (available - ThemeCardGap * (count - 1)) / count)
            val rowState = rememberLazyListState()
            LazyRow(
                state = rowState,
                modifier = Modifier.wheelScroll(rowState, Orientation.Horizontal),
                horizontalArrangement = Arrangement.spacedBy(ThemeCardGap),
                contentPadding = PaddingValues(horizontal = PageGutter),
            ) {
                items(families.entries.toList(), key = { it.key }) { (family, variants) ->
                    ThemeCard(family, variants, Modifier.width(cardWidth))
                }
            }
        }
        if (tree == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No preferences available.", color = LocalTheme.current.textColorSecondary)
            }
            return
        }
        key(revision, tree) {
            ConfigScreen(tree, pageKey = THEMES_ID)
        }
    }
}

@Composable
private fun ThemeCard(family: String, variants: List<UITheme>, modifier: Modifier) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val active = variants.firstOrNull { it.name == theme.name }
    var picked by remember(family) { mutableStateOf(active ?: variants.firstOrNull { it.isLight == theme.isLight } ?: variants.first()) }
    LaunchedEffect(active) {
        if (active != null) picked = active
    }
    val shown = active ?: picked
    val isSelected = active != null

    val borderColor by animateColorAsState(
        if (isSelected) Accent else theme.borderColor
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 3.dp else 1.dp
    )

    Row(
        modifier = modifier
            .hoverGlow(theme.backgroundShape)
            .background(theme.modCardBackground, theme.backgroundShape)
            .border(borderWidth, borderColor, theme.backgroundShape)
            .keepSearchFocus()
            .clickable(interactionSource, indication = null) {
                // Use clickable because onClick always plays CLICK. ThemeRegistry.activate chooses CLICK or OPEN
                ThemeRegistry.activate(shown)
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource("/assets/oneconfig/images/${shown.previewImage}.svg"),
            contentDescription = null,
            modifier = Modifier.size(140.dp, 90.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                family,
                color = theme.textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (variants.size > 1) {
                VariantToggle(variants, shown) { variant ->
                    picked = variant
                    if (isSelected) ThemeRegistry.activate(variant) else UiSounds.play(UiSoundEvent.CLICK)
                }
            }
        }
    }
}

@Composable
private fun VariantToggle(variants: List<UITheme>, shown: UITheme, onPick: (UITheme) -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .height(30.dp)
            .liquidGlass(theme.buttonShape)
            .background(theme.componentBackground, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        variants.forEach { variant ->
            VariantSegment(variant, variant == shown) { onPick(variant) }
        }
    }
}

@Composable
private fun VariantSegment(variant: UITheme, selected: Boolean, onPick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val shape = theme.buttonShape.concentric(3.dp)
    Box(
        Modifier
            .fillMaxHeight()
            .width(32.dp)
            .glassHover(shape, pull = 0.75.dp)
            .background(if (selected) theme.textColor.copy(alpha = 0.1f) else Color.Transparent, shape)
            .then(if (selected) Modifier else Modifier.keepSearchFocus())
            .clickable(interactionSource, indication = null, enabled = !selected, onClick = onPick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (variant.isLight) "sun" else "moon",
            color = if (selected) theme.textColor else theme.textColorSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}
