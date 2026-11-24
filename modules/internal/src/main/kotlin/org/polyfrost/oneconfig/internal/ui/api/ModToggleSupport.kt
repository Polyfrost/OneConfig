package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.NO_MASTER_SWITCH_KEY
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** What the user set for an option whose value is overwritten while its mod is switched off */
class HeldValue(val value: Any?)

const val HELD_VALUE = "heldValue"

/** The value to show for this option, which stays the user's own while the mod is switched off */
@Suppress("UNCHECKED_CAST")
fun <T> Property<T>.shown(): T? {
    val held = getMetadata<HeldValue>(HELD_VALUE) ?: return get()
    return held.value as T?
}

/** Root level option names treated as a mod's master switch, compared without case or an "is" prefix */
private val MASTER_SWITCH_NAMES = setOf("enabled", "enable", "modenabled")

private fun masterSwitchName(id: String): String =
    id.filter(Char::isLetterOrDigit).lowercase().let { if (it.startsWith("is")) it.substring(2) else it }

fun isMasterSwitchName(name: String): Boolean = masterSwitchName(name) in MASTER_SWITCH_NAMES

const val MASTER_TOGGLE = "masterToggle"

const val NO_MASTER_SWITCH = NO_MASTER_SWITCH_KEY

const val LOCK_EXEMPT = "lockExempt"

fun Property<*>.isMarkedSwitch(): Boolean = getMetadata<Boolean>(MASTER_TOGGLE) == true

fun Node.isLockExempt(): Boolean = getMetadata<Boolean>(LOCK_EXEMPT) == true

fun Tree.findMarkedSwitch(): Property<*>? {
    for (node in map.values) {
        val found = when (node) {
            is Property<*> -> node.takeIf { it.getMetadata<Boolean>(MASTER_TOGGLE) == true }
            is Tree -> node.findMarkedSwitch()
            else -> null
        }
        if (found != null) return found
    }
    return null
}

/**
 * Finds the boolean option that switches the whole mod on and off
 *
 * An option marked with `@MasterToggle` is used wherever it sits in the tree. Without one, only direct
 * children of the tree are matched by name, so the switch of a single feature or category (which
 * lives in a child tree) is never mistaken for the mod's own
 */
@Suppress("UNCHECKED_CAST")
fun Tree.findMasterSwitch(): Property<Boolean>? {
    findMarkedSwitch()?.let { return it as Property<Boolean> }
    if (getMetadata<Boolean>(NO_MASTER_SWITCH) == true) return null
    val candidates = map.values.filterIsInstance<Property<*>>().filter {
        (it.type == Boolean::class.javaPrimitiveType || it.type == Boolean::class.javaObjectType) &&
            it.getMetadata<Boolean>(NO_MASTER_SWITCH) != true
    }
    for (name in MASTER_SWITCH_NAMES) {
        candidates.firstOrNull { masterSwitchName(it.id ?: "") == name }?.let { return it as Property<Boolean> }
    }
    return null
}

class PropertyModToggle(val property: Property<Boolean>) : ModToggle {
    override fun isEnabled(): Boolean = property.get() != false

    override fun setEnabled(enabled: Boolean) {
        property.set(enabled)
    }
}

fun ModToggle?.locksOptions(): Boolean = this != null && (this !is PropertyModToggle || property.isMarkedSwitch())

object ModToggleStore : ModToggles.Store, ConfigManager.ProfileChangeListener {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/ModToggles")
    private const val FILE_NAME = "disabled-mods"
    private val PROFILE_SWITCH_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(30L)

    private var installed = false

    private val writeSeq = AtomicLong()

    private val latestWrite = ConcurrentHashMap<String, Long>()

    private val writeLock = Any()

    private class Bound(val profile: String, val disabled: Set<String>, val trusted: Boolean)

    private val bound = AtomicReference(Bound("", emptySet(), false))

    @Volatile
    private var keptEnabled: Set<String> = emptySet()

    private fun file(profile: String): Path = ConfigManager.profileDir(profile).resolve(FILE_NAME)

    @JvmStatic
    @Synchronized
    fun install() {
        if (installed) return
        installed = true
        migrateSharedList()
        ModToggles.setStore(this)
        ConfigManager.addProfileChangeListener(this)
    }

    @JvmStatic
    fun keepEnabled(modId: String) {
        keptEnabled = keptEnabled + modId
    }

    private fun migrateSharedList() {
        val shared = ConfigManager.internal().folder.resolve(FILE_NAME)
        if (!Files.exists(shared)) return
        try {
            for (profile in ConfigManager.profiles()) {
                val target = file(profile)
                if (Files.isDirectory(target.parent) && !Files.exists(target)) Files.copy(shared, target)
            }
            Files.delete(shared)
        } catch (e: Exception) {
            LOGGER.error("Failed to move the disabled mods into the profiles", e)
        }
    }

    private fun read(profile: String): Set<String> {
        val path = file(profile)
        if (!Files.exists(path)) return emptySet()
        return Files.readAllLines(path, StandardCharsets.UTF_8).map(String::trim).filterTo(HashSet(), String::isNotEmpty)
    }

    override fun load(): Set<String> {
        val profile = ConfigManager.activeProfile()
        val loaded = try {
            read(profile)
        } catch (e: Exception) {
            bound.set(Bound(profile, emptySet(), false))
            throw e
        }
        bound.set(Bound(profile, loaded, true))
        return loaded
    }

    override fun onProfileSaving(profile: String) {
        synchronized(writeLock) {
            val state = bound.get()
            if (state.profile != profile || !state.trusted) return
            latestWrite.merge(profile, writeSeq.incrementAndGet(), ::maxOf)
            write(state)
        }
    }

    override fun onProfileRenamed(oldProfile: String, newProfile: String) {
        bound.updateAndGet { if (it.profile == oldProfile) Bound(newProfile, it.disabled, it.trusted) else it }
    }

    override fun onProfileChanged(newProfile: String) {
        val loaded = try {
            read(newProfile)
        } catch (e: Exception) {
            LOGGER.error("Failed to load the disabled mods of profile {}", newProfile, e)
            null
        }
        ConfigManager.dispatchAndWait(
            Platform.screen()::runOnUiThread,
            {
                ModToggles.replaceDisabled { live ->
                    val next = loaded?.minus(keptEnabled) ?: live
                    val state = Bound(newProfile, next, loaded != null)
                    bound.set(state)
                    if (loaded != null && next != loaded) persist(state)
                    next
                }
            },
            PROFILE_SWITCH_BUDGET_NANOS,
            "the mod switches of profile $newProfile",
        )
    }

    override fun save(disabled: Set<String>) {
        val state = bound.updateAndGet { Bound(it.profile, disabled, it.trusted) }
        if (state.trusted) persist(state)
    }

    private fun persist(state: Bound) {
        val seq = writeSeq.incrementAndGet()
        latestWrite.merge(state.profile, seq, ::maxOf)
        Multithreading.submit {
            synchronized(writeLock) {
                if (latestWrite[state.profile] != seq) return@submit
                try {
                    write(state)
                } catch (e: Exception) {
                    LOGGER.error("Failed to persist disabled mods", e)
                }
            }
        }
    }

    private fun write(state: Bound) {
        val path = file(state.profile)
        if (!Files.isDirectory(path.parent)) return
        Files.write(
            path,
            state.disabled.sorted().joinToString("\n").toByteArray(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
    }
}
