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
import java.util.ServiceLoader
import org.apache.logging.log4j.LogManager
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
}

private val LOGGER = LogManager.getLogger("OneConfig/UI")

@ExperimentalOneConfigUiApi
private val loadedBridge: OneConfigUIComponentsBridge? by lazy {
    try {
        ServiceLoader.load(OneConfigUIComponentsBridge::class.java, OneConfigUIComponentsBridge::class.java.classLoader)
            .iterator().let { if (it.hasNext()) it.next() else null }
    } catch (t: Throwable) {
        LOGGER.warn("Failed to load OneConfigUIComponentsBridge; OneConfig UI components cannot be used", t)
        null
    }
}

@ExperimentalOneConfigUiApi
internal fun componentsBridge(): OneConfigUIComponentsBridge =
    loadedBridge ?: error("No OneConfigUIComponentsBridge is available; OneConfig UI components cannot be used")
