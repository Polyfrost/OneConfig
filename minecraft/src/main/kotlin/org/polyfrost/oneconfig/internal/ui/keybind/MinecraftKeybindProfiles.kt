package org.polyfrost.oneconfig.internal.ui.keybind

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshotStore
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import java.util.concurrent.TimeUnit

object MinecraftKeybindProfiles : ConfigManager.ProfileChangeListener {
    private const val NAMESPACE = "controls"
    private const val SHARED_NAMESPACE = "shared-controls"
    private const val CLIENT_THREAD_TIMEOUT_SECONDS = 30L
    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/MC-Keybind-Profiles")
    private val store = CompatSnapshotStore("minecraft-keybinds.json")
    private val modeTransitions = MinecraftKeybindModeTransitions(
        saveProfile = { profile ->
            capture(profile)
            store.flushOrThrow(profile)
        },
        restoreProfile = { profile -> apply(profile) },
        saveShared = {
            capture("", SHARED_NAMESPACE)
            store.flushOrThrow("")
        },
        restoreShared = { apply("", SHARED_NAMESPACE) },
    )

    @Volatile
    private var initialized = false

    @JvmStatic
    @Synchronized
    fun init() {
        if (initialized) return
        val profile = ConfigManager.activeProfile()
        val separateControls = ConfigManager.profileSpecificControls()
        try {
            runOnClientThreadAndWait {
                modeTransitions.initialize(profile, separateControls)
            }
        } catch (failure: Throwable) {
            LOGGER.warn("Failed to initialize Minecraft keybind profiles", failure)
            return
        }
        ConfigManager.addProfileChangeListener(this)
        // Publish initialized only after the live owner and its initial snapshot are both ready.
        initialized = true
    }

    override fun onProfileChanged(newProfile: String) {
        runCatching {
            runOnClientThreadAndWait {
                modeTransitions.profileChanged(newProfile)
            }
        }.onFailure { LOGGER.warn("Failed to switch Minecraft keybinds to profile '{}'", newProfile, it) }
    }

    override fun onProfileSaving(profile: String) {
        val owner = modeTransitions.liveOwner() ?: return
        if (!owner.separateControls) {
            if (store.hasLoadFailure("")) {
                LOGGER.warn("Leaving the shared controls snapshot unreadable instead of overwriting it")
            } else {
                runOnClientThreadAndWait { capture("", SHARED_NAMESPACE) }
                store.flushOrThrow("")
            }
            if (profile.isNotEmpty()) {
                if (store.hasLoadFailure(profile)) {
                    LOGGER.warn(
                        "Leaving profile '{}' without rewriting its unreadable Minecraft controls snapshot",
                        profile,
                    )
                } else {
                    store.flushOrThrow(profile)
                }
            }
            return
        }
        if (store.hasLoadFailure(profile)) {
            LOGGER.warn(
                "Continuing the profile operation without rewriting unreadable Minecraft controls snapshot '{}'",
                profile,
            )
            return
        }
        if (profile == owner.profile) runOnClientThreadAndWait { capture(profile) }
        store.flushOrThrow(profile)
    }

    override fun onProfileCreated(profile: String) {
        store.deleteProfile(profile)
        runOnClientThreadAndWait { modeTransitions.profileCreated(profile) }
    }

    override fun onProfileSpecificControlsChanged(enabled: Boolean) {
        runOnClientThreadAndWait {
            modeTransitions.change(enabled)
        }
    }

    override fun onProfileRenamed(oldProfile: String, newProfile: String) {
        runOnClientThreadAndWait {
            // Keep the cache remap ordered with Options.save callbacks queued by earlier listeners.
            store.renameProfile(oldProfile, newProfile)
            modeTransitions.profileRenamed(oldProfile, newProfile)
        }
    }

    override fun onProfileDeleted(profile: String) {
        runOnClientThreadAndWait {
            // A queued Options.save may still target the outgoing identity. Delete its cache only
            // after those earlier client-thread callbacks have completed.
            store.deleteProfile(profile)
            modeTransitions.profileDeleted(profile)
        }
    }

    @JvmStatic
    fun onOptionsSaved() {
        if (!initialized) return
        val captureSaved: () -> Unit = {
            runCatching {
                modeTransitions.captureSavedOptions { owner -> captureLiveControls(owner, false) }
            }
                .onFailure { LOGGER.warn("Failed to capture saved Minecraft keybinds", it) }
            Unit
        }
        val mc = Minecraft.getInstance() ?: return
        if (mc.isSameThread) captureSaved() else mc.execute(captureSaved)
    }

    @JvmStatic
    fun shutdown() {
        if (!initialized) return
        runCatching {
            runOnClientThreadAndWait {
                modeTransitions.captureSavedOptions { owner -> captureLiveControls(owner, true) }
            }
        }.onFailure { LOGGER.warn("Failed to flush Minecraft keybind profiles during shutdown", it) }
    }

    private fun runOnClientThreadAndWait(action: () -> Unit) {
        val mc = Minecraft.getInstance()
        ConfigManager.dispatchAndWait(
            { task -> if (mc == null || mc.isSameThread) task.run() else mc.execute(task) },
            action,
            TimeUnit.SECONDS.toNanos(CLIENT_THREAD_TIMEOUT_SECONDS),
            "the client thread",
        )
    }

    private fun capture(profile: String, namespace: String = NAMESPACE) {
        for (mapping in MinecraftKeybindProvider.managedMappings()) {
            store.putValue(profile, namespace, mapping.name, mapping.saveString())
        }
    }

    private fun captureLiveControls(owner: MinecraftKeybindLiveOwner, flush: Boolean) {
        val profile = if (owner.separateControls) owner.profile else ""
        val namespace = if (owner.separateControls) NAMESPACE else SHARED_NAMESPACE
        capture(profile, namespace)
        if (flush) store.flushOrThrow(profile)
    }

    private fun apply(profile: String, namespace: String = NAMESPACE) {
        val snapshot = store.load(profile)[namespace] ?: return
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
