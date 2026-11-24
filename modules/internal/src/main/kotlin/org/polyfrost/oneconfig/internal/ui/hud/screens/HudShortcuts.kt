package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.MouseGesture
import org.polyfrost.oneconfig.api.ui.v1.components.Shortcut
import org.polyfrost.oneconfig.api.ui.v1.components.ShortcutGroup
import org.polyfrost.oneconfig.api.ui.v1.components.ShortcutHint
import org.polyfrost.oneconfig.api.ui.v1.components.ShortcutInput
import org.polyfrost.oneconfig.api.ui.v1.components.ShortcutSheetDialog
import org.polyfrost.oneconfig.api.ui.v1.components.shortcutKeys
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.translated

internal val hudActionKeyName: String
    get() = if (DesktopHelper.isMac) "Cmd" else "Ctrl"

internal fun hudShortcutsKeyName(): String? =
    listOf(OneConfigConfig.hudShortcutsKeybind, OneConfigConfig.hudShortcutsAltKeybind)
        .firstOrNull { it != null && it.isBound }
        ?.displayName()

private fun shortcutText(key: String, fallback: String) = translated("oneconfig.hud.shortcuts.$key", fallback)

@OptIn(ExperimentalOneConfigUiApi::class)
private fun keys(vararg labels: String): List<ShortcutInput> = labels.map { ShortcutInput.Key(it) }

@OptIn(ExperimentalOneConfigUiApi::class)
private fun bound(keybind: OneConfigKeybind?, action: String): Shortcut? {
    val name = keybind?.takeIf { it.isBound }?.displayName() ?: return null
    return Shortcut(action, shortcutKeys(name))
}

@OptIn(ExperimentalOneConfigUiApi::class)
private fun hudShortcutGroups(): List<ShortcutGroup> {
    val action = hudActionKeyName
    val click = ShortcutInput.Mouse(MouseGesture.Click, shortcutText("gesture.click", "Click"))
    val drag = ShortcutInput.Mouse(MouseGesture.Drag, shortcutText("gesture.drag", "Drag"))
    val moving = ShortcutGroup(
        shortcutText("moving", "Moving"),
        listOf(
            Shortcut(shortcutText("nudge", "Nudge by 1 pixel"), listOf(ShortcutInput.Arrows)),
            Shortcut(shortcutText("nudge_far", "Nudge by 10 pixels"), keys("Shift") + ShortcutInput.Arrows),
            Shortcut(shortcutText("no_snap", "Move without snapping"), keys("Alt") + drag),
            Shortcut(shortcutText("detach", "Pull out of a merged group"), keys(action) + drag),
            Shortcut(
                shortcutText("free_resize", "Resize width and height freely"),
                keys("Shift") + ShortcutInput.Mouse(MouseGesture.Drag, shortcutText("gesture.drag_corner", "Drag corner")),
            ),
            Shortcut(shortcutText("grid", "Show or hide the grid"), keys("G")),
            Shortcut(shortcutText("vanilla", "Show or hide vanilla outlines"), keys("V")),
        ),
    )
    val selecting = ShortcutGroup(
        shortcutText("selecting", "Selecting"),
        listOfNotNull(
            Shortcut(
                shortcutText("marquee", "Select several HUDs"),
                listOf(ShortcutInput.Mouse(MouseGesture.Drag, shortcutText("gesture.drag_empty", "Drag empty space"))),
            ),
            Shortcut(shortcutText("add_select", "Add to the selection"), keys("Shift") + click),
            Shortcut(shortcutText("toggle_select", "Add or remove one HUD"), keys(action) + click),
            Shortcut(
                shortcutText("measure", "Measure the gap to a HUD"),
                keys("Alt") + ShortcutInput.Mouse(MouseGesture.Hover, shortcutText("gesture.hover", "Hover")),
            ),
            bound(OneConfigConfig.hudSelectAllKeybind, shortcutText("select_all", "Select all")),
            Shortcut(shortcutText("deselect", "Clear the selection"), keys("Esc")),
        ),
    )
    val editing = ShortcutGroup(
        shortcutText("editing", "Editing"),
        listOfNotNull(
            bound(OneConfigConfig.hudSettingsKeybind, shortcutText("settings", "Open settings")),
            bound(OneConfigConfig.hudVisibilityKeybind, shortcutText("toggle_hide", "Show or hide in game")),
            bound(OneConfigConfig.hudLockKeybind, shortcutText("toggle_lock", "Lock or unlock")),
            bound(OneConfigConfig.hudCopyKeybind, translated("oneconfig.menu.text.copy", "Copy")),
            bound(OneConfigConfig.hudCutKeybind, translated("oneconfig.menu.text.cut", "Cut")),
            bound(OneConfigConfig.hudPasteKeybind, translated("oneconfig.menu.text.paste", "Paste")),
            bound(OneConfigConfig.hudDuplicateKeybind, translated("oneconfig.hud.menu.duplicate", "Duplicate")),
            bound(OneConfigConfig.hudResetKeybind, translated("oneconfig.hud.menu.reset_position", "Reset position")),
            bound(OneConfigConfig.hudDeleteKeybind, translated("oneconfig.hud.menu.delete", "Delete HUD")),
            bound(OneConfigConfig.hudUndoKeybind, translated("oneconfig.hud.menu.undo", "Undo")),
            bound(OneConfigConfig.hudRedoKeybind, translated("oneconfig.hud.menu.redo", "Redo")),
            bound(OneConfigConfig.hudShortcutsKeybind, shortcutText("open_sheet", "Show these shortcuts")),
        ),
    )
    return listOf(moving, selecting, editing)
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
internal fun HudShortcutsPill(keyName: String?) {
    ShortcutHint(shortcutText("pill", "Keyboard shortcuts"), keyName?.let { shortcutKeys(it) }.orEmpty())
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
internal fun HudShortcutsDialog(onDismiss: () -> Unit) {
    val groups = remember { hudShortcutGroups() }
    ShortcutSheetDialog(groups, onDismiss)
}
