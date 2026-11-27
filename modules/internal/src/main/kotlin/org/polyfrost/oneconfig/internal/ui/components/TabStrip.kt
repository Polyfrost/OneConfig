package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.TabBadgePlacement
import org.polyfrost.oneconfig.api.ui.v1.components.TabItem
import org.polyfrost.oneconfig.api.ui.v1.components.TabRow
import org.polyfrost.oneconfig.api.ui.v1.components.TabStyle
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

internal class StripTab<T>(val value: T, val label: String, val count: Int? = null)

@Composable
internal fun <T> TabStrip(
    tabs: List<StripTab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val hairline = LocalTheme.current.borderColor
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(hairline, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            TabRow(
                tabs.map { TabItem(it.value, it.label, badge = it.count?.toString()) },
                selected,
                onSelect,
                style = TabStyle.Underline,
                size = ControlSize.Large,
                fill = false,
                track = false,
                badgePlacement = TabBadgePlacement.Count,
            )
        }
        if (trailing != null) {
            Row(
                Modifier.padding(start = 16.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
    }
}
