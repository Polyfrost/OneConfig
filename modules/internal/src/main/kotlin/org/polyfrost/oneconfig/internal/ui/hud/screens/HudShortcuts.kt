package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.ArrowKeys
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.DialogTitle
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.KeyCap
import org.polyfrost.oneconfig.api.ui.v1.components.ModalDialog
import org.polyfrost.oneconfig.api.ui.v1.components.MouseGesture
import org.polyfrost.oneconfig.api.ui.v1.components.MouseGlyph
import org.polyfrost.oneconfig.api.ui.v1.components.Text
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.translated

private sealed interface ShortcutInput {
    class Key(val label: String) : ShortcutInput
    data object Arrows : ShortcutInput
    class Mouse(val gesture: MouseGesture, val label: String) : ShortcutInput
}

private class HudShortcut(val action: String, val inputs: List<ShortcutInput>)

private class HudShortcutGroup(val title: String, val shortcuts: List<HudShortcut>)

internal val hudActionKeyName: String
    get() = if (DesktopHelper.isMac) "Cmd" else "Ctrl"

internal fun hudShortcutsKeyName(): String? =
    listOf(OneConfigConfig.hudShortcutsKeybind, OneConfigConfig.hudShortcutsAltKeybind)
        .firstOrNull { it != null && it.isBound }
        ?.displayName()

private fun shortcutText(key: String, fallback: String) = translated("oneconfig.hud.shortcuts.$key", fallback)

private fun splitKeys(name: String): List<String> = name.split("+").map { it.trim() }.filter { it.isNotEmpty() }

private fun keys(vararg labels: String): List<ShortcutInput> = labels.map { ShortcutInput.Key(it) }

private fun keybindInputs(keybind: OneConfigKeybind?): List<ShortcutInput>? {
    val name = keybind?.takeIf { it.isBound }?.displayName() ?: return null
    return splitKeys(name).map { ShortcutInput.Key(it) }
}

private fun bound(keybind: OneConfigKeybind?, action: String): HudShortcut? =
    keybindInputs(keybind)?.let { HudShortcut(action, it) }

private fun hudShortcutGroups(): Pair<List<HudShortcutGroup>, List<HudShortcutGroup>> {
    val action = hudActionKeyName
    val click = ShortcutInput.Mouse(MouseGesture.Click, shortcutText("gesture.click", "Click"))
    val drag = ShortcutInput.Mouse(MouseGesture.Drag, shortcutText("gesture.drag", "Drag"))
    val moving = HudShortcutGroup(
        shortcutText("moving", "Moving"),
        listOf(
            HudShortcut(shortcutText("nudge", "Nudge by 1 pixel"), listOf(ShortcutInput.Arrows)),
            HudShortcut(shortcutText("nudge_far", "Nudge by 10 pixels"), keys("Shift") + ShortcutInput.Arrows),
            HudShortcut(shortcutText("no_snap", "Move without snapping"), keys("Alt") + drag),
            HudShortcut(shortcutText("detach", "Pull out of a merged group"), keys(action) + drag),
            HudShortcut(
                shortcutText("free_resize", "Resize width and height freely"),
                keys("Shift") + ShortcutInput.Mouse(MouseGesture.Drag, shortcutText("gesture.drag_corner", "Drag corner")),
            ),
            HudShortcut(shortcutText("grid", "Show or hide the grid"), keys("G")),
            HudShortcut(shortcutText("vanilla", "Show or hide vanilla outlines"), keys("V")),
        ),
    )
    val selecting = HudShortcutGroup(
        shortcutText("selecting", "Selecting"),
        listOfNotNull(
            HudShortcut(
                shortcutText("marquee", "Select several HUDs"),
                listOf(ShortcutInput.Mouse(MouseGesture.Drag, shortcutText("gesture.drag_empty", "Drag empty space"))),
            ),
            HudShortcut(shortcutText("add_select", "Add to the selection"), keys("Shift") + click),
            HudShortcut(shortcutText("toggle_select", "Add or remove one HUD"), keys(action) + click),
            HudShortcut(
                shortcutText("measure", "Measure the gap to a HUD"),
                keys("Alt") + ShortcutInput.Mouse(MouseGesture.Hover, shortcutText("gesture.hover", "Hover")),
            ),
            bound(OneConfigConfig.hudSelectAllKeybind, shortcutText("select_all", "Select all")),
            HudShortcut(shortcutText("deselect", "Clear the selection"), keys("Esc")),
        ),
    )
    val editing = HudShortcutGroup(
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
    return listOf(moving, selecting) to listOf(editing).filter { it.shortcuts.isNotEmpty() }
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
private fun MouseInput(input: ShortcutInput.Mouse) {
    val theme = OneConfigTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        MouseGlyph(input.gesture)
        Text(input.label, color = theme.textColorSecondary, style = theme.typography.caption, maxLines = 1)
    }
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
private fun ShortcutInputs(inputs: List<ShortcutInput>) {
    val theme = OneConfigTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        inputs.forEachIndexed { i, input ->
            if (i > 0) Text("+", color = theme.textColorSecondary, style = theme.typography.caption)
            when (input) {
                is ShortcutInput.Key -> KeyCap(input.label)
                ShortcutInput.Arrows -> ArrowKeys()
                is ShortcutInput.Mouse -> MouseInput(input)
            }
        }
    }
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
private fun ShortcutRow(shortcut: HudShortcut, divider: Boolean) {
    val theme = OneConfigTheme.current
    Column {
        if (divider) Spacer(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor.copy(alpha = 0.5f)))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(shortcut.action, Modifier.weight(1f), color = theme.textColor, style = theme.typography.small, maxLines = 2)
            ShortcutInputs(shortcut.inputs)
        }
    }
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
private fun ShortcutColumn(groups: List<HudShortcutGroup>, modifier: Modifier) {
    val theme = OneConfigTheme.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        for (group in groups) {
            Column {
                Text(
                    group.title.uppercase(),
                    Modifier.padding(bottom = 6.dp),
                    color = theme.textColorSecondary,
                    style = theme.typography.eyebrow,
                    maxLines = 1,
                )
                group.shortcuts.forEachIndexed { i, shortcut -> ShortcutRow(shortcut, divider = i > 0) }
            }
        }
    }
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
internal fun HudShortcutsPill(keyName: String?) {
    val theme = OneConfigTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(shortcutText("pill", "Keyboard shortcuts"), color = theme.textColorSecondary, style = theme.typography.caption)
        if (keyName != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                splitKeys(keyName).forEach { KeyCap(it) }
            }
        }
    }
}

@OptIn(ExperimentalOneConfigUiApi::class)
@Composable
internal fun HudShortcutsDialog(onDismiss: () -> Unit) {
    val (left, right) = remember { hudShortcutGroups() }
    ModalDialog(
        onDismiss,
        width = 820.dp,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogTitle(translated("oneconfig.hud.shortcuts.title", "Keyboard shortcuts"), Modifier.weight(1f))
                IconButton("close", translated("oneconfig.hud.shortcuts.close", "Close"), onDismiss, size = ControlSize.Compact)
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            ShortcutColumn(left, Modifier.weight(1f))
            ShortcutColumn(right, Modifier.weight(1f))
        }
    }
}
