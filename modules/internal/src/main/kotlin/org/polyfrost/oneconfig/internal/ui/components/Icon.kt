package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Icon(iconName: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val resolvedColor = if (color == Color.Unspecified) LocalTheme.current.textColor else color
    val path = if (iconName.contains('/') || iconName.contains('.')) iconName
               else "/assets/oneconfig/ico/$iconName.svg"
    val isSvg = path.endsWith(".svg", ignoreCase = true)
    Image(
        painter = painterResource(path),
        contentDescription = null,
        modifier = modifier.size(18.dp),
        colorFilter = if (isSvg) ColorFilter.tint(resolvedColor) else null
    )
}

@Composable
fun IconWithIndicator(iconName: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val resolvedColor = if (color == Color.Unspecified) LocalTheme.current.textColor else color
    Box {
        Image(
            painter = painterResource("/assets/oneconfig/ico/$iconName.svg"),
            contentDescription = null,
            modifier = modifier.size(18.dp),
            colorFilter = ColorFilter.tint(resolvedColor)
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .offset(2.dp, (-2).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        Accent,
                        shape = LocalTheme.current.circleShape
                    )
                    .blur(7.dp)
            )

            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        Accent,
                        shape = LocalTheme.current.circleShape
                    )
            )
        }
    }
}
