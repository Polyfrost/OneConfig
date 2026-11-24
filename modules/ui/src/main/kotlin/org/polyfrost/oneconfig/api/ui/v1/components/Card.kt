@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
public fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    label: String? = null,
    selected: Boolean? = null,
    enabled: Boolean = true,
    shape: Shape? = null,
    hoverGlow: Boolean = true,
    border: CardBorder = CardBorder.Gradient,
    borderColor: Color? = null,
    role: A11yRole = if (selected != null) A11yRole.Option else A11yRole.Button,
    sound: UiSound? = UiSound.Click,
    recipe: ControlIndication? = null,
    selectionStyle: CardSelectionStyle = SelectionDefaults.Card,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val outline = shape ?: OneConfigTheme.current.shapes.large
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val surface = modifier.cardSurface(outline, hoverGlow, border, borderColor)
    val interactive = when {
        onClick != null && selected != null ->
            surface.selectableCard(selected, onClick, label ?: "", outline, enabled, selectionStyle, role, sound, recipe, source)
        onClick != null ->
            surface
                .then(if (recipe != null) Modifier.controlInteraction(source, outline, recipe, enabled, focusRing = false, pointerIcon = null) else Modifier)
                .oneConfigClickable(label, onClick, role, enabled, shape = outline, indication = false, sound = sound, interactionSource = source)
        selected != null -> surface.cardSelection(selected, outline, selectionStyle)
        else -> surface
    }
    Box(interactive.clip(outline).padding(contentPadding), content = content)
}
