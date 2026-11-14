package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private const val COLLAPSED_ROTATION = 90f
private const val EXPANDED_ROTATION = 180f
private val ChevronSize = 12.dp

@Composable
internal fun CollapseChevron(expanded: Boolean, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val rotation by Motion.animateFloatAsState(if (expanded) EXPANDED_ROTATION else COLLAPSED_ROTATION, label = "collapseChevron")
    val tint = if (color == Color.Unspecified) LocalTheme.current.textColorSecondary else color
    Icon("up", color = tint, modifier = modifier.size(ChevronSize).graphicsLayer { rotationZ = rotation })
}

internal fun Modifier.collapsibleToggle(
    label: String,
    onToggle: (() -> Unit)?,
    interactionSource: MutableInteractionSource? = null,
): Modifier = if (onToggle == null) this else oneConfigClickable(label, onToggle, indication = false, interactionSource = interactionSource)

@Composable
internal fun CollapsibleHeader(
    label: String,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
    chevron: Boolean = onToggle != null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp),
    interactionSource: MutableInteractionSource? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Row(
        modifier = modifier.fillMaxWidth().collapsibleToggle(label, onToggle, source).padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        content()
        if (chevron) CollapseChevron(expanded)
        trailing()
    }
}
