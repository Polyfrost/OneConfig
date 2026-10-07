package org.polyfrost.oneconfig.internal.compat.toggle

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.backend.Backend

/**
 * Switches a mod off by overwriting its options with values that leave the game untouched
 *
 * Most mods read their config fields straight from their own mixins so there is no method to
 * intercept. While the mask is on the mod sees [values] instead of what the user configured and the
 * config is kept from being written so the file on disk always holds the user's own values
 *
 * @param className the mod's config class
 * @param treeIds ids the config may be registered under when it is a OneConfig config
 * @param values option name to its vanilla value where names missing from the mod's version are skipped
 *   and a dot steps into a nested object
 */
internal class ConfigMask(
    private val className: String,
    private val treeIds: List<String> = emptyList(),
    private val values: Map<String, Any?> = emptyMap(),
) {
    private var restore: (() -> Unit)? = null

    val applied: Boolean get() = restore != null

    @Synchronized
    fun apply() {
        if (restore != null) return
        restore = try {
            maskTree() ?: maskFields()
        } catch (t: Throwable) {
            LOGGER.error("Failed to mask the config {}", className, t)
            null
        }
    }

    @Synchronized
    fun remove() {
        val undo = restore ?: return
        restore = null
        try {
            undo()
        } catch (t: Throwable) {
            LOGGER.error("Failed to restore the config {}", className, t)
        }
    }

    /** Masks again after the mod reloaded its config from disk keeping what it loaded as the user's values */
    @Synchronized
    fun reapply() {
        if (restore == null) return
        restore = null
        apply()
    }

    fun masks(configClassName: String): Boolean = applied && configClassName == className

    private fun maskTree(): (() -> Unit)? {
        val manager = ConfigManager.active()
        val tree = manager.trees().firstOrNull { it.id in treeIds && it.isNative() } ?: return null
        // flush first because the tree is not written again until the mask comes off
        manager.save(tree)

        val originals = ArrayList<Pair<Property<Any?>, Any?>>()
        for ((name, value) in values) {
            @Suppress("UNCHECKED_CAST")
            val prop = tree.getProp(*name.split('.').toTypedArray()) as? Property<Any?> ?: continue
            try {
                val original = prop.get()
                prop.setAsSilently(convert(value, prop.type))
                originals += prop to original
            } catch (t: Throwable) {
                LOGGER.warn("Could not mask {} of {}", name, className, t)
            }
        }
        refreshVisibility(tree)
        val customSave = tree.getMetadata<Any>(CUSTOM_SAVE).takeIf { it !== NO_SAVE }
        tree.addMetadata(CUSTOM_SAVE, NO_SAVE)
        return {
            originals.forEach { (prop, value) -> prop.setAsSilently(value) }
            refreshVisibility(tree)
            if (tree.getMetadata<Any>(CUSTOM_SAVE) === NO_SAVE) {
                if (customSave != null) tree.addMetadata(CUSTOM_SAVE, customSave) else tree.removeMetadata(CUSTOM_SAVE)
            }
        }
    }

    // options shown depending on another one only notice a change made through its callbacks
    private fun refreshVisibility(tree: Tree) {
        for (node in tree.map.values) {
            when (node) {
                is Property<*> -> node.revaluateDisplay()
                is Tree -> refreshVisibility(node)
            }
        }
    }

    private fun maskFields(): (() -> Unit)? {
        val type = try {
            Class.forName(className, true, ConfigMask::class.java.classLoader)
        } catch (_: ClassNotFoundException) {
            return null
        }

        val originals = ArrayList<Pair<String, Any?>>()
        for ((path, value) in values) {
            try {
                val (field, owner) = resolve(type, path) ?: continue
                originals += path to field.get(owner)
                field.set(owner, convert(value, field.type))
            } catch (t: Throwable) {
                LOGGER.warn("Could not mask {} of {}", path, className, t)
            }
        }
        return {
            for ((path, value) in originals) {
                try {
                    resolve(type, path)?.let { (field, owner) -> field.set(owner, value) }
                } catch (t: Throwable) {
                    LOGGER.warn("Could not restore {} of {}", path, className, t)
                }
            }
        }
    }

    private fun resolve(type: Class<*>, path: String): Pair<Field, Any?>? {
        val instance = yaclHandler(type)?.let { it.javaClass.getMethod("instance").invoke(it) } ?: singleton(type)
        return resolve(type, instance, path.split('.'))
    }

    private fun resolve(type: Class<*>, instance: Any?, path: List<String>): Pair<Field, Any?>? {
        var field = type.field(path.first()) ?: return null
        var owner: Any? = if (Modifier.isStatic(field.modifiers)) null else instance ?: return null
        for (name in path.drop(1)) {
            owner = field.get(owner) ?: return null
            field = owner.javaClass.field(name) ?: return null
        }
        return field to owner
    }

    private fun Class<*>.field(name: String): Field? =
        generateSequence(this) { it.superclass }
            .firstNotNullOfOrNull { type -> type.declaredFields.firstOrNull { it.name == name } }
            ?.also { it.isAccessible = true }

    /** YACL keeps the config in a static `ConfigClassHandler` rather than a singleton */
    private fun yaclHandler(type: Class<*>): Any? =
        type.declaredFields.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.type.name == "dev.isxander.yacl3.config.v2.api.ConfigClassHandler"
        }?.let { it.isAccessible = true; it.get(null) }

    private fun singleton(type: Class<*>): Any? =
        type.declaredFields.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.type == type && it.name in SINGLETON_FIELDS
        }?.let { it.isAccessible = true; it.get(null) }

    private fun Tree.isNative(): Boolean =
        getMetadata<Any>(Backend.UI_ONLY_METADATA) != true && getMetadata<Any>(CompatSnapshots.SNAPSHOT_METADATA) != true

    private companion object {
        val LOGGER = LogManager.getLogger("OneConfig/ModToggles")
        const val CUSTOM_SAVE = "custom_save"
        val NO_SAVE = Runnable { }
        val SINGLETON_FIELDS = setOf("INSTANCE", "instance", "CONFIG", "config")

        fun convert(value: Any?, type: Class<*>): Any? = when {
            value is Number && (type == Boolean::class.javaPrimitiveType || type == java.lang.Boolean::class.java) -> value.toInt() != 0
            value is Number && (type == Float::class.javaPrimitiveType || type == java.lang.Float::class.java) -> value.toFloat()
            value is Number && (type == Double::class.javaPrimitiveType || type == java.lang.Double::class.java) -> value.toDouble()
            value is Number && (type == Int::class.javaPrimitiveType || type == java.lang.Integer::class.java) -> value.toInt()
            value is Number && (type == Long::class.javaPrimitiveType || type == java.lang.Long::class.java) -> value.toLong()
            value is Number && type == PolyColor::class.java -> PolyColor(value.toInt())
            value is Number && type == java.awt.Color::class.java -> java.awt.Color(value.toInt(), true)
            value is String && type.isEnum -> type.enumConstants.first { (it as Enum<*>).name == value }
            else -> value
        }
    }
}
