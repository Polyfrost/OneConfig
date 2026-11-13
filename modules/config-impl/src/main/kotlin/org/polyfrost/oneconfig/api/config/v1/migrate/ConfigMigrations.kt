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

    private val LOGGER: Logger = LogManager.getLogger("OneConfig/Migrations")
    private val lock = Any()
    private val steps = LinkedHashMap<String, TreeMap<Int, MigrationScope.() -> Unit>>()
    private val checked = HashMap<Path, Int>()
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

    internal fun runPending(folder: Path, reload: Boolean) {
        val root = folder.toAbsolutePath().normalize()
        synchronized(lock) {
            if (!reload && checked[root] == generation) return
            checked[root] = generation
            if (steps.isEmpty()) return
            val store = readStore(root) ?: return
            for ((modId, forMod) in steps.entries.toList()) {
                val stored = store[modId] ?: 0
                for ((version, step) in forMod.tailMap(stored, false).entries.toList()) {
                    try {
                        MigrationScope(modId, version, root).step()
                    } catch (t: Throwable) {
                        LOGGER.error("Migration {} of {} failed in {}; it stays at version {}", version, modId, root, store[modId] ?: 0, t)
                        break
                    }
                    store[modId] = version
                    writeStore(root, store)
                    LOGGER.info("Migrated {} to config version {} in {}", modId, version, root)
                }
            }
        }
    }

    private fun readStore(root: Path): MutableMap<String, Int>? {
        val file = root.resolve(STORE_FILE)
        if (!Files.isRegularFile(file)) return LinkedHashMap()
        return try {
            val out = LinkedHashMap<String, Int>()
            for ((key, value) in MigrationJson.parse(String(Files.readAllBytes(file), Charsets.UTF_8))) {
                val version = (value as? Number)?.toInt() ?: continue
                out[key] = version
            }
            out
        } catch (t: Throwable) {
            LOGGER.error("Could not read config versions from {}; skipping migrations for this profile", file, t)
            null
        }
    }

    private fun writeStore(root: Path, store: Map<String, Int>) {
        try {
            MigrationFiles.write(root.resolve(STORE_FILE), MigrationJson.write(store))
        } catch (t: Throwable) {
            LOGGER.error("Could not save config versions to {}", root.resolve(STORE_FILE), t)
        }
    }
}

@ApiStatus.Internal
public object MigrationRunner {
    @JvmStatic
    public fun profileOpened(folder: Path) {
        ConfigMigrations.runPending(folder, true)
    }

    @JvmStatic
    public fun beforeLoad(folder: Path) {
        ConfigMigrations.runPending(folder, false)
    }
}
