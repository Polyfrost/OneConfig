package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
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
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                enumValues<E>().forEach { it as RadioValue
                    Box(
                        modifier = Modifier
                            .size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        SelectableIconButton(
                            it.icon,
                            selected = value == it,
                            modifier = Modifier.size(18.dp),
                            onClick = {
                                Snapshot.withMutableSnapshot {
                                    onChange(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
