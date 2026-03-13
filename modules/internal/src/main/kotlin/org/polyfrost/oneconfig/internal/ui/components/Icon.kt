package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun Icon(iconName: String, color: Color = Color.White, modifier: Modifier = Modifier) {
    val path = if (iconName.contains('/') || iconName.contains('.')) iconName
               else "/assets/oneconfig/ico/$iconName.svg"
    Image(
        painter = painterResource(path),
        contentDescription = null,
        modifier = modifier.size(18.dp),
        colorFilter = ColorFilter.tint(color)
    )
}

@Composable
fun IconWithIndicator(iconName: String, color: Color = Color.White, modifier: Modifier = Modifier) {
    Box {
        Image(
            painter = painterResource("/assets/oneconfig/ico/$iconName.svg"),
            contentDescription = null,
            modifier = modifier.size(18.dp),
            colorFilter = ColorFilter.tint(color)
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .offset(2.dp, (-2).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .blur(4.dp)
                    .background(
                        Color(0xFF1878F1),
                        shape = CircleShape
                    )
            )

            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        Color(0xFF1878F1),
                        shape = CircleShape
                    )
            )
        }
    }
}
