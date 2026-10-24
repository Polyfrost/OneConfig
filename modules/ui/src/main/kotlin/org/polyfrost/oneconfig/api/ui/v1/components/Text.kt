package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.BaselineShift
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun Text(text: Any, modifier: Modifier, color: Color) {
    Text(text, modifier, color, 14.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@ExperimentalOneConfigUiApi
@Composable
fun Text(text: Any, modifier: Modifier, color: Color, fontSize: TextUnit) {
    Text(text, modifier, color, fontSize, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@ExperimentalOneConfigUiApi
@Composable
fun Text(text: Any, modifier: Modifier, color: Color, fontSize: TextUnit, fontWeight: FontWeight) {
    Text(text, modifier, color, fontSize, null, BaselineShift.None, fontWeight, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@ExperimentalOneConfigUiApi
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
) {
    componentsBridge().Text(text, modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow)
}
