@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import kotlin.math.abs
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.Icon
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuCustomItem
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuIconRow
import org.polyfrost.oneconfig.api.ui.v1.components.MenuItem
import org.polyfrost.oneconfig.api.ui.v1.components.MenuList
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.components.settings.identitySet
import org.polyfrost.oneconfig.internal.ui.components.settings.optionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.optionHasDefault
import org.polyfrost.oneconfig.internal.ui.components.settings.rememberOptionMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.resetOption
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudAlign
import org.polyfrost.oneconfig.internal.ui.hud.screens.alignHuds
import org.polyfrost.oneconfig.internal.ui.hud.screens.tourString
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val MenuIconSize = 14.dp

private val hudUiEpochs = mutableStateMapOf<Hud, Int>()

private val STATIC_SIZE_OPTIONS = setOf("staticW", "staticH")
private val POSITION_OPTIONS = setOf(
    "section", "relativeX", "relativeY",
    "anchorTargetId", "anchorPoint", "selfAnchorPoint", "anchorOffsetX", "anchorOffsetY", "movedByPlayer",
)

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
    return treeHasResettableDefaults(tree, root = true)
}

private fun treeHasResettableDefaults(tree: Tree, root: Boolean): Boolean {
    for (node in tree.map.values) {
        when (node) {
            is Property<*> -> if (!(root && (node.id ?: node.getID()) in POSITION_OPTIONS) && optionHasDefault(node)) return true
            is Tree -> if (treeHasResettableDefaults(node, root = false)) return true
        }
    }
    return false
}

fun resetAllHudProperties(hud: Hud) {
    val tree = hud.tree ?: return
    resetHudStaticSize(hud)
    resetTreeDefaults(tree, root = true)
    finishHudReset(hud)
}

private fun resetHudStaticSize(hud: Hud) {
    if (!hud.staticWidth) return
    for (id in STATIC_SIZE_OPTIONS) {
        val prop = hudProperty(hud, id) ?: continue
        performHudOptionReset(hud, prop, id)
    }
}

fun hudHasPositionDefaults(hud: Hud): Boolean = hud.defaultPosition() != null

/** Counts the HUD's position too when [position] is set */
fun hudOptionsCanReset(hud: Hud, optionIds: Collection<String>, position: Boolean = false): Boolean =
    (position && hudCanResetPosition(hud)) || optionIds.any { id ->
        val prop = hudProperty(hud, id) ?: return@any false
        val default = prop.getMetadata<Any?>("default") ?: return@any false
        prop.get() != default
    }

/** Resets the HUD's position too when [position] is set */
fun resetHudOptions(hud: Hud, optionIds: Collection<String>, position: Boolean = false) {
    for (id in optionIds) {
        val prop = hudProperty(hud, id) ?: continue
        if (optionHasDefault(prop)) performHudOptionReset(hud, prop, id)
    }
    if (position && hudHasPositionDefaults(hud)) resetPositionOptions(hud)
    finishHudReset(hud)
}

/** Whether [hud] sits on its default position, or has none to compare against */
private fun hudIsAtDefaultPosition(hud: Hud): Boolean {
    val section = hudProperty(hud, "section")?.getMetadata<Any?>("default") ?: return true
    val x = (hudProperty(hud, "relativeX")?.getMetadata<Any?>("default") as? Number)?.toFloat() ?: return true
    val y = (hudProperty(hud, "relativeY")?.getMetadata<Any?>("default") as? Number)?.toFloat() ?: return true
    return section == hud.section && abs(x - hud.relativeX) < 1e-4f && abs(y - hud.relativeY) < 1e-4f
}

/** Whether [resetHudPosition] would move [hud] or drop its anchor right now */
fun hudCanResetPosition(hud: Hud): Boolean =
    hudHasPositionDefaults(hud) && (hud.isAnchored || hud.isMergeAnchored || !hudIsAtDefaultPosition(hud))

fun resetHudPosition(hud: Hud) {
    if (!resetPositionOptions(hud)) return
    bumpHudUiEpoch(hud)
    hud.updateAndRecalculate()
}

private fun resetPositionOptions(hud: Hud): Boolean {
    val props = POSITION_OPTIONS.mapNotNull { id -> hudProperty(hud, id)?.takeIf(::optionHasDefault) }
    if (props.isEmpty()) return false
    hud.dropMergeLinks()
    for (prop in props) resetOption(prop)
    return true
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
        else -> {
            if (optionId in POSITION_OPTIONS) hud.dropMergeLinks()
            resetOption(prop)
        }
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

private fun resetTreeDefaults(tree: Tree, root: Boolean, seen: MutableSet<Property<*>> = identitySet()) {
    for (node in tree.map.values) {
        when (node) {
            is Property<*> -> {
                val optionId = node.id ?: node.getID()
                val builtIn = root && (optionId in STATIC_SIZE_OPTIONS || optionId in POSITION_OPTIONS)
                if (!builtIn && optionHasDefault(node)) resetOption(node, seen)
            }
            is Tree -> resetTreeDefaults(node, root = false, seen = seen)
        }
    }
}

/**
 * Wraps a HUD settings control so right-click offers "Reset to default" for [optionId] and the [linkedIds] the
 * control also sets
 */
@Composable
fun HudSettingTarget(
    hud: Hud,
    optionId: String,
    vararg linkedIds: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val prop = remember(hud, optionId) { hudProperty(hud, optionId) }
    val linked = remember(hud, linkedIds.toList()) {
        linkedIds.mapNotNull { id -> hudProperty(hud, id)?.let { id to it } }
    }
    if (prop == null) {
        Box(modifier) { content() }
        return
    }

    val menu = rememberOptionMenu(
        prop,
        onReset = {
            performHudOptionReset(hud, prop, optionId)
            for ((id, p) in linked) performHudOptionReset(hud, p, id)
            finishHudReset(hud)
        },
        resetEnabled = canResetHudOption(hud, prop, optionId) ||
            linked.any { (id, p) -> canResetHudOption(hud, p, id) },
    )

    Box(modifier = modifier.optionContextMenu(menu)) {
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
    }
}

/** A [HudSettingTarget] for a control that moves [hud], so right-click resets its whole position */
@Composable
fun HudPositionTarget(hud: Hud, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val linked = if (hudHasPositionDefaults(hud)) (POSITION_OPTIONS - "section").toTypedArray() else emptyArray()
    HudSettingTarget(hud, "section", *linked, modifier = modifier, content = content)
}

/** Recompose key for HUD panels whose controls use local [remember] copies of HUD fields */
@Composable
fun HudSettingsContent(hud: Hud, content: @Composable () -> Unit) {
    key(hud, hudUiEpoch(hud)) {
        content()
    }
}

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
    preview: Boolean = false,
    onMeasured: (IntSize) -> Unit = {},
) {
    if (hud == null || !expanded) return
    val entries = buildHudCanvasMenuEntries(
        hud = hud,
        targets = targets,
        onSettings = {},
        settingsEnabled = true,
        onCopy = {},
        onCut = {},
        onPaste = {},
        pasteEnabled = false,
        onDuplicate = {},
        duplicateEnabled = true,
        onAnchor = {},
        anchorEnabled = true,
        onDelete = {},
        onUndo = {},
        undoEnabled = false,
        onRedo = {},
        redoEnabled = false,
        highlightAnchor = preview,
    )
    ShellPopup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = !preview, dismissOnClickOutside = !preview),
    ) {
        MenuList(entries, onDismiss, Modifier.onSizeChanged(onMeasured).blockInteraction(preview, keyboard = true))
    }
}

internal fun hudCanvasMenuEntries(
    hud: Hud,
    targets: List<Hud>,
    onSettings: (Hud) -> Unit,
    settingsEnabled: Boolean,
    onCopy: (Hud) -> Unit,
    onCut: (Hud) -> Unit,
    onPaste: (Hud) -> Unit,
    pasteEnabled: Boolean,
    onDuplicate: (Hud) -> Unit,
    duplicateEnabled: Boolean,
    onAnchor: (Hud) -> Unit,
    anchorEnabled: Boolean,
    onDelete: (Hud) -> Unit,
    onUndo: () -> Unit,
    undoEnabled: Boolean,
    onRedo: () -> Unit,
    redoEnabled: Boolean,
): List<MenuEntry> = buildHudCanvasMenuEntries(
    hud = hud,
    targets = targets,
    onSettings = onSettings,
    settingsEnabled = settingsEnabled,
    onCopy = onCopy,
    onCut = onCut,
    onPaste = onPaste,
    pasteEnabled = pasteEnabled,
    onDuplicate = onDuplicate,
    duplicateEnabled = duplicateEnabled,
    onAnchor = onAnchor,
    anchorEnabled = anchorEnabled,
    onDelete = onDelete,
    onUndo = onUndo,
    undoEnabled = undoEnabled,
    onRedo = onRedo,
    redoEnabled = redoEnabled,
    highlightAnchor = false,
)

private fun buildHudCanvasMenuEntries(
    hud: Hud,
    targets: List<Hud>,
    onSettings: (Hud) -> Unit,
    settingsEnabled: Boolean,
    onCopy: (Hud) -> Unit,
    onCut: (Hud) -> Unit,
    onPaste: (Hud) -> Unit,
    pasteEnabled: Boolean,
    onDuplicate: (Hud) -> Unit,
    duplicateEnabled: Boolean,
    onAnchor: (Hud) -> Unit,
    anchorEnabled: Boolean,
    onDelete: (Hud) -> Unit,
    onUndo: () -> Unit,
    undoEnabled: Boolean,
    onRedo: () -> Unit,
    redoEnabled: Boolean,
    highlightAnchor: Boolean,
): List<MenuEntry> {
    val actionTargets = targets.ifEmpty { listOf(hud) }
    val allHidden = actionTargets.all { it.hidden }
    val allLocked = actionTargets.all { it.locked }
    val entries = ArrayList<MenuEntry>(24)

    entries += MenuAction(
        translated("oneconfig.hud.menu.settings", "Settings"),
        { onSettings(hud) },
        icon = "settings",
        enabled = settingsEnabled,
        shortcutHint = keybindHint(OneConfigConfig.hudSettingsKeybind),
    )
    entries += MenuAction(
        if (allHidden) translated("oneconfig.hud.menu.show", "Show") else translated("oneconfig.hud.menu.hide", "Hide"),
        {
            Snapshot.withMutableSnapshot {
                val hide = actionTargets.any { !it.hidden }
                actionTargets.forEach { it.hidden = hide }
            }
        },
        icon = if (allHidden) "eye-off" else "eye",
        shortcutHint = keybindHint(OneConfigConfig.hudVisibilityKeybind),
    )
    val lockIcon = if (allLocked) "lock" else "unlock"
    entries += MenuAction(
        if (allLocked) translated("oneconfig.hud.menu.unlock", "Unlock") else translated("oneconfig.hud.menu.lock", "Lock"),
        {
            Snapshot.withMutableSnapshot {
                val lock = actionTargets.any { !it.locked }
                actionTargets.forEach { it.locked = lock }
            }
        },
        icon = lockIcon,
        shortcutHint = keybindHint(OneConfigConfig.hudLockKeybind),
        leading = if (allLocked) accentMenuIcon(lockIcon) else null,
    )
    entries += MenuIconRow(
        HudAlign.entries.map { align ->
            MenuAction(alignLabel(align), { alignHuds(actionTargets, align) }, icon = align.icon)
        },
    )
    val anchorLabel = translated("oneconfig.hud.menu.anchor", "Anchor")
    entries += if (highlightAnchor) {
        MenuCustomItem { MenuItem(anchorLabel, {}, icon = "align", enabled = anchorEnabled, selected = true) }
    } else {
        MenuAction(anchorLabel, { onAnchor(hud) }, icon = "align", enabled = anchorEnabled)
    }
    if (actionTargets.any { it.isAnchored }) {
        entries += MenuAction(
            translated("oneconfig.hud.menu.remove_anchor", "Remove anchor"),
            {
                Snapshot.withMutableSnapshot {
                    actionTargets.forEach { it.clearAnchor() }
                }
            },
            icon = "x-circle",
        )
    }

    entries += MenuSeparator
    entries += MenuAction(
        translated("oneconfig.menu.text.copy", "Copy"),
        { onCopy(hud) },
        icon = "copy",
        shortcutHint = keybindHint(OneConfigConfig.hudCopyKeybind),
    )
    entries += MenuAction(
        translated("oneconfig.menu.text.cut", "Cut"),
        { onCut(hud) },
        icon = "scissors",
        shortcutHint = keybindHint(OneConfigConfig.hudCutKeybind),
    )
    entries += pasteAction({ onPaste(hud) }, pasteEnabled)
    entries += MenuAction(
        translated("oneconfig.hud.menu.duplicate", "Duplicate"),
        { onDuplicate(hud) },
        icon = "copy-plus",
        enabled = duplicateEnabled,
        shortcutHint = keybindHint(OneConfigConfig.hudDuplicateKeybind),
    )

    entries += MenuSeparator
    entries += undoAction(onUndo, undoEnabled)
    entries += redoAction(onRedo, redoEnabled)

    entries += MenuSeparator
    if (actionTargets.any(::hudHasPositionDefaults)) {
        entries += MenuAction(
            translated("oneconfig.hud.menu.reset_position", "Reset position"),
            { actionTargets.filter { hudCanResetPosition(it) }.forEach(::resetHudPosition) },
            icon = "move",
            enabled = actionTargets.any { hudCanResetPosition(it) },
        )
    }
    entries += MenuAction(
        translated("oneconfig.hud.menu.reset_settings", "Reset HUD settings"),
        { actionTargets.forEach { resetAllHudProperties(it) } },
        icon = "refresh",
        enabled = actionTargets.any { hudHasResettableDefaults(it) },
        shortcutHint = keybindHint(OneConfigConfig.hudResetKeybind),
    )
    if (actionTargets.any { it.canDelete() }) {
        entries += MenuAction(
            translated("oneconfig.hud.menu.delete", "Delete HUD"),
            { onDelete(hud) },
            icon = "trash",
            shortcutHint = keybindHint(OneConfigConfig.hudDeleteKeybind),
            danger = true,
        )
    }
    return entries
}

internal fun hudCanvasPasteMenuEntries(
    pasteEnabled: Boolean,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    onUndo: () -> Unit,
    undoEnabled: Boolean,
    onRedo: () -> Unit,
    redoEnabled: Boolean,
    onTour: (() -> Unit)?,
): List<MenuEntry> {
    val entries = ArrayList<MenuEntry>(8)
    entries += undoAction(onUndo, undoEnabled)
    entries += redoAction(onRedo, redoEnabled)
    entries += MenuSeparator
    entries += pasteAction(onPaste, pasteEnabled)
    entries += MenuAction(
        translated("oneconfig.menu.text.select_all", "Select all"),
        onSelectAll,
        icon = "select-all",
        shortcutHint = keybindHint(OneConfigConfig.hudSelectAllKeybind),
    )
    if (onTour != null) {
        entries += MenuSeparator
        entries += MenuAction(tourString("menu"), onTour, icon = "info")
    }
    return entries
}

private fun pasteAction(onPaste: () -> Unit, enabled: Boolean): MenuAction = MenuAction(
    translated("oneconfig.menu.text.paste", "Paste"),
    onPaste,
    icon = "clipboard",
    enabled = enabled,
    shortcutHint = keybindHint(OneConfigConfig.hudPasteKeybind),
)

private fun undoAction(onUndo: () -> Unit, enabled: Boolean): MenuAction = MenuAction(
    translated("oneconfig.hud.menu.undo", "Undo"),
    onUndo,
    icon = "undo",
    enabled = enabled,
    shortcutHint = keybindHint(OneConfigConfig.hudUndoKeybind),
)

private fun redoAction(onRedo: () -> Unit, enabled: Boolean): MenuAction = MenuAction(
    translated("oneconfig.hud.menu.redo", "Redo"),
    onRedo,
    icon = "redo",
    enabled = enabled,
    shortcutHint = keybindHint(OneConfigConfig.hudRedoKeybind),
)

private fun alignLabel(align: HudAlign): String =
    translated("oneconfig.hud.align.${align.name.lowercase()}", align.label)

private fun accentMenuIcon(icon: String): @Composable () -> Unit = {
    Icon(icon, null, Modifier.size(MenuIconSize), Accent)
}

private fun keybindHint(keybind: OneConfigKeybind?): String? =
    if (OneConfigConfig.showKeybindHints) keybind?.takeIf { it.isBound }?.displayName() else null
