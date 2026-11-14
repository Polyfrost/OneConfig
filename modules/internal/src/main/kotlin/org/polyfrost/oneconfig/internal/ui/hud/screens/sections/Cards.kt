package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.screens.Accordion
import org.polyfrost.oneconfig.internal.ui.screens.displayAlpha
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** The panel grid's gap between columns and its fixed second and third column widths, the first taking the rest */
internal val GridGap = 12.dp
internal val GridCol2 = 112.dp
internal val GridCol3 = 96.dp

private val CardRowGap = 12.dp

internal fun hudPanelText(key: String, fallback: String): String = translated("oneconfig.hud.panel.$key", fallback)

/** Whether a HUD panel block described by [labels] is kept by the panel's search [query] */
internal fun hudSearchMatches(query: String, vararg labels: String) =
    query.isEmpty() || labels.any { searchMatches(it, query) }

/**
 * Controls on a config screen accordion card, hidden unless [query] matches [title] or one of the [labels]
 *
 * With [checked] the header gets a switch and an arrow: the switch turns the group on or off and folds it to match,
 * and while off the body is dimmed and ignores input. A search keeps the card open so its matches stay visible
 *
 * Without [checked], a non-null [collapsedByDefault] still gives the header an arrow and picks whether the card starts folded
 *
 * [onReset] adds a reset button after the title, shown while [canReset]. [content] is laid out in the panel grid and
 * given the first column's width
 */
@Composable
internal fun CardSection(
    title: String,
    query: String,
    vararg labels: String,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    onReset: (() -> Unit)? = null,
    canReset: Boolean = true,
    collapsedByDefault: Boolean? = null,
    content: @Composable (col1: Dp) -> Unit,
) {
    if (!hudSearchMatches(query, title, *labels)) return
    val foldable = checked != null || collapsedByDefault != null
    var expanded by remember { mutableStateOf(checked ?: (collapsedByDefault != true)) }
    Accordion(
        title = title,
        expanded = !foldable || expanded || query.isNotEmpty(),
        onExpandedChange = if (foldable) ({ expanded = it }) else null,
        checked = checked,
        onCheckedChange = {
            onCheckedChange(it)
            expanded = it
        },
        titleActions = {
            if (onReset != null && canReset) {
                val label = translated("oneconfig.hud.panel.reset_card", "Reset %s", title)
                IconButton(
                    "refresh",
                    label,
                    onReset,
                    Modifier.size(24.dp, 20.dp),
                    size = ControlSize.Compact,
                    tooltipPlacement = TooltipPlacement.AboveCenter,
                )
            }
        },
    ) { enabled ->
        BoxWithConstraints(
            Modifier
                .padding(horizontal = 16.dp)
                .alpha(if (enabled) 1f else displayAlpha(Property.Display.DISABLED))
                .blockInteraction(!enabled, keyboard = true),
        ) {
            val col1 = maxWidth - GridCol2 - GridCol3 - GridGap * 2
            Column(verticalArrangement = Arrangement.spacedBy(CardRowGap)) { content(col1) }
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

/** [label] with its controls at the far end, laid out like a config screen option row */
@Composable
internal fun ControlRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), color = LocalTheme.current.textColor, fontSize = 14.sp)
        content()
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ControlRow(label) { SwitchControl(checked, onCheckedChange) }
}

/**
 * A card [row] with [folded] rows under it that unfold while [unfolded] like an accordion's body
 *
 * The gap above the folded rows moves with them so nothing jumps once they finish folding away
 */
@Composable
internal fun RowWithFold(unfolded: Boolean, row: @Composable () -> Unit, folded: @Composable () -> Unit) {
    Column {
        row()
        AnimatedVisibility(
            visible = unfolded,
            enter = Motion.enter(expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()),
            exit = Motion.exit(shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()),
        ) {
            Column(Modifier.padding(top = CardRowGap), verticalArrangement = Arrangement.spacedBy(CardRowGap)) { folded() }
        }
    }
}

/** A grid cell whose [content] fades in and out with [visible] while the cell keeps its place */
@Composable
internal fun FadingCell(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = Motion.enter(fadeIn()), exit = Motion.exit(fadeOut())) { content() }
}
