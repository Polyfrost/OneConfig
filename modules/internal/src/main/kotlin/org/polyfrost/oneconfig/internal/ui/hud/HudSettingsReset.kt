package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.optionHasDefault
import org.polyfrost.oneconfig.internal.ui.components.settings.resetOption

private val MenuPadding = 4.dp

private val hudUiEpochs = mutableStateMapOf<Hud, Int>()

private val STATIC_SIZE_OPTIONS = setOf("staticW", "staticH")
private val POSITION_OPTIONS = setOf("section", "relativeX", "relativeY")

/** Bumped when any HUD field is reset so bound [remember] state is recreated from the HUD */
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

/** Fixes invalid static dimensions such as after a bad reset and refreshes the settings UI */
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

/** Wraps a HUD settings control so right-click offers "Reset to default" for [optionId] */
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
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && !KeybindRecordingBus.isRecording) {
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

/** Recompose key for HUD panels whose controls use local [remember] copies of HUD fields */
@Composable
fun HudSettingsContent(hud: Hud, content: @Composable () -> Unit) {
    key(hud, hudUiEpoch(hud)) {
        content()
    }
}

private val DeleteMenuColor = androidx.compose.ui.graphics.Color(0xFFE5484D)

/**
 * Right-click menu on a HUD element in the design studio canvas
 *
 * Mirrors the hover action bar so the buttons stay reachable when HUDs are packed too tightly for the bar
 * to be aimed at
 */
@Composable
fun HudCanvasResetMenu(
    hud: Hud?,
    expanded: Boolean,
    offset: IntOffset,
    onDismiss: () -> Unit,
    targets: List<Hud> = emptyList(),
    onSettings: (Hud) -> Unit = {},
    settingsEnabled: Boolean = true,
    onCopy: (Hud) -> Unit = {},
    onCut: (Hud) -> Unit = {},
    onPaste: (Hud) -> Unit = {},
    pasteEnabled: Boolean = false,
    onDuplicate: (Hud) -> Unit = {},
    duplicateEnabled: Boolean = true,
    onAnchor: (Hud) -> Unit = {},
    anchorEnabled: Boolean = true,
    onDelete: (Hud) -> Unit = {},
) {
    if (hud == null || !expanded) return
    val theme = LocalTheme.current
    val actionTargets = targets.ifEmpty { listOf(hud) }
    val anyAnchored = actionTargets.any { it.isAnchored }
    val resetEnabled = actionTargets.any { hudHasResettableDefaults(it) }
    val deletable = actionTargets.any { it.canDelete() }
    val allHidden = actionTargets.all { it.hidden }
    val allLocked = actionTargets.all { it.locked }
    val showHints = OneConfigConfig.showKeybindHints
    val settingsHint = if (showHints) keybindHint(OneConfigConfig.hudSettingsKeybind) else null
    val visibilityHint = if (showHints) keybindHint(OneConfigConfig.hudVisibilityKeybind) else null
    val lockHint = if (showHints) keybindHint(OneConfigConfig.hudLockKeybind) else null
    val duplicateHint = if (showHints) keybindHint(OneConfigConfig.hudDuplicateKeybind) else null
    val resetHint = if (showHints) keybindHint(OneConfigConfig.hudResetKeybind) else null
    val copyHint = if (showHints) keybindHint(OneConfigConfig.hudCopyKeybind) else null
    val cutHint = if (showHints) keybindHint(OneConfigConfig.hudCutKeybind) else null
    val pasteHint = if (showHints) keybindHint(OneConfigConfig.hudPasteKeybind) else null
    val deleteHint = if (showHints) keybindHint(OneConfigConfig.hudDeleteKeybind) else null
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            // without this the separator's fillMaxWidth stretches the menu out to the whole screen
            modifier = Modifier
                .widthIn(min = 190.dp, max = 240.dp)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(MenuPadding),
        ) {
            HudMenuRow(
                icon = "settings",
                text = "Settings",
                enabled = settingsEnabled,
                hint = settingsHint,
            ) {
                onSettings(hud)
                onDismiss()
            }
            HudMenuRow(
                icon = if (allHidden) "eye-off" else "eye",
                text = if (allHidden) "Show" else "Hide",
                hint = visibilityHint,
            ) {
                Snapshot.withMutableSnapshot {
                    val hide = actionTargets.any { !it.hidden }
                    actionTargets.forEach { it.hidden = hide }
                }
                onDismiss()
            }
            HudMenuRow(
                icon = if (allLocked) "lock" else "unlock",
                text = if (allLocked) "Unlock" else "Lock",
                color = if (allLocked) Accent else null,
                hint = lockHint,
            ) {
                Snapshot.withMutableSnapshot {
                    val lock = actionTargets.any { !it.locked }
                    actionTargets.forEach { it.locked = lock }
                }
                onDismiss()
            }
            HudMenuRow(
                icon = "align",
                text = "Anchor",
                enabled = anchorEnabled,
            ) {
                onAnchor(hud)
                onDismiss()
            }
            if (anyAnchored) {
                HudMenuRow(icon = "x-circle", text = "Remove anchor") {
                    Snapshot.withMutableSnapshot {
                        actionTargets.forEach { it.clearAnchor() }
                    }
                    onDismiss()
                }
            }
            HudMenuDivider()
            HudMenuRow(icon = "copy", text = "Copy", hint = copyHint) {
                onCopy(hud)
                onDismiss()
            }
            HudMenuRow(icon = "scissors", text = "Cut", hint = cutHint) {
                onCut(hud)
                onDismiss()
            }
            HudMenuRow(
                icon = "clipboard",
                text = "Paste",
                enabled = pasteEnabled,
                hint = pasteHint,
            ) {
                onPaste(hud)
                onDismiss()
            }
            HudMenuRow(
                icon = "copy-plus",
                text = "Duplicate",
                enabled = duplicateEnabled,
                hint = duplicateHint,
            ) {
                onDuplicate(hud)
                onDismiss()
            }
            HudMenuDivider()
            HudMenuRow(
                icon = "refresh",
                text = "Reset all to default",
                enabled = resetEnabled,
                hint = resetHint,
            ) {
                actionTargets.forEach { resetAllHudProperties(it) }
                onDismiss()
            }
            if (deletable) {
                HudMenuRow(
                    icon = "trash",
                    text = "Delete HUD",
                    color = DeleteMenuColor,
                    hint = deleteHint,
                ) {
                    onDelete(hud)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun HudMenuDivider() {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(theme.borderColor)
    )
}

private fun keybindHint(keybind: OneConfigKeybind?): String? =
    keybind?.takeIf { it.isBound }?.displayName()

/** Context popup shown on empty canvas space when there is a HUD in the clipboard */
@Composable
fun HudCanvasPasteMenu(
    expanded: Boolean,
    offset: IntOffset,
    pasteEnabled: Boolean,
    onDismiss: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
) {
    if (!expanded) return
    val theme = LocalTheme.current
    val pasteHint = if (OneConfigConfig.showKeybindHints) keybindHint(OneConfigConfig.hudPasteKeybind) else null
    val selectAllHint = if (OneConfigConfig.showKeybindHints) keybindHint(OneConfigConfig.hudSelectAllKeybind) else null
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 190.dp, max = 240.dp)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(MenuPadding),
        ) {
            HudMenuRow(
                icon = "clipboard",
                text = "Paste",
                enabled = pasteEnabled,
                hint = pasteHint,
            ) {
                onPaste()
                onDismiss()
            }
            HudMenuRow(
                icon = "select-all",
                text = "Select All",
                enabled = true,
                hint = selectAllHint,
            ) {
                onSelectAll()
                onDismiss()
            }
        }
    }
}

@Composable
private fun HudMenuRow(
    icon: String,
    text: String,
    enabled: Boolean = true,
    color: Color? = null,
    hint: String? = null,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val rowColor = color ?: if (enabled) theme.textColor else theme.textColorSecondary
    val hintColor = if (enabled) theme.textColorSecondary else theme.textColorSecondary.copy(alpha = 0.5f)
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverBgColor by animateColorAsState(
        targetValue = if (enabled && isHovered) {
            theme.textColor.copy(alpha = 0.10f)
        } else {
            theme.textColor.copy(alpha = 0f)
        },
        animationSpec = tween(120),
        label = "hudMenuRowHover"
    )
    val itemShape = theme.popupShape.concentric(MenuPadding)
    Row(
        modifier = Modifier
            .clip(itemShape)
            .background(hoverBgColor, itemShape)
            .then(
                if (enabled) {
                    Modifier.onClick(interactionSource, onClick = onClick)
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
        Icon(icon, color = rowColor, modifier = Modifier.size(14.dp))
        Text(text, color = rowColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (hint != null) {
            Text(hint, color = hintColor, fontSize = 10.sp)
        }
    }
}
