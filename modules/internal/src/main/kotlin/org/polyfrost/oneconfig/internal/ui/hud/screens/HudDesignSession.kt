package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.hud.resetAllHudProperties

internal sealed interface StudioCommand {
    data class Select(val huds: List<Hud>) : StudioCommand

    data object OpenSettings : StudioCommand

    data object Copy : StudioCommand

    data object Cut : StudioCommand

    data object Paste : StudioCommand

    data object Delete : StudioCommand

    data object SelectAll : StudioCommand

    data object Lock : StudioCommand
}

object HudDesignSession {
    private const val RESTORE_WINDOW_MULTIPLIER = 4f

    @JvmStatic
    fun restoreWindowMillis(): Long =
        (OneConfigConfig.timeBeforeReset * 1000f * RESTORE_WINDOW_MULTIPLIER).toLong()

    private var selection: List<Hud> = emptyList()

    private var panelOpen = false

    private var category = StudioCategory.Settings

    private var lastClosedAt = 0L

    /**
     * The HUDs currently selected in the design studio, kept up to date by the studio so global
     * keybinds (e.g. the duplicate HUD keybind) can act on it.
     */
    @Volatile
    var activeSelection: List<Hud> = emptyList()

    private val commandQueue = Channel<StudioCommand>(Channel.UNLIMITED)

    /**
     * Commands waiting for the design studio. The studio suspends on this while it is open, so a
     * posted command is handled the moment it arrives rather than on the next frame.
     */
    internal val commands: ReceiveChannel<StudioCommand> get() = commandQueue

    private fun post(command: StudioCommand) {
        commandQueue.trySend(command)
    }

    /**
     * Drops commands that were never consumed, e.g. ones posted while the studio was closing. They
     * are stale by the time it opens again.
     */
    internal fun clearCommands() {
        while (commandQueue.tryReceive().isSuccess) { /* drain */ }
    }

    /**
     * Action for the "Duplicate HUD" keybind. Returns `true` if a duplicate was created.
     */
    @JvmStatic
    fun handleDuplicate(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        val huds = activeSelection
        if (huds.isEmpty()) return false
        if (huds.any { it.locked }) return false
        if (huds.none(::canDuplicateHud)) return false
        val dups = duplicateHudGroup(huds)
        if (dups.isEmpty()) return false
        post(StudioCommand.Select(dups))
        return true
    }

    /**
     * Action for the "Open HUD Settings" keybind. Returns `true` if the settings panel will open.
     */
    @JvmStatic
    fun handleOpenSettings(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        val huds = activeSelection
        if (huds.size != 1) return false
        if (huds.first().locked) return false
        post(StudioCommand.OpenSettings)
        return true
    }

    /**
     * Action for the "Show/Hide HUD" keybind. Returns `true` if visibility was toggled.
     */
    @JvmStatic
    fun handleToggleVisibility(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        val huds = activeSelection
        if (huds.isEmpty()) return false
        Snapshot.withMutableSnapshot {
            val hide = huds.any { !it.hidden }
            huds.forEach { it.hidden = hide }
        }
        return true
    }

    /**
     * Action for the "Reset HUD to Default" keybind. Returns `true` if any HUD was reset.
     */
    @JvmStatic
    fun handleReset(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        val huds = activeSelection
        if (huds.isEmpty()) return false
        huds.forEach { resetAllHudProperties(it) }
        return true
    }

    /**
     * Action for the "Copy HUD" keybind. Returns `true` if the studio will copy its selection.
     */
    @JvmStatic
    fun handleCopy(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        if (activeSelection.isEmpty()) return false
        post(StudioCommand.Copy)
        return true
    }

    /**
     * Action for the "Cut HUD" keybind. Returns `true` if the studio will cut its selection.
     */
    @JvmStatic
    fun handleCut(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        if (activeSelection.isEmpty()) return false
        post(StudioCommand.Cut)
        return true
    }

    /**
     * Action for the "Paste HUD" keybind. Returns `true` if the studio will paste its clipboard.
     */
    @JvmStatic
    fun handlePaste(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        post(StudioCommand.Paste)
        return true
    }

    /**
     * Action for the "Delete HUD" keybind. Returns `true` if the studio will delete its selection.
     */
    @JvmStatic
    fun handleDelete(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        if (activeSelection.isEmpty()) return false
        post(StudioCommand.Delete)
        return true
    }

    /**
     * Action for the "Select All HUDs" keybind.
     */
    @JvmStatic
    fun handleSelectAll(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        post(StudioCommand.SelectAll)
        return true
    }

    /**
     * Action for the "Lock/Unlock HUD" keybind.
     */
    @JvmStatic
    fun handleLock(pressed: Boolean): Boolean {
        if (!pressed) return false
        if (!HudManager.isEditorOpen) return false
        if (activeSelection.isEmpty()) return false
        post(StudioCommand.Lock)
        return true
    }

    fun save(selected: List<Hud>, panelOpen: Boolean, category: StudioCategory) {
        this.selection = selected
        this.panelOpen = panelOpen
        this.category = category
        this.lastClosedAt = System.currentTimeMillis()
    }

    fun forget(hud: Hud) {
        val newSelection = selection - hud
        if (newSelection.size != selection.size) {
            selection = newSelection
            if (newSelection.isEmpty()) panelOpen = false
        }
    }

    fun restoreSelection(): List<Hud> {
        if (!shouldRestore()) return emptyList()
        val huds = selection.filter { it in HudManager.activeInstances }
        if (huds.size != selection.size) selection = huds
        return huds
    }

    fun restorePanelOpen(): Boolean = panelOpen

    fun restoreCategory(): StudioCategory = category

    private fun shouldRestore(): Boolean = when (OneConfigConfig.openingBehavior) {
        2 -> true
        3 -> lastClosedAt > 0L && System.currentTimeMillis() - lastClosedAt <= restoreWindowMillis()
        else -> false
    }
}
