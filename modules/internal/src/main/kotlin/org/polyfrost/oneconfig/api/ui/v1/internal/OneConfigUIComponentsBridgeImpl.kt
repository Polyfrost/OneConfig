package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.internal.ui.api.Tooltip as InternalTooltip
import org.polyfrost.oneconfig.internal.ui.components.Chip as InternalChip
import org.polyfrost.oneconfig.internal.ui.components.Icon as InternalIcon
import org.polyfrost.oneconfig.internal.ui.components.IconButton as InternalIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text as InternalText
import org.polyfrost.oneconfig.internal.ui.components.fadingEdges
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup as InternalColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.colorToHex as internalColorToHex
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery as shellPageSearchQuery
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
    ) = InternalText(text, modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow)

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
    ) = InternalTooltip(text, modifier, anchor, alignment, offset, content)

    override fun onClick(
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
        enabled: Boolean,
        onClick: () -> Unit,
    ): Modifier = modifier.onClick(interactionSource, enabled, onClick)

    @Composable
    override fun fadingEdges(modifier: Modifier, scrollState: ScrollState, color: Color, length: Dp): Modifier =
        modifier.fadingEdges(scrollState, color, length)

    override fun createColorPicker(initialColor: Color, hasAlpha: Boolean): Any = ColorPickerModel(initialColor, hasAlpha)

    @Composable
    override fun ColorPickerPopup(model: Any, onColorChanged: (Color) -> Unit, onClose: () -> Unit, chromaCapable: Boolean) =
        InternalColorPickerPopup(model as ColorPickerModel, onColorChanged, onClose, chromaCapable)

    override fun colorToHex(color: Color): String = internalColorToHex(color)

    @Composable
    override fun optionWidth(): Dp = LocalOptionWidth.current

    @Composable
    override fun pageSearchQuery(): String = shellPageSearchQuery()
}
