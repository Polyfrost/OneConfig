package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import net.kyori.adventure.text.Component
import org.polyfrost.oneconfig.api.ui.v1.components.ColorPickerModel as KitColorPickerModel
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.internal.ui.components.ReducedMotion
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.internal.ui.api.Tooltip as InternalTooltip
import org.polyfrost.oneconfig.internal.ui.components.Chip as InternalChip
import org.polyfrost.oneconfig.internal.ui.components.Icon as InternalIcon
import org.polyfrost.oneconfig.internal.ui.components.IconButton as InternalIconButton
import org.polyfrost.oneconfig.internal.ui.components.StyledText
import org.polyfrost.oneconfig.internal.ui.components.componentText
import org.polyfrost.oneconfig.internal.ui.components.legacyText
import org.polyfrost.oneconfig.internal.ui.components.fadingEdges
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.settings.ChromaColorAnimation
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
import org.polyfrost.oneconfig.internal.ui.themes.PolyfrostedDark
import org.polyfrost.oneconfig.internal.ui.themes.PolyfrostedLight
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.api.ui.v1.components.UiSound
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeBuilder
import org.polyfrost.oneconfig.internal.ui.components.keepSearchFocus
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidthOrNull
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQueryOrNull as shellPageSearchQueryOrNull
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.ThemeWith
import org.polyfrost.oneconfig.internal.ui.themes.modTheme
import org.polyfrost.oneconfig.internal.ui.themes.themedShape

class OneConfigUIComponentsBridgeImpl : OneConfigUIComponentsBridge {
    @Composable
    override fun currentTheme(): OneConfigUITheme = LocalTheme.current

    @Composable
    override fun accent(): Color = Accent

    @Composable
    override fun ProvideTheme(theme: OneConfigUITheme?, scaleToUi: Boolean, content: @Composable () -> Unit) {
        ensureThemeLoaded()
        ThemeWith(theme as? UITheme, scaleToUi, content = content)
    }

    @Composable
    override fun themeShape(radius: Dp): Shape = themedShape(radius)

    @Composable
    override fun themeIcon(name: String): String = LocalTheme.current.iconOverrides[name] ?: name

    override fun customTheme(base: OneConfigUITheme, id: String, name: String, builder: ThemeBuilder): OneConfigUITheme =
        modTheme(base, id, name, builder)

    override fun playSound(sound: UiSound) {
        UiSounds.play(
            when (sound) {
                UiSound.Click -> UiSoundEvent.CLICK
                UiSound.Open -> UiSoundEvent.OPEN
                UiSound.Close -> UiSoundEvent.CLOSE
                UiSound.SliderTick -> UiSoundEvent.SLIDER_TICK
            },
        )
    }

    override fun keepSearchFocus(modifier: Modifier): Modifier = modifier.keepSearchFocus()

    override fun polyfrostedDark(): OneConfigUITheme = PolyfrostedDark

    override fun polyfrostedLight(): OneConfigUITheme = PolyfrostedLight

    override fun glassDark(): OneConfigUITheme = PolyGlassDark

    override fun glassLight(): OneConfigUITheme = PolyGlassLight

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
    override fun Icon(iconName: String, color: Color, modifier: Modifier, fitAspectRatio: Boolean, tint: (() -> Color)?) =
        InternalIcon(iconName, color, modifier, fitAspectRatio, tint)

    @Composable
    override fun Text(
        text: String,
        modifier: Modifier,
        color: Color,
        style: TextStyle,
        maxLines: Int,
        overflow: TextOverflow,
        softWrap: Boolean,
        textAlign: TextAlign?,
        colorProducer: ColorProducer?,
        onTextLayout: ((TextLayoutResult) -> Unit)?,
    ) = StyledText(legacyText(text), modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer, onTextLayout)

    @Composable
    override fun Text(
        text: AnnotatedString,
        modifier: Modifier,
        color: Color,
        style: TextStyle,
        maxLines: Int,
        overflow: TextOverflow,
        softWrap: Boolean,
        textAlign: TextAlign?,
        colorProducer: ColorProducer?,
        onTextLayout: ((TextLayoutResult) -> Unit)?,
    ) = StyledText(text, modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer, onTextLayout)

    @Composable
    override fun Text(
        text: Component,
        modifier: Modifier,
        color: Color,
        style: TextStyle,
        maxLines: Int,
        overflow: TextOverflow,
        softWrap: Boolean,
        textAlign: TextAlign?,
        colorProducer: ColorProducer?,
        onTextLayout: ((TextLayoutResult) -> Unit)?,
    ) {
        val annotated = remember(text) { componentText(text) }
        StyledText(annotated, modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer, onTextLayout)
    }

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

    override fun reducedMotion(): Boolean = ReducedMotion.enabled

    @Composable
    override fun popupSurface(modifier: Modifier, shape: Shape): Modifier {
        val theme = LocalTheme.current
        return modifier
            .shellTransition()
            .liquidGlass(shape, frosted = true)
            .background(theme.popupBackground, shape)
            .border(1.dp, theme.borderColor, shape)
    }

    @Composable
    override fun Popup(
        positionProvider: PopupPositionProvider,
        onDismissRequest: (() -> Unit)?,
        focusable: Boolean,
        content: @Composable () -> Unit,
    ) = ShellPopup(positionProvider, onDismissRequest, PopupProperties(focusable = focusable, clippingEnabled = false), content)

    @Composable
    override fun fadingEdges(modifier: Modifier, scrollState: ScrollState, color: Color, length: Dp): Modifier =
        modifier.fadingEdges(scrollState, color, length)

    override fun createColorPicker(
        initialColor: Color,
        hasAlpha: Boolean,
        chroma: Boolean,
        chromaSpeed: Float,
        gradient: Color?,
    ): KitColorPickerModel = ColorPickerModel(initialColor, hasAlpha).also { configure(it, initialColor, chroma, chromaSpeed, gradient) }

    override fun updateColorPicker(model: KitColorPickerModel, color: Color, chroma: Boolean, chromaSpeed: Float, gradient: Color?) {
        if (model is ColorPickerModel) configure(model, color, chroma, chromaSpeed, gradient) else model.set(color)
    }

    private fun configure(model: ColorPickerModel, color: Color, chroma: Boolean, chromaSpeed: Float, gradient: Color?) {
        model.chromaEnabled = false
        model.set(color)
        model.chromaSpeed = chromaSpeed
        model.chromaEnabled = chroma
        model.gradientEnabled = gradient != null
        if (gradient != null) model.gradientColor = gradient
    }

    @Composable
    override fun ColorPickerPanel(
        model: KitColorPickerModel,
        onColorChanged: (Color) -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier,
        chromaCapable: Boolean,
        gradientCapable: Boolean,
    ) {
        val picker = model as? ColorPickerModel ?: remember(model) {
            ColorPickerModel(model.color, true).also { configure(it, model.color, model.chroma, model.chromaSpeed, model.gradient) }
        }
        val changed: (Color) -> Unit = if (picker === model) onColorChanged else { color -> model.set(color); onColorChanged(color) }
        ChromaColorAnimation(picker) {}
        InternalColorPickerPopup(picker, changed, onDismiss, chromaCapable, gradientCapable, modifier)
    }

    override fun colorToHex(color: Color): String = internalColorToHex(color)

    @Composable
    override fun optionWidth(): Dp = LocalOptionWidth.current

    @Composable
    override fun pageSearchQuery(): String = shellPageSearchQuery()

    @Composable
    override fun optionWidthOrNull(): Dp? = LocalOptionWidthOrNull.current

    @Composable
    override fun pageSearchQueryOrNull(): String? = shellPageSearchQueryOrNull()
}
