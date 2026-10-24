package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.DataInputStream
import java.util.function.Predicate
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.IntOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.PolyVisualizer
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.settings.BooleanOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.*
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent

private val ContextMenuPadding = 4.dp

/**
 * Per-property reset counters
 *
 * Bumping a counter recomposes the visualizer subtree with a fresh [key] so controls re-read the reset
 * value instead of their cached state
 */
private val resetEpochs = mutableStateMapOf<Int, Int>()

/** True if [prop] has a captured default value that can be restored */
fun optionHasDefault(prop: Property<*>): Boolean = prop.getMetadata<Any?>("default") != null

fun bumpResetEpoch(prop: Property<*>) {
    resetEpochs[System.identityHashCode(prop)] = (resetEpochs[System.identityHashCode(prop)] ?: 0) + 1
}

/** Restore [prop] to the default value captured at config initialization and refresh its UI */
@Suppress("UNCHECKED_CAST")
fun resetOption(prop: Property<*>) {
    val def = prop.getMetadata<Any?>("default") ?: return
    (prop as Property<Any?>).setAsReferential(Config.copyDefault(prop.type, def))
    // like ColorOption a PolyColor change must refresh the live accent or it only updates once the GUI
    // is reopened
    if (prop.type == PolyColor::class.java) updateAccent()
    resetEpochs[System.identityHashCode(prop)] = (resetEpochs[System.identityHashCode(prop)] ?: 0) + 1
}

@Composable
fun Option(prop: Property<*>) {
    val vis = remember(System.identityHashCode(prop)) {
        when (val raw = prop.getMetadata<Any>("visualizer")) {
            is Visualizer -> raw
            is Class<*> -> if (Visualizer::class.java.isAssignableFrom(raw)) runCatching { raw.getDeclaredConstructor().newInstance() as Visualizer }.getOrNull() else null
            else -> null
        }?.takeIf { canVisualize(it) }
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
            is Array<*> -> value.joinToString()
            is Iterable<*> -> value.joinToString()
            else -> value.toString()
        },
        modifier = Modifier.widthIn(max = LocalOptionWidth.current),
        color = LocalTheme.current.textColorSecondary,
        fontSize = 14.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
    )
}

@Composable
fun BooleanOption(data: BooleanOptionData) {
    val checked = rememberPropValue(data.boolProp) == true
    when (data.style) {
        BooleanOptionData.Style.Switch -> SwitchControl(checked) { data.boolProp.set(it) }
        BooleanOptionData.Style.Checkbox -> CheckboxControl(checked) { data.boolProp.set(it) }
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

    TooltipArea(
        tooltip = {
            Box(
                modifier = Modifier
                    .background(theme.popupBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("More actions. Right-click option as shortcut.", color = theme.textColor, fontSize = 12.sp)
            }
        },
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
    val theme = LocalTheme.current
    val density = LocalDensity.current
    val isKeybind = remember(System.identityHashCode(prop)) { prop.get() is OneConfigKeybind }
    val effectiveReset = onReset ?: if (isKeybind) ({ resetKeybindOption(prop) }) else null
    val effectiveUnbind = onUnbind ?: if (isKeybind) ({ unbindKeybindOption(prop) }) else null
    val labelFont = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 13f)
    val labels = if (effectiveUnbind != null) listOf("Reset to default", "Unbind") else listOf("Reset to default")
    val itemWidth = labels.maxOf { 10f + 14f + 8f + labelFont.measureTextWidth(it) + 10f }.dp
    Popup(
        alignment = Alignment.TopStart,
        offset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        OpenTransition {
            Column(
                modifier = Modifier
                    .background(theme.popupBackground, theme.popupShape)
                    .border(1.dp, theme.borderColor, theme.popupShape)
                    .padding(ContextMenuPadding),
            ) {
                ContextMenuItem("refresh", "Reset to default", itemWidth, enabled = resetEnabled) {
                    if (effectiveReset != null) effectiveReset() else resetOption(prop)
                    onDismiss()
                }
                if (effectiveUnbind != null) {
                    ContextMenuItem("close", "Unbind", itemWidth, enabled = true) {
                        effectiveUnbind()
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(icon: String, label: String, width: Dp, enabled: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val color = if (enabled) theme.textColor else theme.textColorSecondary
    val hoverAlpha by animateFloatAsState(
        targetValue = if (enabled && isHovered) 0.10f else 0f,
        animationSpec = tween(120),
    )
    val itemShape = theme.popupShape.concentric(ContextMenuPadding)
    Row(
        modifier = Modifier
            .width(width)
            .background(theme.textColor.copy(alpha = hoverAlpha), itemShape)
            .then(
                if (enabled) {
                    Modifier.onClick(interactionSource, onClick)
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, color = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 13.sp)
    }
}

@Composable
fun <T> rememberPropValue(prop: Property<T>): T? {
    var value by remember(prop) { mutableStateOf(prop.shown()) }
    DisposableEffect(prop) {
        val listener = Predicate<T> { v -> value = v; false }
        prop.addCallback(listener)
        onDispose { prop.removeCallback(listener) }
    }
    return value
}

fun isInfoOption(prop: Property<*>) = when (val raw = prop.getMetadata<Any?>("visualizer")) {
    is Class<*> -> raw
    null -> null
    else -> raw.javaClass
} == Visualizer.InfoVisualizer::class.java

private fun canVisualize(vis: Visualizer): Boolean =
    vis.javaClass.enclosingClass == Visualizer::class.java || vis is PolyVisualizer || linksResolve(vis.javaClass)

private fun linksResolve(cls: Class<*>): Boolean = runCatching {
    val loader = cls.classLoader ?: return true
    val bytes = loader.getResourceAsStream(cls.name.substringBefore("$$").replace('.', '/') + ".class")?.use { it.readBytes() } ?: return false
    val d = DataInputStream(bytes.inputStream())
    d.skipBytes(8)
    val n = d.readUnsignedShort()
    val utf = arrayOfNulls<String>(n)
    val classes = ArrayList<Int>()
    var i = 1
    while (i < n) {
        when (d.readUnsignedByte()) {
            1 -> utf[i] = d.readUTF()
            7 -> classes += d.readUnsignedShort()
            8, 16, 19, 20 -> d.skipBytes(2)
            15 -> d.skipBytes(3)
            5, 6 -> { d.skipBytes(8); i++ }
            else -> d.skipBytes(4)
        }
        i++
    }
    classes.all { idx ->
        val name = utf[idx]!!.trimStart('[').removePrefix("L").removeSuffix(";").replace('/', '.')
        name.length == 1 || runCatching { Class.forName(name, false, loader) }.isSuccess
    }
}.getOrDefault(false)

internal fun enumClass(prop: Property<*>): Class<*>? = when {
    prop.type.isEnum -> prop.type
    prop.type.superclass?.isEnum == true -> prop.type.superclass
    else -> null
}

@Suppress("UNCHECKED_CAST")
internal fun Property<*>.write(value: Any) = (this as Property<Any>).set(value)

