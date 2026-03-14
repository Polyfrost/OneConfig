package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.SelectableIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

interface RadioValue {
    val icon: String
}

@Composable
inline fun <reified E> Radio(
    label: String,
    value: E,
    crossinline onChange: (E) -> Unit,
) where E : RadioValue, E : Enum<E> {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        Box(
            modifier = Modifier.background(Color(0x192126).copy(.7f), RoundedCornerShape(6.dp))
                .border(1.dp, LocalTheme.current.borderColor, RoundedCornerShape(6.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                enumValues<E>().forEach { it as RadioValue
                    SelectableIconButton(
                        it.icon,
                        selected = value == it,
                        modifier = Modifier.size(18.dp),
                        onClick = { onChange(it as E) }
                    )
                }
            }
        }
    }
}