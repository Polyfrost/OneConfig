package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.internal.ui.api.settings.BooleanOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent

/**
 * Per-property reset counters. Bumping a property's counter forces its visualizer subtree to
 * recompose with a fresh [key], so controls re-read the (reset) value instead of their cached state.
 */
private val resetEpochs = mutableStateMapOf<Property<*>, Int>()

/** True if [prop] has a captured default value that can be restored. */
fun optionHasDefault(prop: Property<*>): Boolean = prop.getMetadata<Any?>("default") != null

/** Restore [prop] to the default value captured at config initialization, and refresh its UI. */
@Suppress("UNCHECKED_CAST")
fun resetOption(prop: Property<*>) {
    val def = prop.getMetadata<Any?>("default") ?: return
    (prop as Property<Any?>).setAsReferential(def)
    // mirror ColorOption: a PolyColor change must refresh the live accent (otherwise it only
    // updates after the GUI is reopened).
    if (prop.type == PolyColor::class.java) updateAccent()
    resetEpochs[prop] = (resetEpochs[prop] ?: 0) + 1
}

@Composable
fun Option(prop: Property<*>) {
    val vis = remember(prop) {
        when (val raw = prop.getMetadata<Any>("visualizer")) {
            is Visualizer -> raw
            is Class<*> -> (raw.getDeclaredConstructor().newInstance() as? Visualizer)
            else -> null
        }
    }
    if (vis != null) {
        val epoch = resetEpochs[prop] ?: 0
        key(prop, epoch) {
            vis.visualize(prop)
        }
        return
    }
    // Fallback: render value as text for properties with no visualizer set
    val value = prop.get()
    Text(
        when (value) {
            is Boolean -> if (value) "On" else "Off"
            null -> "—"
            else -> value.toString()
        },
        color = LocalTheme.current.textColorSecondary,
        fontSize = 14.sp,
    )
}

@Composable
fun BooleanOption(data: BooleanOptionData) {
    var checked by remember(data.prop) { mutableStateOf(data.boolProp.get() == true) }
    when (data.style) {
        BooleanOptionData.Style.Switch -> SwitchControl(checked) { checked = it; data.boolProp.set(it) }
        BooleanOptionData.Style.Checkbox -> CheckboxControl(checked) { checked = it; data.boolProp.set(it) }
    }
}

/**
 * Right-click context menu for a single option, anchored at [offset] (pixels, relative to the
 * anchoring layout). Currently offers "Reset to default".
 */
@Composable
fun OptionContextMenu(
    prop: Property<*>,
    expanded: Boolean,
    offset: IntOffset,
    onDismiss: () -> Unit,
    resetEnabled: Boolean = optionHasDefault(prop),
    onReset: (() -> Unit)? = null,
) {
    if (!expanded) return
    val theme = LocalTheme.current
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(4.dp),
        ) {
            ContextMenuItem("refresh", "Reset to default", enabled = resetEnabled) {
                if (onReset != null) onReset() else resetOption(prop)
                onDismiss()
            }
        }
    }
}

@Composable
private fun ContextMenuItem(icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val color = if (enabled) theme.textColor else theme.textColorSecondary
    Row(
        modifier = Modifier
            .clip(theme.sideBarNavigationEntryShape)
            .then(if (enabled) Modifier.onClick(interactionSource, onClick).pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, color = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 13.sp)
    }
}
