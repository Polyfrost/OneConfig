package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Collections
import java.util.IdentityHashMap
import java.util.function.Consumer
import java.util.function.Predicate
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.MetaKeys
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.get
import org.polyfrost.oneconfig.api.ui.v1.components.ContextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.MenuItem
import org.polyfrost.oneconfig.api.ui.v1.components.MenuItemSize
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.api.settings.BooleanOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent

val LocalOptionWidth = compositionLocalOf { 300.dp }

val LocalOptionWidthOrNull = compositionLocalOf<Dp?> { null }
internal val OptionControlHeight = 32.dp

/**
 * Per-property reset counters
 *
 * Bumping a counter recomposes the visualizer subtree with a fresh [key] so controls re-read the reset
 * value instead of their cached state
 */
private val resetEpochs = mutableStateMapOf<Int, Int>()

/** True if [prop] has a captured default value that can be restored */
fun optionHasDefault(prop: Property<*>, seen: MutableSet<Property<*>> = identitySet()): Boolean =
    seen.add(prop) && (prop.getMetadata<Any?>("default") != null ||
        prop.getMetadata<Collection<Property<*>>>("resetWith")?.any { optionHasDefault(it, seen) } == true)

internal fun identitySet(): MutableSet<Property<*>> = Collections.newSetFromMap(IdentityHashMap())

fun bumpResetEpoch(prop: Property<*>) {
    resetEpochs[System.identityHashCode(prop)] = (resetEpochs[System.identityHashCode(prop)] ?: 0) + 1
}

/**
 * Restore [prop] to the default value captured at config initialization and refresh its UI.
 * Properties listed in its `resetWith` metadata are reset too, for custom visualizers that draw
 * several hidden properties in one row
 */
@Suppress("UNCHECKED_CAST")
fun resetOption(prop: Property<*>, seen: MutableSet<Property<*>> = identitySet()) {
    if (!seen.add(prop)) return
    val def = prop.getMetadata<Any?>("default")
    if (def != null) {
        (prop as Property<Any?>).setAsReferential(Config.copyDefault(prop.type, def))
        if (prop.type == PolyColor::class.java) updateAccent()
    }
    val linked = prop.getMetadata<Collection<Property<*>>>("resetWith")
    linked?.forEach { resetOption(it, seen) }
    if (def != null || linked != null) bumpResetEpoch(prop)
}

@Composable
fun Option(prop: Property<*>) {
    val vis = remember(System.identityHashCode(prop)) {
        when (val raw = prop[MetaKeys.VISUALIZER]) {
            is Visualizer -> raw
            is Class<*> -> (raw.getDeclaredConstructor().newInstance() as? Visualizer)
            else -> null
        }
    }
    if (vis != null) {
        val epoch = resetEpochs[System.identityHashCode(prop)] ?: 0
        key(System.identityHashCode(prop), epoch) {
            vis.visualize(prop)
        }
        return
    }
    val value = prop.shown()
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
    var checked by remember(data.prop) { mutableStateOf(data.boolProp.shown() == true) }
    // the same option can be flipped from elsewhere on screen, a mod's card for its master switch for one
    DisposableEffect(data.prop) {
        val listener = Predicate<Boolean> { value -> checked = value == true; false }
        // whatever locks an option can also change what it shows in place of the value
        val displayListener = Consumer<Property.Display> { checked = data.boolProp.shown() == true }
        data.boolProp.addCallback(listener)
        data.boolProp.addDisplayListener(displayListener)
        onDispose {
            data.boolProp.removeCallback(listener)
            data.boolProp.removeDisplayListener(displayListener)
        }
    }
    when (data.style) {
        BooleanOptionData.Style.Switch -> SwitchControl(checked) { checked = it; data.boolProp.set(it) }
        BooleanOptionData.Style.Checkbox -> CheckboxControl(checked) { checked = it; data.boolProp.set(it) }
    }
}

@Composable
fun OptionActionButton(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val color = if (isHovered) theme.textColor else theme.textColorSecondary

    if (!visible) {
        Box(modifier = modifier.size(24.dp).alpha(0f))
        return
    }

    Tooltip(
        text = {
            Text("More actions. Right-click option as shortcut.", color = theme.textColor, fontSize = 12.sp)
        },
        modifier = Modifier,
        anchor = Alignment.TopCenter,
    ) {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(theme.sideBarNavigationEntryShape)
                .onClick(interactionSource, onClick)
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            Icon("dots-vertical", color = color, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Right-click context menu for a single option anchored at [offset] in pixels relative to the anchoring
 * layout
 */
@Composable
fun OptionContextMenu(
    prop: Property<*>,
    expanded: Boolean,
    offset: IntOffset,
    onDismiss: () -> Unit,
    resetEnabled: Boolean = optionHasDefault(prop),
    onReset: (() -> Unit)? = null,
    onUnbind: (() -> Unit)? = null,
) {
    if (!expanded) return
    val isKeybind = remember(System.identityHashCode(prop)) { prop.get() is OneConfigKeybind }
    val effectiveReset = onReset ?: if (isKeybind) ({ resetKeybindOption(prop) }) else null
    val effectiveUnbind = onUnbind ?: if (isKeybind) ({ unbindKeybindOption(prop) }) else null
    ContextMenu(expanded, offset, onDismiss) {
        MenuItem("Reset to default", "refresh", null, MenuItemSize.Compact, false, false, resetEnabled, null, Modifier, null) {
            if (effectiveReset != null) effectiveReset() else resetOption(prop)
            onDismiss()
        }
        if (effectiveUnbind != null) {
            MenuItem("Unbind", "close") {
                effectiveUnbind()
                onDismiss()
            }
        }
    }
}

