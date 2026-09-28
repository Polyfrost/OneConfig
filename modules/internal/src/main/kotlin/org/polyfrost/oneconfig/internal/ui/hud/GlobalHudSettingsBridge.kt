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
    fun setEnabled(value: Boolean) = commit { OneConfigConfig.setGlobalHudOption("globalHudEnabled", value) }

    @JvmStatic
    fun setFontOverride(value: Boolean) = commit { OneConfigConfig.setGlobalHudOption("globalHudFontOverride", value) }

    @JvmStatic
    fun setFont(value: Font) = commit { OneConfigConfig.setGlobalHudOption("globalHudFont", value.ordinal) }

    @JvmStatic
    fun setTextWeightOverride(value: Boolean) = commit { OneConfigConfig.setGlobalHudOption("globalHudWeightOverride", value) }

    @JvmStatic
    fun setTextWeight(value: Weight) = commit { OneConfigConfig.setGlobalHudOption("globalHudWeight", value.ordinal) }

    @JvmStatic
    fun setShowBackgroundOverride(value: Boolean) = commit { OneConfigConfig.setGlobalHudOption("globalHudBackgroundOverride", value) }

    @JvmStatic
    fun setShowBackground(value: Boolean) = commit { OneConfigConfig.setGlobalHudOption("globalHudShowBackground", value) }

    @JvmStatic
    fun setBackgroundRadiusOverride(value: Boolean) = commit { OneConfigConfig.setGlobalHudOption("globalHudRadiusOverride", value) }

    @JvmStatic
    fun setBackgroundRadius(value: Float) = commit { OneConfigConfig.setGlobalHudOption("globalHudRadius", value) }

    /** Writes through the config [org.polyfrost.oneconfig.api.config.v1.Property], then saves */
    private inline fun commit(crossinline change: () -> Unit) {
        change()
        OneConfigConfig.INSTANCE?.save()
    }
}
