@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.glassBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val NAV_REST_ALPHA = 0.8f

@ExperimentalOneConfigUiApi
@Immutable
public class NavItemState internal constructor(
    public val selected: Boolean,
    public val hovered: Boolean,
    public val onAccent: Boolean,
    public val content: () -> Color,
    public val secondary: () -> Color,
)

@ExperimentalOneConfigUiApi
public object NavItemDefaults {
    public val height: Dp = 33.dp

    public val horizontalPadding: Dp = 12.dp

    public val iconGap: Dp = 16.dp

    public val trailingGap: Dp = 8.dp

    public val hoverPull: Dp = 0.75.dp

    public val shape: Shape
        @Composable get() = OneConfigTheme.current.sideBarNavigationEntryShape

    public val style: ItemSelectionStyle
        @Composable get() = if (OneConfigTheme.current.translucentSelection) ItemSelectionStyle.Translucent else ItemSelectionStyle.Accent
}

@ExperimentalOneConfigUiApi
@Composable
public fun NavItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    trailing: (@Composable RowScope.(NavItemState) -> Unit)? = null,
    enabled: Boolean = true,
    style: ItemSelectionStyle? = null,
    height: Dp = NavItemDefaults.height,
    shape: Shape? = null,
    labelStyle: TextStyle = OneConfigTheme.typography.body,
    role: A11yRole = A11yRole.Button,
    sound: UiSound? = UiSound.Click,
    recipe: ControlIndication? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val outline = shape ?: NavItemDefaults.shape
    val resolved = style ?: NavItemDefaults.style
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered && enabled
    val content = Motion.animateColorAsState(
        when {
            selected -> SelectionDefaults.itemContentColor(true, resolved)
            active -> theme.textColor
            else -> theme.textColor.copy(alpha = theme.textColor.alpha * NAV_REST_ALPHA)
        },
        Motion.colorSpec(),
        "navItemContent",
    )
    val secondary = Motion.animateColorAsState(SelectionDefaults.itemSecondaryColor(selected, resolved), Motion.colorSpec(), "navItemSecondary")
    val bridge = glassBridge()
    val pulled = if (bridge != null && enabled) bridge.glassHover(Modifier, outline, NavItemDefaults.hoverPull).graphicsLayer {} else Modifier
    val state = NavItemState(selected, active, selected && resolved == ItemSelectionStyle.Accent, { content.value }, { secondary.value })
    Row(
        modifier
            .height(height)
            .alpha(if (enabled) 1f else theme.disabledAlpha)
            .then(pulled)
            .itemSelection(selected, resolved, outline, active)
            .then(if (recipe != null) Modifier.controlInteraction(source, outline, recipe, enabled, focusRing = false, pointerIcon = null) else Modifier)
            .oneConfigClickable(label, onClick, role, enabled, selected = selected, shape = outline, indication = false, sound = sound, interactionSource = source)
            .padding(horizontal = NavItemDefaults.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NavItemDefaults.trailingGap),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.padding(end = NavItemDefaults.iconGap - NavItemDefaults.trailingGap), fitAspectRatio = true, tintProducer = state.content)
        }
        Text(
            label,
            if (trailing == null) Modifier.weight(1f, fill = false) else Modifier,
            style = labelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            colorProducer = { content.value },
        )
        trailing?.invoke(this, state)
    }
}
