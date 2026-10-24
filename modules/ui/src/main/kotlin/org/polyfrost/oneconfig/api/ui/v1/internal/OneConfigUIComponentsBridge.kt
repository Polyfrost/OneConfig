package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.BaselineShift
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme

@ExperimentalOneConfigUiApi
interface OneConfigUIComponentsBridge {
    @Composable
    fun currentTheme(): OneConfigUITheme

    @Composable
    fun accent(): Color

    @Composable
    fun ProvideTheme(content: @Composable () -> Unit)

    fun polyGlassDark(): OneConfigUITheme

    fun polyGlassLight(): OneConfigUITheme

    fun minecraftDark(): OneConfigUITheme

    fun minecraftLight(): OneConfigUITheme

    fun activeTheme(): OneConfigUITheme?

    fun activateTheme(theme: OneConfigUITheme)

    fun ensureThemeLoaded()

    @Composable
    fun concentric(shape: Shape, inset: Dp, minRadius: Dp): Shape

    @Composable
    fun Icon(iconName: String, color: Color, modifier: Modifier, fitAspectRatio: Boolean)

    @Composable
    fun Text(
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
    fun Chip(label: String, selected: Boolean, icon: String?, onClick: () -> Unit)

    @Composable
    fun IconButton(
        iconName: String,
        modifier: Modifier,
        foreground: Color,
        hoveredForeground: Color,
        enabled: Boolean,
        hoverBorder: Boolean,
        onClick: () -> Unit,
    )

    @Composable
    fun Tooltip(
        text: @Composable () -> Unit,
        modifier: Modifier,
        anchor: Alignment,
        alignment: Alignment,
        offset: DpOffset,
        content: @Composable () -> Unit,
    )

    fun onClick(modifier: Modifier, interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit): Modifier

    @Composable
    fun fadingEdges(modifier: Modifier, scrollState: ScrollState, color: Color, length: Dp): Modifier

    fun createColorPicker(initialColor: Color, hasAlpha: Boolean): Any

    @Composable
    fun ColorPickerPopup(model: Any, onColorChanged: (Color) -> Unit, onClose: () -> Unit, chromaCapable: Boolean)

    fun colorToHex(color: Color): String

    @Composable
    fun optionWidth(): Dp

    @Composable
    fun pageSearchQuery(): String
}

@ExperimentalOneConfigUiApi
private val loadedBridge: OneConfigUIComponentsBridge? by lazy {
    loadBridge(OneConfigUIComponentsBridge::class.java, "OneConfig UI components cannot be used")
}

@ExperimentalOneConfigUiApi
internal fun componentsBridge(): OneConfigUIComponentsBridge =
    loadedBridge ?: error("No OneConfigUIComponentsBridge is available; OneConfig UI components cannot be used")
