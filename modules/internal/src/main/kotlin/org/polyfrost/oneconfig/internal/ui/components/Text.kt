package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Text(text: String, modifier: Modifier = Modifier, color: Color, fontSize: TextUnit = 14.sp, lineHeight: TextUnit? = null, shift: BaselineShift = BaselineShift.None, fontWeight: FontWeight = FontWeight.Normal) {
    BasicText(
        text, style = TextStyle(
            fontSize = fontSize, fontWeight = fontWeight,
            fontFamily = LocalTheme.current.typography.family,
            color = color,
            lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
        ),
        modifier = modifier
    )
}