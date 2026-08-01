package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.optionHasDefault
import org.polyfrost.oneconfig.internal.ui.components.settings.resetOption

private val MenuPadding = 4.dp

private val hudUiEpochs = mutableStateMapOf<Hud, Int>()

private val STATIC_SIZE_OPTIONS = setOf("staticW", "staticH")
private val POSITION_OPTIONS = setOf("section", "relativeX", "relativeY")

/** Bumped when any HUD field is reset so bound [remember] state is recreated from the HUD. */
fun hudUiEpoch(hud: Hud): Int = hudUiEpochs[hud] ?: 0

private fun bumpHudUiEpoch(hud: Hud) {
    hudUiEpochs[hud] = (hudUiEpochs[hud] ?: 0) + 1
}

fun hudProperty(hud: Hud, optionId: String): Property<*>? =
    hud.tree?.getProp(optionId)

fun resetHudProperty(hud: Hud, optionId: String) {
    val prop = hudProperty(hud, optionId) ?: return
    resetOption(prop)
    finishHudReset(hud)
}

fun hudHasResettableDefaults(hud: Hud): Boolean {
    val tree = hud.tree ?: return false
    return treeHasResettableDefaults(tree)
}

private fun treeHasResettableDefaults(tree: Tree): Boolean {
    for (node in tree.map.values) {
        when (node) {
            is Property<*> -> if (optionHasDefault(node)) return true
            is Tree -> if (treeHasResettableDefaults(node)) return true
        }
    }
    return false
}

fun resetAllHudProperties(hud: Hud) {
    val tree = hud.tree ?: return
    resetHudStaticSize(hud)
    resetTreeDefaults(hud, tree)
    finishHudReset(hud)
}

private fun resetHudStaticSize(hud: Hud) {
    if (!hud.staticWidth) return
    for (id in STATIC_SIZE_OPTIONS) {
        val prop = hudProperty(hud, id) ?: continue
        performHudOptionReset(hud, prop, id)
    }
}

fun hudHasPositionDefaults(hud: Hud): Boolean =
    POSITION_OPTIONS.any { id ->
        hudProperty(hud, id)?.let { optionHasDefault(it) } == true
    }

fun resetHudPosition(hud: Hud) {
    var reset = false
    for (id in POSITION_OPTIONS) {
        val prop = hudProperty(hud, id) ?: continue
        if (!optionHasDefault(prop)) continue
        resetOption(prop)
        reset = true
    }
    if (!reset) return
    bumpHudUiEpoch(hud)
    hud.updateAndRecalculate()
}

private fun finishHudReset(hud: Hud) {
    hud.reseedStaticSizeIfNeeded()
    hud.captureStaticSizeDefaults()
    bumpHudUiEpoch(hud)
    hud.updateAndRecalculate()
}

private fun canResetHudOption(hud: Hud, prop: Property<*>, optionId: String): Boolean {
    if (optionHasDefault(prop)) return true
    return hud.staticWidth && optionId in STATIC_SIZE_OPTIONS
}

private fun performHudOptionReset(hud: Hud, prop: Property<*>, optionId: String) {
    when (optionId) {
        "staticW" -> {
            if (optionHasDefault(prop)) resetOption(prop) else hud.reseedStaticWidth()
        }
        "staticH" -> {
            if (optionHasDefault(prop)) resetOption(prop) else hud.reseedStaticHeight()
        }
        else -> resetOption(prop)
    }
    hud.captureStaticSizeDefaults(force = optionId in STATIC_SIZE_OPTIONS)
}

/** Fixes invalid static dimensions (e.g. after a bad reset) and refreshes the settings UI. */
fun repairHudStaticSize(hud: Hud) {
    if (!hud.staticWidth) return
    if (hud.staticW > 0f && hud.staticH > 0f) {
        hud.captureStaticSizeDefaults()
        return
    }
    hud.reseedStaticSizeIfNeeded()
    hud.captureStaticSizeDefaults(force = true)
    bumpHudUiEpoch(hud)
}

private fun resetTreeDefaults(hud: Hud, tree: Tree) {
    for (node in tree.map.values) {
        when (node) {
            is Property<*> -> {
                val optionId = node.id ?: node.getID()
                if (optionId !in STATIC_SIZE_OPTIONS && optionHasDefault(node)) resetOption(node)
            }
            is Tree -> resetTreeDefaults(hud, node)
        }
    }
}

/**
 * Wraps a HUD designer/settings control so right-click offers "Reset to default" for [optionId].
 */
@Composable
fun HudSettingTarget(
    hud: Hud,
    optionId: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val prop = remember(hud, optionId) { hudProperty(hud, optionId) }
    if (prop == null) {
        Box(modifier) { content() }
        return
    }

    var menuOpen by remember(prop) { mutableStateOf(false) }
    var menuOffset by remember(prop) { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = modifier.pointerInput(prop) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val pos = event.changes.first().position
                        menuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                        menuOpen = true
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        },
    ) {
        val description = prop.localizedDescription()?.takeUnless { it.isEmptyText() }
        if (description != null) {
            Tooltip(
                text = {
                    Text(description, color = LocalTheme.current.textColor, fontSize = 12.sp)
                },
                modifier = Modifier.widthIn(max = 260.dp),
                anchor = Alignment.TopCenter,
            ) {
                content()
            }
        } else {
            content()
        }
        OptionContextMenu(
            prop = prop,
            expanded = menuOpen,
            offset = menuOffset,
            onDismiss = { menuOpen = false },
            resetEnabled = canResetHudOption(hud, prop, optionId),
            onReset = {
                performHudOptionReset(hud, prop, optionId)
                finishHudReset(hud)
            },
        )
    }
}

/** Recompose key for HUD panels whose controls use local [remember] copies of HUD fields. */
@Composable
fun HudSettingsContent(hud: Hud, content: @Composable () -> Unit) {
    key(hud, hudUiEpoch(hud)) {
        content()
    }
}

private val DeleteMenuColor = androidx.compose.ui.graphics.Color(0xFFE5484D)

@Composable
private fun HudMenuItem(
    icon: String,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    Row(
        modifier = Modifier
            .clip(theme.popupShape.concentric(MenuPadding))
            .then(
                if (enabled) {
                    Modifier.onClick(interactionSource) { onClick() }.pointerHoverIcon(PointerIcon.Hand)
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

/**
 * Right-click menu on a HUD element in the design studio canvas. Mirrors the hover action bar so the
 * buttons stay reachable when HUDs are packed too tightly for the bar to be aimed at.
 */
@Composable
fun HudCanvasResetMenu(
    hud: Hud?,
    expanded: Boolean,
    offset: IntOffset,
    onDismiss: () -> Unit,
    onSettings: (Hud) -> Unit = {},
    onDelete: (Hud) -> Unit = {},
) {
    if (hud == null || !expanded) return
    val theme = LocalTheme.current
    val enabled = hudHasResettableDefaults(hud)
    val deletable = hud.deletable()
    val isHidden = hud.hidden
    val isLocked = hud.locked
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            // without this the separator's fillMaxWidth stretches the menu to the popup constraints,
            // which are the whole screen
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(MenuPadding),
        ) {
            HudMenuItem("settings", "Settings", theme.textColor) {
                UiSounds.play(UiSoundEvent.HUD_SELECT)
                onSettings(hud)
                onDismiss()
            }
            HudMenuItem(
                if (isHidden) "eye-off" else "eye",
                if (isHidden) "Show HUD" else "Hide HUD",
                theme.textColor,
            ) {
                UiSounds.play(UiSoundEvent.CLICK)
                Snapshot.withMutableSnapshot { hud.hidden = !hud.hidden }
                onDismiss()
            }
            HudMenuItem(
                if (isLocked) "lock" else "unlock",
                if (isLocked) "Unlock HUD" else "Lock HUD",
                if (isLocked) Accent else theme.textColor,
            ) {
                UiSounds.play(UiSoundEvent.CLICK)
                Snapshot.withMutableSnapshot { hud.locked = !hud.locked }
                onDismiss()
            }
            Box(
                modifier = Modifier
                    .padding(vertical = MenuPadding)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(theme.borderColor),
            )
            HudMenuItem(
                "refresh",
                "Reset all to default",
                if (enabled) theme.textColor else theme.textColorSecondary,
                enabled = enabled,
            ) {
                resetAllHudProperties(hud)
                onDismiss()
            }
            if (deletable) {
                HudMenuItem("trash", "Delete HUD", DeleteMenuColor) {
                    onDelete(hud)
                    onDismiss()
                }
            }
        }
    }
}
