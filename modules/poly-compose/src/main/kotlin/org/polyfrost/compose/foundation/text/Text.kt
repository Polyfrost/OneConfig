package org.polyfrost.compose.foundation.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.polyfrost.compose.node.PolyTextSpan
import org.polyfrost.compose.node.TextNode
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.text.font.FontFamily
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.BaselineShift
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.compose.ui.unit.sp

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: PolyColor = PolyColor.WHITE,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    fontFamily: FontFamily = FontFamily.Default,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    spans: List<PolyTextSpan>? = null,
    baselineShift: BaselineShift = BaselineShift.None,
) {
    val font = fontFamily.resolve(fontWeight)
    ComposeNode<TextNode, PolyApplier>(
        factory = { TextNode() },
        update = {
            set(text) { this.text = it; remeasure() }
            set(fontSize) { this.fontSize = it.value; remeasure() }
            set(color) { this.color = it }
            set(font) { fontName = it.takeUnless { n -> n == FontManager.DEFAULT_KEY }; remeasure() }
            set(textAlign) { center = it == TextAlign.Center; remeasure() }
            set(lineHeight) { this.lineHeight = it.value.takeUnless(Float::isNaN); remeasure() }
            set(overflow) { ellipsis = it == TextOverflow.Ellipsis; remeasure() }
            set(softWrap) { wrap = it; remeasure() }
            set(maxLines) { this.maxLines = it; remeasure() }
            set(spans) { this.spans = it; remeasure() }
            set(baselineShift) { this.baselineShift = it.multiplier }
            set(modifier) { it.applyTo(style); remeasure() }
        },
    )
}
