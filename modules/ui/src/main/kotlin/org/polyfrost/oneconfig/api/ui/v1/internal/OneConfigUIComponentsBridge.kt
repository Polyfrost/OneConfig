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
import androidx.compose.ui.window.PopupPositionProvider
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.UiSound
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeBuilder

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUIComponentsBridge {
    @Composable
    public fun currentTheme(): OneConfigUITheme

    @Composable
    public fun accent(): Color

    @Composable
    public fun ProvideTheme(theme: OneConfigUITheme?, scaleToUi: Boolean, content: @Composable () -> Unit)

    @Composable
    public fun themeShape(radius: Dp): Shape

    @Composable
    public fun themeIcon(name: String): String

    public fun customTheme(base: OneConfigUITheme, id: String, name: String, builder: ThemeBuilder): OneConfigUITheme

    public fun playSound(sound: UiSound)

    public fun keepSearchFocus(modifier: Modifier): Modifier

    public fun polyfrostedDark(): OneConfigUITheme

    public fun polyfrostedLight(): OneConfigUITheme

    public fun glassDark(): OneConfigUITheme

    public fun glassLight(): OneConfigUITheme

    public fun minecraftDark(): OneConfigUITheme

    public fun minecraftLight(): OneConfigUITheme

    public fun activeTheme(): OneConfigUITheme?

    public fun activateTheme(theme: OneConfigUITheme)

    public fun ensureThemeLoaded()

    @Composable
    public fun concentric(shape: Shape, inset: Dp, minRadius: Dp): Shape

    @Composable
    public fun Icon(iconName: String, color: Color, modifier: Modifier, fitAspectRatio: Boolean)

    @Composable
    public fun Text(
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
    )

    @Composable
    public fun Chip(label: String, selected: Boolean, icon: String?, onClick: () -> Unit)

    @Composable
    public fun IconButton(
        iconName: String,
        modifier: Modifier,
        foreground: Color,
        hoveredForeground: Color,
        enabled: Boolean,
        hoverBorder: Boolean,
        onClick: () -> Unit,
    )

    @Composable
    public fun Tooltip(
        text: @Composable () -> Unit,
        modifier: Modifier,
        anchor: Alignment,
        alignment: Alignment,
        offset: DpOffset,
        content: @Composable () -> Unit,
    )

    public fun onClick(modifier: Modifier, interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit): Modifier

    public fun reducedMotion(): Boolean

    @Composable
    public fun popupSurface(modifier: Modifier, shape: Shape): Modifier

    @Composable
    public fun Popup(
        positionProvider: PopupPositionProvider,
        onDismissRequest: (() -> Unit)?,
        focusable: Boolean,
        content: @Composable () -> Unit,
    )

    @Composable
    public fun fadingEdges(modifier: Modifier, scrollState: ScrollState, color: Color, length: Dp): Modifier

    public fun createColorPicker(initialColor: Color, hasAlpha: Boolean): Any

    @Composable
    public fun ColorPickerPopup(model: Any, onColorChanged: (Color) -> Unit, onClose: () -> Unit, chromaCapable: Boolean)

    public fun colorToHex(color: Color): String

    @Composable
    public fun optionWidth(): Dp

    @Composable
    public fun pageSearchQuery(): String

    @Composable
    public fun optionWidthOrNull(): Dp?

    @Composable
    public fun pageSearchQueryOrNull(): String?
}

@ExperimentalOneConfigUiApi
private val loadedBridge: OneConfigUIComponentsBridge? by lazy {
    loadBridge(OneConfigUIComponentsBridge::class.java, "OneConfig UI components cannot be used")
}

@ExperimentalOneConfigUiApi
internal fun componentsBridge(): OneConfigUIComponentsBridge =
    loadedBridge ?: error("No OneConfigUIComponentsBridge is available; OneConfig UI components cannot be used")
