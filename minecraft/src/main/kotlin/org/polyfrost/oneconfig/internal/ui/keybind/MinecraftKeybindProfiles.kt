package org.polyfrost.oneconfig.internal.ui.keybind

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshotStore
import org.polyfrost.oneconfig.api.config.v1.ConfigManager

object MinecraftKeybindProfiles : ConfigManager.ProfileChangeListener {
    private const val NAMESPACE = "controls"
    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/MC-Keybind-Profiles")
    private val store = CompatSnapshotStore("minecraft-keybinds.json")

    @Volatile
    private var currentProfile: String = ""

    @Volatile
    private var initialized = false

    @JvmStatic
    fun init() {
        if (initialized) return
        initialized = true
        currentProfile = ConfigManager.activeProfile()
        runCatching { capture(currentProfile) }
            .onFailure { LOGGER.warn("Failed to capture baseline Minecraft keybinds", it) }
        ConfigManager.addProfileChangeListener(this)
    }

    override fun onProfileChanged(newProfile: String) {
        val old = currentProfile
        currentProfile = newProfile
        if (old == newProfile) return
        val mc = Minecraft.getInstance() ?: return
        mc.execute {
            runCatching {
                capture(old)
                store.flush(old)
                apply(newProfile)
            }.onFailure { LOGGER.warn("Failed to switch Minecraft keybinds to profile '{}'", newProfile, it) }
        }
    }

    private fun capture(profile: String) {
        for (mapping in MinecraftKeybindProvider.managedMappings()) {
            store.putValue(profile, NAMESPACE, mapping.name, mapping.saveString())
        }
    }

    private fun apply(profile: String) {
        val snapshot = store.load(profile)[NAMESPACE] ?: return
        var changed = false
        for (mapping in MinecraftKeybindProvider.managedMappings()) {
            val saved = snapshot[mapping.name] as? String ?: continue
            val key = runCatching { InputConstants.getKey(saved) }.getOrDefault(InputConstants.UNKNOWN)
            mapping.setKey(key)
            changed = true
        }
        if (changed) {
            Minecraft.getInstance()?.options?.save()
            runCatching { KeyMapping.resetMapping() }
        }
    }
}
