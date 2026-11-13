package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuCustomItem
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.MoreButton
import org.polyfrost.oneconfig.api.ui.v1.components.OptionMenuContributors
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.hiddenUntilHover
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.settings.BooleanOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
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
            is Boolean -> if (value) translated("oneconfig.control.on", "On") else translated("oneconfig.control.off", "Off")
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

fun optionMenuEntries(
    prop: Property<*>,
    onReset: (() -> Unit)? = null,
    resetEnabled: Boolean? = null,
    onUnbind: (() -> Unit)? = null,
): List<MenuEntry> {
    val isKeybind = runCatching { prop.get() is OneConfigKeybind }.getOrDefault(false)
    val reset = onReset ?: { if (prop.get() is OneConfigKeybind) resetKeybindOption(prop) else resetOption(prop) }
    val builtIn = OptionMenuContributors.builtInEntries(prop, onReset = reset)
    val canReset = resetEnabled ?: optionHasDefault(prop)
    val entries = ArrayList<MenuEntry>(builtIn.size + 4)
    builtIn.forEachIndexed { index, entry ->
        entries += if (index == 0 && !canReset && entry is MenuAction) entry.disabled() else entry
    }
    if (isKeybind || onUnbind != null) {
        entries += MenuAction(
            translated("oneconfig.menu.option.unbind", "Unbind"),
            onUnbind ?: { unbindKeybindOption(prop) },
            icon = "close",
            enabled = prop.display != Property.Display.DISABLED,
        )
    }
    val contributed = OptionMenuContributors.entriesFor(prop)
    if (contributed.isNotEmpty()) {
        entries += MenuSeparator
        entries += contributed
    }
    return entries
}

private fun MenuAction.disabled(): MenuAction = MenuAction(
    label,
    onClick,
    icon = icon,
    painter = painter,
    detail = detail,
    style = style,
    enabled = false,
    disabledReason = disabledReason,
    checked = checked,
    shortcutHint = shortcutHint,
    badge = badge,
    size = size,
    leading = leading,
    children = children,
)

@Stable
class OptionMenuState internal constructor(private val prop: Property<*>) {
    private var openCount by mutableIntStateOf(0)

    internal var onReset: (() -> Unit)? = null
    internal var resetEnabled: Boolean? = null

    private val tracker = MenuCustomItem { TrackOpen() }

    internal var buttonEntries: List<MenuEntry> by mutableStateOf(deferredEntries())
        private set

    val isOpen: Boolean get() = openCount > 0

    fun entries(): List<MenuEntry> = optionMenuEntries(prop, onReset, resetEnabled) + tracker

    private fun deferredEntries(): List<MenuEntry> = DeferredMenuEntries(::entries)

    @Composable
    private fun TrackOpen() {
        DisposableEffect(Unit) {
            openCount++
            onDispose {
                openCount--
                buttonEntries = deferredEntries()
            }
        }
    }
}

private class DeferredMenuEntries(compute: () -> List<MenuEntry>) : AbstractList<MenuEntry>() {
    private val entries by lazy(LazyThreadSafetyMode.NONE, compute)

    override val size: Int get() = entries.size

    override fun get(index: Int): MenuEntry = entries[index]
}

@Composable
fun rememberOptionMenu(prop: Property<*>, onReset: (() -> Unit)? = null, resetEnabled: Boolean? = null): OptionMenuState {
    val state = remember(System.identityHashCode(prop)) { OptionMenuState(prop) }
    state.onReset = onReset
    state.resetEnabled = resetEnabled
    return state
}

fun Modifier.optionContextMenu(menu: OptionMenuState, enabled: Boolean = true): Modifier =
    contextMenu(enabled) { menu.entries() }

@Composable
fun OptionMoreButton(menu: OptionMenuState, visible: Boolean, modifier: Modifier = Modifier) {
    MoreButton(
        menu.buttonEntries,
        modifier.hiddenUntilHover(visible || menu.isOpen),
        contentDescription = translated("oneconfig.option.more_actions", "More actions. Right-click option as shortcut."),
    )
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
    val entries = remember(System.identityHashCode(prop)) { optionMenuEntries(prop, onReset, resetEnabled, onUnbind) }
    ContextMenu(expanded, offset, onDismiss, entries)
}

