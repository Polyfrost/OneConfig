package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.screens.Accordion
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** The panel grid's gap between columns and its fixed second and third column widths, the first taking the rest */
internal val GridGap = 12.dp
internal val GridCol2 = 112.dp
internal val GridCol3 = 96.dp

/** Whether a HUD panel block described by [labels] is kept by the panel's search [query] */
internal fun hudSearchMatches(query: String, vararg labels: String) =
    query.isEmpty() || labels.any { searchMatches(it, query) }

/** Hidden unless [query] matches [title] or one of the [labels] of the controls inside */
@Composable
fun Section(title: String, query: String, vararg labels: String, content: @Composable () -> Unit) {
    if (!hudSearchMatches(query, title, *labels)) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        content()
    }
}

/**
 * A group of controls on a config screen accordion card, hidden unless [query] matches [title] or one of the [labels]
 *
 * When [checked] isn't null the header holds a switch for the whole group which folds its body away while off, a
 * search keeping it open so matches stay visible. [content] is laid out in the panel grid and given the first
 * column's width
 */
@Composable
internal fun CardSection(
    title: String,
    query: String,
    vararg labels: String,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    content: @Composable (col1: Dp) -> Unit,
) {
    if (!hudSearchMatches(query, title, *labels)) return
    Accordion(
        title = title,
        expanded = checked != false || query.isNotEmpty(),
        onExpandedChange = null,
        checked = checked,
        onCheckedChange = onCheckedChange,
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 16.dp)) {
            val col1 = maxWidth - GridCol2 - GridCol3 - GridGap * 2
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content(col1) }
        }
    }
}

/** One row of the panel grid where [wide] takes the place of the first two columns */
@Composable
internal fun GridRow(
    col1: Dp,
    first: @Composable () -> Unit = {},
    second: @Composable () -> Unit = {},
    third: @Composable () -> Unit = {},
    wide: (@Composable () -> Unit)? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
        if (wide != null) {
            Box(Modifier.width(col1 + GridGap + GridCol2)) { wide() }
        } else {
            Box(Modifier.width(col1)) { first() }
            Box(Modifier.width(GridCol2)) { second() }
        }
        Box(Modifier.width(GridCol3)) { third() }
    }
}

/** [label] above its control */
@Composable
internal fun LabeledControl(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        content()
    }
}

/** [label] with its switch at the far end, laid out like a config screen option row */
@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = LocalTheme.current.textColor, fontSize = 14.sp)
        SwitchControl(checked, onCheckedChange)
    }
}
