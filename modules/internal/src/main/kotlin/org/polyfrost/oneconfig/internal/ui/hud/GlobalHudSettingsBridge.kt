package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.GlobalHudSettings
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.internal.OneConfigConfig

/**
 * Keeps [GlobalHudSettings] and the persisted "Global HUD" fields on [OneConfigConfig] in sync:
 * the preferences screen writes the config fields and resyncs through [syncFromConfig], while the
 * HUD editor panel writes through the setters here
 */
object GlobalHudSettingsBridge {
    /** Copies every persisted field into the live settings; runs on load and on any config change */
    @JvmStatic
    fun syncFromConfig() {
        Snapshot.withMutableSnapshot {
            GlobalHudSettings.enabled = OneConfigConfig.globalHudEnabled
            GlobalHudSettings.overrideFont = OneConfigConfig.globalHudFontOverride
            GlobalHudSettings.font = Font.entries.getOrElse(OneConfigConfig.globalHudFont) { Font.Minecraft }
            GlobalHudSettings.overrideTextWeight = OneConfigConfig.globalHudWeightOverride
            GlobalHudSettings.textWeight = Weight.entries.getOrElse(OneConfigConfig.globalHudWeight) { Weight.Regular }
            GlobalHudSettings.overrideShowBackground = OneConfigConfig.globalHudBackgroundOverride
            GlobalHudSettings.showBackground = OneConfigConfig.globalHudShowBackground
            GlobalHudSettings.overrideBackgroundRadius = OneConfigConfig.globalHudRadiusOverride
            GlobalHudSettings.backgroundRadius = OneConfigConfig.globalHudRadius
        }
    }

    @JvmStatic
    fun setEnabled(value: Boolean) = commit {
        OneConfigConfig.globalHudEnabled = value
        GlobalHudSettings.enabled = value
    }

    @JvmStatic
    fun setFontOverride(value: Boolean) = commit {
        OneConfigConfig.globalHudFontOverride = value
        GlobalHudSettings.overrideFont = value
    }

    @JvmStatic
    fun setFont(value: Font) = commit {
        OneConfigConfig.globalHudFont = value.ordinal
        GlobalHudSettings.font = value
    }

    @JvmStatic
    fun setTextWeightOverride(value: Boolean) = commit {
        OneConfigConfig.globalHudWeightOverride = value
        GlobalHudSettings.overrideTextWeight = value
    }

    @JvmStatic
    fun setTextWeight(value: Weight) = commit {
        OneConfigConfig.globalHudWeight = value.ordinal
        GlobalHudSettings.textWeight = value
    }

    @JvmStatic
    fun setShowBackgroundOverride(value: Boolean) = commit {
        OneConfigConfig.globalHudBackgroundOverride = value
        GlobalHudSettings.overrideShowBackground = value
    }

    @JvmStatic
    fun setShowBackground(value: Boolean) = commit {
        OneConfigConfig.globalHudShowBackground = value
        GlobalHudSettings.showBackground = value
    }

    @JvmStatic
    fun setBackgroundRadiusOverride(value: Boolean) = commit {
        OneConfigConfig.globalHudRadiusOverride = value
        GlobalHudSettings.overrideBackgroundRadius = value
    }

    @JvmStatic
    fun setBackgroundRadius(value: Float) = commit {
        OneConfigConfig.globalHudRadius = value
        GlobalHudSettings.backgroundRadius = value
    }

    /** Applies [change] to both stores in one snapshot and persists the preferences */
    private inline fun commit(crossinline change: () -> Unit) {
        Snapshot.withMutableSnapshot { change() }
        OneConfigConfig.INSTANCE?.save()
    }
}
