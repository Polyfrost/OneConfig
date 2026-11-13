@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val LOGGER = LogManager.getLogger("OneConfig/UI")

private val warnedTextTypes = ConcurrentHashMap.newKeySet<Class<*>>()

@ExperimentalOneConfigUiApi
@Composable
public fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = OneConfigTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    textAlign: TextAlign? = null,
    colorProducer: ColorProducer? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    componentsBridge().Text(text, modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer, onTextLayout)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = OneConfigTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    textAlign: TextAlign? = null,
    colorProducer: ColorProducer? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    componentsBridge().Text(text, modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer, onTextLayout)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Text(
    text: Component,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = OneConfigTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    textAlign: TextAlign? = null,
    colorProducer: ColorProducer? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    componentsBridge().Text(text, modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer, onTextLayout)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Text(
    text: List<String>,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = OneConfigTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    textAlign: TextAlign? = null,
    colorProducer: ColorProducer? = null,
) {
    Column(modifier) {
        for (line in text) Text(line, Modifier, color, style, maxLines, overflow, softWrap, textAlign, colorProducer)
    }
}

@ExperimentalOneConfigUiApi
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
) {
    val style = OneConfigTheme.typography.body.copy(
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight ?: TextUnit.Unspecified,
        baselineShift = shift,
    )
    AnyText(text, modifier, color, style, textAlign, maxLines, overflow)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Text(text: Any, modifier: Modifier, color: Color) {
    Text(text, modifier, color, 14.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Text(text: Any, modifier: Modifier, color: Color, fontSize: TextUnit) {
    Text(text, modifier, color, fontSize, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Text(text: Any, modifier: Modifier, color: Color, fontSize: TextUnit, fontWeight: FontWeight) {
    Text(text, modifier, color, fontSize, null, BaselineShift.None, fontWeight, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@Composable
private fun AnyText(
    text: Any,
    modifier: Modifier,
    color: Color,
    style: TextStyle,
    textAlign: TextAlign,
    maxLines: Int,
    overflow: TextOverflow,
) {
    when (val value = runCatching { Platform.compatibility().wrapPlatformComponent(text) }.getOrNull() ?: text) {
        is String -> Text(value, modifier, color, style, maxLines, overflow, textAlign = textAlign)
        is AnnotatedString -> Text(value, modifier, color, style, maxLines, overflow, textAlign = textAlign)
        is Component -> Text(value, modifier, color, style, maxLines, overflow, textAlign = textAlign)
        is ComponentLike -> Text(value.asComponent(), modifier, color, style, maxLines, overflow, textAlign = textAlign)
        is Iterable<*> -> Column(modifier) {
            for (line in value) if (line != null) AnyText(line, Modifier, color, style, textAlign, maxLines, overflow)
        }
        else -> {
            if (warnedTextTypes.add(value.javaClass)) LOGGER.warn("Text does not support {}; drawing its toString() instead", value.javaClass.name)
            Text(value.toString(), modifier, color, style, maxLines, overflow, textAlign = textAlign)
        }
    }
}
