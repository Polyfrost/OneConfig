package org.polyfrost.oneconfig.internal.ui.hud.screens

import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.OneConfigConfig

object HudDesignSession {
    private const val RESTORE_WINDOW_MULTIPLIER = 4f

    @JvmStatic
    fun restoreWindowMillis(): Long =
        (OneConfigConfig.timeBeforeReset * 1000f * RESTORE_WINDOW_MULTIPLIER).toLong()

    private var selection: Hud? = null

    private var panelOpen = false

    private var category = StudioCategory.Settings

    private var lastClosedAt = 0L

    fun save(selected: Hud?, panelOpen: Boolean, category: StudioCategory) {
        this.selection = selected
        this.panelOpen = panelOpen
        this.category = category
        this.lastClosedAt = System.currentTimeMillis()
    }

    fun forget(hud: Hud) {
        if (selection === hud) {
            selection = null
            panelOpen = false
        }
    }

    fun restoreSelection(): Hud? {
        if (!shouldRestore()) return null
        val hud = selection ?: return null
        if (hud !in HudManager.activeInstances) {
            selection = null
            return null
        }
        return hud
    }

    fun restorePanelOpen(): Boolean = panelOpen

    fun restoreCategory(): StudioCategory = category

    private fun shouldRestore(): Boolean = when (OneConfigConfig.openingBehavior) {
        2 -> true
        3 -> lastClosedAt > 0L && System.currentTimeMillis() - lastClosedAt <= restoreWindowMillis()
        else -> false
    }
}
