package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onSizeChanged
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.IntOffset
import org.polyfrost.compose.ui.unit.IntSize
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.optionHasDefault
import org.polyfrost.oneconfig.internal.ui.components.settings.resetOption
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudAlign
import org.polyfrost.oneconfig.internal.ui.hud.screens.alignHuds
import org.polyfrost.oneconfig.internal.ui.hud.screens.tourString
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.poly.OpenTransition
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val MenuPadding = 4.dp

private val hudUiEpochs = mutableStateMapOf<Hud, Int>()

private val STATIC_SIZE_OPTIONS = setOf("staticW", "staticH")
private val POSITION_OPTIONS = setOf("section", "relativeX", "relativeY")

/** Bumped when any HUD field is reset so bound [remember] state is recreated from the HUD */
fun hudUiEpoch(hud: Hud): Int = hudUiEpochs[hud] ?: 0

internal fun bumpHudUiEpoch(hud: Hud) {
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

@Composable
fun HudSettingTarget(
    hud: Hud,
    vararg optionIds: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val props = remember(hud, *optionIds) { optionIds.mapNotNull { id -> hudProperty(hud, id)?.let { id to it } } }
    val prop = props.firstOrNull()?.second
    if (prop == null) {
        Box(modifier) { content() }
        return
    }

    var menuOpen by remember(System.identityHashCode(prop)) { mutableStateOf(false) }
    var menuOffset by remember(System.identityHashCode(prop)) { mutableStateOf(DpOffset.Zero) }

    Box(
        modifier = modifier.onPointerEvent { event ->
            if (event.type == PointerEventType.Press && event.button == 1 && !KeybindRecordingBus.isRecording) {
                menuOffset = DpOffset((event.x - event.node.x).dp, (event.y - event.node.y).dp)
                menuOpen = true
                true
            } else {
                false
            }
        },
    ) {
        val theme = LocalTheme.current
        val description = remember(prop) { prop.localizedDescription()?.takeUnless { it.isEmptyText() } }
        if (description != null) {
            TooltipArea(
                tooltip = {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .background(theme.popupBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(description, color = theme.textColor, fontSize = 12.sp, softWrap = true)
                    }
                },
            ) {
                content()
            }
        } else {
            content()
        }
        if (menuOpen) {
            val font = theme.typography.family.resolve(FontWeight.Normal)
            Popup(offset = menuOffset, onDismissRequest = { menuOpen = false }) {
                Column(
                    modifier = Modifier
                        .background(theme.popupBackground, theme.popupShape)
                        .border(1.dp, theme.borderColor, theme.popupShape)
                        .padding(MenuPadding),
                ) {
                    HudMenuRow(
                        icon = "refresh",
                        text = "Reset to default",
                        width = menuRowWidth(font, "Reset to default", null).dp,
                        enabled = props.any { (id, p) -> canResetHudOption(hud, p, id) },
                    ) {
                        UiSounds.play(UiSoundEvent.CLICK)
                        for ((id, p) in props) if (canResetHudOption(hud, p, id)) performHudOptionReset(hud, p, id)
                        finishHudReset(hud)
                        menuOpen = false
                    }
                }
            }
        }
    }
}

/** Recompose key for HUD panels whose controls use local [remember] copies of HUD fields */
@Composable
fun HudSettingsContent(hud: Hud, content: @Composable () -> Unit) {
    key(hud, hudUiEpoch(hud)) {
        content()
    }
}

private val DeleteMenuColor = Color(0xFFE5484D)

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
    onUndo: () -> Unit = {},
    undoEnabled: Boolean = false,
    onRedo: () -> Unit = {},
    redoEnabled: Boolean = false,
    preview: Boolean = false,
    onMeasured: (IntSize) -> Unit = {},
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
    val undoHint = if (showHints) keybindHint(OneConfigConfig.hudUndoKeybind) else null
    val redoHint = if (showHints) keybindHint(OneConfigConfig.hudRedoKeybind) else null
    val font = theme.typography.family.resolve(FontWeight.Normal)
    val rowWidth = listOf(
        "Settings" to settingsHint,
        (if (allHidden) "Show" else "Hide") to visibilityHint,
        (if (allLocked) "Unlock" else "Lock") to lockHint,
        "Anchor" to null,
        "Remove anchor" to null,
        "Copy" to copyHint,
        "Cut" to cutHint,
        "Paste" to pasteHint,
        "Duplicate" to duplicateHint,
        "Undo" to undoHint,
        "Redo" to redoHint,
        "Reset all to default" to resetHint,
        "Delete HUD" to deleteHint,
    ).maxOf { (text, hint) -> menuRowWidth(font, text, hint) }.coerceIn(190f, 240f).dp
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopStart,
        offset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = !preview, dismissOnClickOutside = !preview),
    ) {
        OpenTransition(
            modifier = Modifier
                .onSizeChanged(onMeasured)
                .blockInteraction(preview)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(MenuPadding),
        ) {
            Column {
                HudMenuRow(
                    icon = "settings",
                    text = "Settings",
                    width = rowWidth,
                    enabled = settingsEnabled,
                    hint = settingsHint,
                ) {
                    onDismiss()
                    onSettings(hud)
                }
                HudMenuRow(
                    icon = if (allHidden) "eye-off" else "eye",
                    text = if (allHidden) "Show" else "Hide",
                    width = rowWidth,
                    hint = visibilityHint,
                ) {
                    onDismiss()
                    UiSounds.play(UiSoundEvent.CLICK)
                    Snapshot.withMutableSnapshot {
                        val hide = actionTargets.any { !it.hidden }
                        actionTargets.forEach { it.hidden = hide }
                    }
                }
                HudMenuRow(
                    icon = if (allLocked) "lock" else "unlock",
                    text = if (allLocked) "Unlock" else "Lock",
                    width = rowWidth,
                    color = if (allLocked) Accent else null,
                    hint = lockHint,
                ) {
                    onDismiss()
                    UiSounds.play(UiSoundEvent.CLICK)
                    Snapshot.withMutableSnapshot {
                        val lock = actionTargets.any { !it.locked }
                        actionTargets.forEach { it.locked = lock }
                    }
                }
                HudAlignRow(rowWidth) { align ->
                    onDismiss()
                    UiSounds.play(UiSoundEvent.CLICK)
                    alignHuds(actionTargets, align)
                }
                Box(
                    modifier = if (preview) {
                        Modifier
                            .background(Accent.copy(alpha = 0.18f), theme.buttonShape)
                            .border(1.dp, Accent, theme.buttonShape)
                    } else {
                        Modifier
                    },
                ) {
                    HudMenuRow(
                        icon = "align",
                        text = "Anchor",
                        width = rowWidth,
                        enabled = anchorEnabled,
                    ) {
                        onDismiss()
                        onAnchor(hud)
                    }
                }
                if (anyAnchored) {
                    HudMenuRow(icon = "x-circle", text = "Remove anchor", width = rowWidth) {
                        onDismiss()
                        Snapshot.withMutableSnapshot {
                            actionTargets.forEach { it.clearAnchor() }
                        }
                    }
                }
                HudMenuDivider(rowWidth)
                HudMenuRow(icon = "copy", text = "Copy", width = rowWidth, hint = copyHint) {
                    onDismiss()
                    onCopy(hud)
                }
                HudMenuRow(icon = "scissors", text = "Cut", width = rowWidth, hint = cutHint) {
                    onDismiss()
                    onCut(hud)
                }
                HudMenuRow(
                    icon = "clipboard",
                    text = "Paste",
                    width = rowWidth,
                    enabled = pasteEnabled,
                    hint = pasteHint,
                ) {
                    onDismiss()
                    onPaste(hud)
                }
                HudMenuRow(
                    icon = "copy-plus",
                    text = "Duplicate",
                    width = rowWidth,
                    enabled = duplicateEnabled,
                    hint = duplicateHint,
                ) {
                    onDismiss()
                    onDuplicate(hud)
                }
                HudMenuDivider(rowWidth)
                HudMenuRow(icon = "undo", text = "Undo", width = rowWidth, enabled = undoEnabled, hint = undoHint) {
                    onDismiss()
                    onUndo()
                }
                HudMenuRow(icon = "redo", text = "Redo", width = rowWidth, enabled = redoEnabled, hint = redoHint) {
                    onDismiss()
                    onRedo()
                }
                HudMenuDivider(rowWidth)
                HudMenuRow(
                    icon = "refresh",
                    text = "Reset all to default",
                    width = rowWidth,
                    enabled = resetEnabled,
                    hint = resetHint,
                ) {
                    onDismiss()
                    actionTargets.forEach { resetAllHudProperties(it) }
                }
                if (deletable) {
                    HudMenuRow(
                        icon = "trash",
                        text = "Delete HUD",
                        width = rowWidth,
                        color = DeleteMenuColor,
                        hint = deleteHint,
                    ) {
                        onDismiss()
                        onDelete(hud)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMenuDivider(width: Dp) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(width)
            .height(1.dp)
            .background(theme.borderColor)
    )
}

private fun keybindHint(keybind: OneConfigKeybind?): String? =
    keybind?.takeIf { it.isBound }?.displayName()

@Composable
fun HudCanvasPasteMenu(
    expanded: Boolean,
    offset: IntOffset,
    pasteEnabled: Boolean,
    onDismiss: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    onUndo: () -> Unit = {},
    undoEnabled: Boolean = false,
    onRedo: () -> Unit = {},
    redoEnabled: Boolean = false,
    onTour: (() -> Unit)? = null,
) {
    if (!expanded) return
    val theme = LocalTheme.current
    val pasteHint = if (OneConfigConfig.showKeybindHints) keybindHint(OneConfigConfig.hudPasteKeybind) else null
    val selectAllHint = if (OneConfigConfig.showKeybindHints) keybindHint(OneConfigConfig.hudSelectAllKeybind) else null
    val undoHint = if (OneConfigConfig.showKeybindHints) keybindHint(OneConfigConfig.hudUndoKeybind) else null
    val redoHint = if (OneConfigConfig.showKeybindHints) keybindHint(OneConfigConfig.hudRedoKeybind) else null
    val font = theme.typography.family.resolve(FontWeight.Normal)
    val rows = listOf("Undo" to undoHint, "Redo" to redoHint, "Paste" to pasteHint, "Select All" to selectAllHint)
    val rowWidth = (if (onTour != null) rows + (tourString("menu") to null) else rows)
        .maxOf { (text, hint) -> menuRowWidth(font, text, hint) }.coerceIn(190f, 240f).dp
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopStart,
        offset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        OpenTransition(
            modifier = Modifier
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(MenuPadding),
        ) {
            Column {
                HudMenuRow(icon = "undo", text = "Undo", width = rowWidth, enabled = undoEnabled, hint = undoHint) {
                    onDismiss()
                    onUndo()
                }
                HudMenuRow(icon = "redo", text = "Redo", width = rowWidth, enabled = redoEnabled, hint = redoHint) {
                    onDismiss()
                    onRedo()
                }
                HudMenuDivider(rowWidth)
                HudMenuRow(
                    icon = "clipboard",
                    text = "Paste",
                    width = rowWidth,
                    enabled = pasteEnabled,
                    hint = pasteHint,
                ) {
                    onDismiss()
                    onPaste()
                }
                HudMenuRow(
                    icon = "select-all",
                    text = "Select All",
                    width = rowWidth,
                    enabled = true,
                    hint = selectAllHint,
                ) {
                    onDismiss()
                    onSelectAll()
                }
                if (onTour != null) {
                    HudMenuDivider(rowWidth)
                    HudMenuRow(icon = "info", text = tourString("menu"), width = rowWidth) {
                        onDismiss()
                        onTour()
                    }
                }
            }
        }
    }
}

private fun menuRowWidth(font: String, text: String, hint: String?): Float =
    10f + 14f + 8f + PolyFont(font, 13f).measureTextWidth(text) + (hint?.let { 8f + PolyFont(font, 10f).measureTextWidth(it) } ?: 0f) + 10f

@Composable
private fun HudMenuRow(
    icon: String,
    text: String,
    width: Dp,
    enabled: Boolean = true,
    color: Color? = null,
    hint: String? = null,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val rowColor = color ?: if (enabled) theme.textColor else theme.textColorSecondary
    val hintColor = if (enabled) theme.textColorSecondary else theme.textColorSecondary.copy(alpha = theme.textColorSecondary.alphaF * 0.5f)
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverAlpha by animateFloatAsState(
        targetValue = if (enabled && isHovered) 0.10f else 0f,
        animationSpec = tween(120),
    )
    val itemShape = theme.popupShape.concentric(MenuPadding)
    Row(
        modifier = Modifier
            .width(width)
            .background(theme.textColor.copy(alpha = hoverAlpha), itemShape)
            .then(
                if (enabled) {
                    Modifier.clickable(interactionSource, indication = null, onClick = onClick)
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

@Composable
private fun HudAlignRow(width: Dp, onAlign: (HudAlign) -> Unit) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (align in HudAlign.entries) {
            val interactionSource = rememberInteractionSource()
            val isHovered by interactionSource.collectIsHoveredAsState()
            val hoverAlpha by animateFloatAsState(if (isHovered) 0.10f else 0f, tween(120))
            TooltipArea(
                tooltip = {
                    Box(
                        modifier = Modifier
                            .background(theme.popupBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(align.label, color = theme.textColor, fontSize = 12.sp)
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(theme.textColor.copy(alpha = hoverAlpha), RoundedCornerShape(4.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(interactionSource, indication = null) { onAlign(align) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(align.icon, theme.textColor, Modifier.size(14.dp), rotation = align.rotation.toFloat())
                }
            }
        }
    }
}
