package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.MutableInteractionSource
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.BiasAlignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.drawWithContent
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.BaselineShift
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.internal.ui.components.Chip as InternalChip
import org.polyfrost.oneconfig.internal.ui.components.Icon as InternalIcon
import org.polyfrost.oneconfig.internal.ui.components.IconButton as InternalIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text as InternalText
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup as InternalColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.colorToHex as internalColorToHex
import org.polyfrost.oneconfig.internal.ui.shell.LocalPageScope
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.MinecraftDark
import org.polyfrost.oneconfig.internal.ui.themes.MinecraftLight
import org.polyfrost.oneconfig.internal.ui.themes.PolyGlassDark
import org.polyfrost.oneconfig.internal.ui.themes.PolyGlassLight
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

class OneConfigUIComponentsBridgeImpl : OneConfigUIComponentsBridge {
    @Composable
    override fun currentTheme(): OneConfigUITheme = LocalTheme.current

    @Composable
    override fun accent(): Color = Accent

    @Composable
    override fun ProvideTheme(content: @Composable () -> Unit) = Theme(content)

    override fun polyGlassDark(): OneConfigUITheme = PolyGlassDark

    override fun polyGlassLight(): OneConfigUITheme = PolyGlassLight

    override fun minecraftDark(): OneConfigUITheme = MinecraftDark

    override fun minecraftLight(): OneConfigUITheme = MinecraftLight

    override fun activeTheme(): OneConfigUITheme? = ThemeRegistry.activeTheme

    override fun activateTheme(theme: OneConfigUITheme) {
        require(theme is UITheme) { "Only themes from OneConfigTheme can be activated" }
        ThemeRegistry.activate(theme)
    }

    override fun ensureThemeLoaded() {
        ThemeRegistry.init()
        ThemeRegistry.loadFromConfig()
    }

    @Composable
    override fun concentric(shape: Shape, inset: Dp, minRadius: Dp): Shape = shape.concentric(inset, minRadius)

    @Composable
    override fun Icon(iconName: String, color: Color, modifier: Modifier, fitAspectRatio: Boolean) =
        InternalIcon(iconName, color, modifier, fitAspectRatio)

    @Composable
    override fun Text(
        text: Any,
        modifier: Modifier,
        color: Color,
        fontSize: TextUnit,
        lineHeight: TextUnit?,
        shift: BaselineShift,
        fontWeight: FontWeight,
        textAlign: TextAlign,
        maxLines: Int,
        overflow: TextOverflow,
    ) = InternalText(text, modifier, color, fontSize, lineHeight, fontWeight, textAlign, maxLines, overflow, baselineShift = shift)

    @Composable
    override fun Chip(label: String, selected: Boolean, icon: String?, onClick: () -> Unit) =
        InternalChip(label, selected, icon, onClick)

    @Composable
    override fun IconButton(
        iconName: String,
        modifier: Modifier,
        foreground: Color,
        hoveredForeground: Color,
        enabled: Boolean,
        hoverBorder: Boolean,
        onClick: () -> Unit,
    ) = InternalIconButton(iconName, modifier, foreground, hoveredForeground, enabled, hoverBorder, onClick)

    @Composable
    override fun Tooltip(
        text: @Composable () -> Unit,
        modifier: Modifier,
        anchor: Alignment,
        alignment: Alignment,
        offset: DpOffset,
        content: @Composable () -> Unit,
    ) {
        val theme = LocalTheme.current
        var hovered by remember { mutableStateOf(false) }
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(hovered) {
            if (!hovered) {
                shown = false
                return@LaunchedEffect
            }
            delay(200)
            shown = true
        }
        Box(Modifier.hoverable { hovered = it }) {
            content()
            if (shown) Popup(
                alignment = BiasAlignment(-alignment.horizontalBias, -alignment.verticalBias),
                offset = offset,
                anchor = anchor,
            ) {
                Box(
                    modifier.background(theme.popupBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    text()
                }
            }
        }
    }

    override fun onClick(
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
        enabled: Boolean,
        onClick: () -> Unit,
    ): Modifier = modifier.onClick(interactionSource, enabled, onClick)

    @Composable
    override fun fadingEdges(modifier: Modifier, scrollState: ScrollState, color: Color, length: Dp): Modifier {
        var canScrollBack by remember(scrollState) { mutableStateOf(false) }
        var canScrollForward by remember(scrollState) { mutableStateOf(false) }
        val top by animateFloatAsState(if (canScrollBack) 1f else 0f)
        val bottom by animateFloatAsState(if (canScrollForward) 1f else 0f)
        val topColor = color.copy(alpha = color.alphaF * top)
        val bottomColor = color.copy(alpha = color.alphaF * bottom)
        return modifier
            .onPlaced {
                canScrollBack = scrollState.value > 0f
                canScrollForward = scrollState.value < scrollState.maxValue
            }
            .drawWithContent {
                drawContent()
                val l = length.value.coerceAtMost(height / 2f)
                if (topColor.alpha > 0) ctx.rectGradient(x, y, width, l, Brush.verticalGradient(topColor, topColor.copy(alpha = 0f)))
                if (bottomColor.alpha > 0) ctx.rectGradient(x, y + height - l, width, l, Brush.verticalGradient(bottomColor.copy(alpha = 0f), bottomColor))
            }
    }

    override fun createColorPicker(initialColor: Color, hasAlpha: Boolean): Any = ColorPickerModel(initialColor, hasAlpha)

    @Composable
    override fun ColorPickerPopup(model: Any, onColorChanged: (Color) -> Unit, onClose: () -> Unit, chromaCapable: Boolean) =
        InternalColorPickerPopup(model as ColorPickerModel, onColorChanged, onClose, chromaCapable)

    override fun colorToHex(color: Color): String = internalColorToHex(color)

    @Composable
    override fun optionWidth(): Dp = LocalOptionWidth.current

    @Composable
    override fun pageSearchQuery(): String = LocalPageScope.current?.query ?: ShellState.searchQuery
}
