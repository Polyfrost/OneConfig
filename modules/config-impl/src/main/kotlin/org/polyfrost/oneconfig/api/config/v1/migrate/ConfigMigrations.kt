package org.polyfrost.oneconfig.api.config.v1.migrate

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap

@ApiStatus.Experimental
public object ConfigMigrations {
    internal const val STORE_FILE: String = "oneconfig-migrations.json"
    private const val RETRY_DELAY_MS = 30_000L

    private class Store(val versions: MutableMap<String, Int>, val invalid: Map<String, Any?>)

    private class Gate(val generation: Int, val until: Long)

    private val LOGGER: Logger = LogManager.getLogger("OneConfig/Migrations")
    private val lock = Any()
    private val steps = LinkedHashMap<String, TreeMap<Int, MigrationScope.() -> Unit>>()
    private val checked = HashMap<Path, Int>()
    private val retryAt = HashMap<Path, HashMap<String, Long>>()
    private val gates = HashMap<Path, Gate>()
    private val warnedInvalid = HashSet<String>()
    private var running = false

    @Volatile
    private var generation = 0

    @JvmStatic
    public fun register(modId: String, version: Int, migrate: MigrationScope.() -> Unit) {
        require(modId.isNotBlank()) { "Migration mod id cannot be blank" }
        require(version > 0) { "Migration versions start at 1, got $version for $modId" }
        synchronized(lock) {
            val forMod = steps.getOrPut(modId) { TreeMap() }
            require(version !in forMod) { "Migration $version of $modId is already registered" }
            forMod[version] = migrate
            generation++
        }
    }

    internal fun runPending(folder: Path, reload: Boolean): Boolean {
        if (generation == 0) return false
        val root = folder.toAbsolutePath().normalize()
        synchronized(lock) {
            if (running) return false
            if (reload) {
                retryAt.remove(root)
                gates.remove(root)
            } else {
                if (checked[root] == generation) return false
                val gate = gates[root]
                if (gate != null && gate.generation == generation && gate.until > System.currentTimeMillis()) return false
            }
            running = true
            try {
                return runLocked(root)
            } finally {
                running = false
            }
        }
    }

    private fun runLocked(root: Path): Boolean {
        if (steps.isEmpty()) {
            checked[root] = generation
            return false
        }
        val now = System.currentTimeMillis()
        val store = readStore(root)
        if (store == null) {
            gates[root] = Gate(generation, now + RETRY_DELAY_MS)
            return false
        }
        val pending = steps.entries.filter { (modId, forMod) ->
            if (forMod.higherKey(store.versions[modId] ?: 0) == null) return@filter false
            if (modId in store.invalid) {
                if (warnedInvalid.add("$root|$modId")) {
                    LOGGER.error("The stored config version of {} in {} is not a whole number; skipping its migrations", modId, root.resolve(STORE_FILE))
                }
                return@filter false
            }
            true
        }
        if (pending.isEmpty()) {
            checked[root] = generation
            gates.remove(root)
            return false
        }
        val modRetry = retryAt.getOrPut(root) { HashMap() }
        val due = pending.filter { (modRetry[it.key] ?: 0L) <= now }
        if (due.isEmpty()) {
            gates[root] = Gate(generation, modRetry.values.min())
            return false
        }
        if (!writeStore(root, store)) {
            LOGGER.error("Skipping config migrations in {} because their versions cannot be saved", root)
            gates[root] = Gate(generation, now + RETRY_DELAY_MS)
            return false
        }
        var changed = false
        for ((modId, forMod) in due) {
            val stored = store.versions[modId] ?: 0
            var modFailed = false
            for ((version, step) in forMod.tailMap(stored, false).entries.toList()) {
                try {
                    MigrationScope(modId, version, root).step()
                } catch (t: Throwable) {
                    LOGGER.error("Migration {} of {} failed in {}; it stays at version {}", version, modId, root, store.versions[modId] ?: 0, t)
                    modFailed = true
                    break
                }
                changed = true
                store.versions[modId] = version
                if (!writeStore(root, store)) {
                    LOGGER.error("Stopping config migrations in {} because version {} of {} could not be saved", root, version, modId)
                    gates[root] = Gate(generation, System.currentTimeMillis() + RETRY_DELAY_MS)
                    return changed
                }
                LOGGER.info("Migrated {} to config version {} in {}", modId, version, root)
            }
            if (modFailed) modRetry[modId] = System.currentTimeMillis() + RETRY_DELAY_MS else modRetry.remove(modId)
        }
        if (modRetry.isEmpty()) {
            checked[root] = generation
            retryAt.remove(root)
            gates.remove(root)
        } else {
            gates[root] = Gate(generation, modRetry.values.min())
        }
        return changed
    }

    private fun readStore(root: Path): Store? {
        val file = root.resolve(STORE_FILE)
        if (!Files.isRegularFile(file)) return Store(LinkedHashMap(), emptyMap())
        return try {
            val versions = LinkedHashMap<String, Int>()
            val invalid = LinkedHashMap<String, Any?>()
            for ((key, value) in MigrationJson.parse(String(Files.readAllBytes(file), Charsets.UTF_8))) {
                val version = wholeVersion(value)
                if (version == null) invalid[key] = value else versions[key] = version
            }
            Store(versions, invalid)
        } catch (t: Throwable) {
            LOGGER.error("Could not read config versions from {}; skipping migrations for this profile", file, t)
            null
        }
    }

    private fun wholeVersion(value: Any?): Int? {
        if (value !is Number) return null
        val number = value.toDouble()
        if (number < 0.0 || number > Int.MAX_VALUE || number != Math.floor(number)) return null
        return number.toInt()
    }

    private fun writeStore(root: Path, store: Store): Boolean {
        return try {
            val out = LinkedHashMap<String, Any?>(store.invalid)
            out.putAll(store.versions)
            MigrationFiles.write(root.resolve(STORE_FILE), MigrationJson.write(out))
            true
        } catch (t: Throwable) {
            LOGGER.error("Could not save config versions to {}", root.resolve(STORE_FILE), t)
            false
        }
    }
}

@ApiStatus.Internal
public object MigrationRunner {
    @JvmStatic
    public fun profileOpened(folder: Path): Boolean = ConfigMigrations.runPending(folder, true)

    @JvmStatic
    public fun beforeLoad(folder: Path): Boolean = ConfigMigrations.runPending(folder, false)
}
