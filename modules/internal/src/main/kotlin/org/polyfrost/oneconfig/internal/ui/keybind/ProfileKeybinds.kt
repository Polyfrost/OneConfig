package org.polyfrost.oneconfig.internal.ui.keybind

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.ui.v1.keybind.BindNotInScreen
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.slf4j.LoggerFactory

object ProfileKeybinds {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/ProfileKeybinds")
    private const val FILE_NAME = "profile-keybinds"

    private val props = LinkedHashMap<String, Property<OneConfigKeybind>>()
    private val switching = AtomicBoolean()
    private var initialized = false

    private val listener = object : ConfigManager.ProfileChangeListener {
        override fun onProfileChanged(newProfile: String) {}

        override fun onProfileRenamed(oldProfile: String, newProfile: String) = rename(oldProfile, newProfile)

        override fun onProfileDeleted(profile: String) = remove(profile)
    }

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    @JvmStatic
    @Synchronized
    fun init() {
        if (initialized) return
        initialized = true
        try {
            val path = file()
            if (Files.exists(path)) {
                val existing = ConfigManager.profiles().toSet()
                Files.readAllLines(path, StandardCharsets.UTF_8).forEach { line ->
                    val parts = line.split('\t', limit = 4)
                    if (parts.size < 4) return@forEach
                    val profile = parts[3]
                    val mods = parts[0].toByteOrNull() ?: return@forEach
                    if (profile !in existing) return@forEach
                    props[profile] = create(profile, parseCodes(parts[1]), parseCodes(parts[2]), mods)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load profile keybinds", e)
        }
        ConfigManager.addProfileChangeListener(listener)
    }

    @Synchronized
    fun property(profile: String): Property<OneConfigKeybind> {
        init()
        return props.getOrPut(profile) { create(profile, null, null, 0) }
    }

    private fun parseCodes(raw: String): IntArray? =
        raw.split(',').mapNotNull { it.toIntOrNull() }.toIntArray().takeIf { it.isNotEmpty() }

    private fun create(profile: String, keys: IntArray?, mouse: IntArray?, mods: Byte): Property<OneConfigKeybind> {
        val bind = BindNotInScreen(keys, mouse, mods, 0L) { pressed ->
            if (pressed) switchTo(profile)
            true
        }
        KeybindManager.register(bind)
        val prop = Properties.simple<OneConfigKeybind>("profile:$profile", profile, null, bind, OneConfigKeybind::class.java)
        prop.addCallback(Predicate<OneConfigKeybind> {
            persist()
            false
        })
        return prop
    }

    @Synchronized
    private fun rename(oldProfile: String, newProfile: String) {
        val old = props.remove(oldProfile) ?: return
        val bind = old.get() ?: return
        KeybindManager.unregister(bind)
        props[newProfile] = create(newProfile, bind.keyCodes, bind.mouseBtns, bind.mods)
        persist()
    }

    @Synchronized
    private fun remove(profile: String) {
        val old = props.remove(profile) ?: return
        old.get()?.let { KeybindManager.unregister(it) }
        persist()
    }

    @Synchronized
    private fun persist() {
        try {
            val lines = props.mapNotNull { (profile, prop) ->
                val bind = prop.get()
                if (bind == null || !bind.isBound) return@mapNotNull null
                "${bind.mods}\t${bind.keyCodes?.joinToString(",").orEmpty()}\t${bind.mouseBtns?.joinToString(",").orEmpty()}\t$profile"
            }
            val path = file()
            Files.createDirectories(path.parent)
            Files.write(
                path,
                lines.joinToString("\n").toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to persist profile keybinds", e)
        }
    }

    private fun switchTo(profile: String) {
        if (ConfigManager.activeProfile() == profile) return
        if (!switching.compareAndSet(false, true)) return
        val thread = Thread({
            try {
                ConfigManager.openProfile(profile)
                if (OneConfigConfig.profileSwitchToast) {
                    Notifications.info("Profile switched", "Now using ${profile.ifEmpty { "Default" }}.")
                }
            } catch (t: Throwable) {
                LOGGER.error("Failed to switch to profile {}", profile, t)
                Notifications.error("Could not switch profile", t.message ?: t::class.java.simpleName)
            } finally {
                switching.set(false)
            }
        }, "OneConfig Profile Keybind")
        thread.isDaemon = true
        thread.start()
    }
}
