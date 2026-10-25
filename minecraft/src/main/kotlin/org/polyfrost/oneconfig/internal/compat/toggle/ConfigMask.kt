package org.polyfrost.oneconfig.internal.compat.toggle

import java.awt.Color
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer
import org.polyfrost.oneconfig.internal.ui.api.HELD_VALUE
import org.polyfrost.oneconfig.internal.ui.api.HeldValue
import org.polyfrost.oneconfig.utils.v1.WrappingUtils

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
    @Volatile
    private var restore: (() -> Unit)? = null
    private var onFields = false
    private var liftedForSave = false
    private var maskedTree: Tree? = null
    private var fieldOriginals: List<Pair<String, Any?>> = emptyList()
    private var carried: Map<String, Any?> = emptyMap()
    private val held = ArrayList<Property<*>>()

    /** The trees another config library's options are shown through, which read the masked fields directly */
    var compatTrees: () -> List<Tree> = { emptyList() }

    val applied: Boolean get() = restore != null

    @Synchronized
    fun apply() {
        // a tree rebuilt underneath the mask holds values of its own, and the ones kept for the old tree are not its
        val stale = maskedTree
        if (restore != null && stale != null && ConfigManager.active().trees().none { it === stale }) {
            restore = null
            maskedTree = null
        }
        if (restore != null) return
        restore = try {
            val tree = maskTree() ?: configClass()?.let { maskTree() }
            onFields = tree == null
            tree ?: maskFields()
        } catch (t: Throwable) {
            LOGGER.error("Failed to mask the config {}", className, t)
            null
        }
        holdForDisplay()
    }

    @Synchronized
    fun remove() {
        liftedForSave = false
        val undo = restore ?: return
        restore = null
        try {
            undo()
        } catch (t: Throwable) {
            LOGGER.error("Failed to restore the config {}", className, t)
        }
    }

    /** Lets the options the masked fields are shown through keep showing what the user chose */
    @Synchronized
    fun holdForDisplay() {
        if (restore == null || !onFields) return
        try {
            for (tree in compatTrees()) {
                for ((path, original) in fieldOriginals) {
                    val prop = tree.find(optionKey(path.substringAfterLast('.'))) ?: continue
                    if (prop in held) continue
                    // some options show a field as another type, and a value of the field's type would not fit them
                    if (original != null && !prop.type.kotlin.javaObjectType.isInstance(original)) continue
                    prop.addMetadata(HELD_VALUE, HeldValue(original))
                    held += prop
                }
            }
        } catch (t: Throwable) {
            LOGGER.warn("Could not keep the options of {} showing the user's values", className, t)
        }
    }

    private fun optionKey(name: String) = name.filter(Char::isLetterOrDigit).lowercase()

    private fun Tree.find(key: String): Property<*>? {
        for (node in map.values) {
            when (node) {
                is Property<*> -> if (node.id?.let(::optionKey) == key) return node
                is Tree -> node.find(key)?.let { return it }
            }
        }
        return null
    }

    /**
     * Takes a mask written straight into the mod's fields off until [apply] is called again
     *
     * @return whether there was one to take off
     */
    @Synchronized
    fun suspend(): Boolean {
        if (restore == null || !onFields) return false
        remove()
        return true
    }

    /** Masks again after the mod reloaded its config from disk keeping what it loaded as the user's values */
    @Synchronized
    fun reapply() {
        if (restore == null) return
        held.forEach { it.removeMetadata(HELD_VALUE) }
        held.clear()
        if (onFields) carried = fieldOriginals.toMap()
        restore = null
        try {
            apply()
        } finally {
            carried = emptyMap()
        }
    }

    /**
     * Takes what the mod's own screen wrote over a mask on its fields as the user's values and masks them again
     */
    @Synchronized
    fun absorbEdits() {
        if (restore == null || !onFields) return
        val type = configClass() ?: return
        var changed = false
        fieldOriginals = fieldOriginals.map { (path, original) ->
            try {
                val (field, owner) = resolve(type, path) ?: return@map path to original
                val substitute = convert(values[path], field.type)
                val current = field.get(owner)
                if (current == substitute) return@map path to original
                field.set(owner, substitute)
                changed = true
                path to current
            } catch (t: Throwable) {
                LOGGER.warn("Could not keep the edit to {} of {}", path, className, t)
                path to original
            }
        }
        if (!changed) return
        held.forEach { it.removeMetadata(HELD_VALUE) }
        held.clear()
        holdForDisplay()
    }

    /** [reapply] for a mod that only sometimes puts its own values back over a mask written into its fields */
    @Synchronized
    fun reapplyIfOverwritten() {
        if (liftedForSave) return applyAfterSave()
        if (restore == null || !onFields) return
        val overwritten = try {
            val type = Class.forName(className, true, ConfigMask::class.java.classLoader)
            values.any { (path, value) ->
                val (field, owner) = resolve(type, path) ?: return@any false
                field.get(owner) != convert(value, field.type)
            }
        } catch (t: Throwable) {
            LOGGER.warn("Could not check the mask of {}", className, t)
            false
        }
        if (overwritten) reapply()
    }

    @Synchronized
    fun liftForSave() {
        if (restore == null || !onFields) return
        val type = configClass() ?: return
        for ((path, original) in fieldOriginals) {
            try {
                val (field, owner) = resolve(type, path) ?: continue
                if (field.get(owner) == convert(values[path], field.type)) field.set(owner, original)
            } catch (t: Throwable) {
                LOGGER.warn("Could not restore {} of {}", path, className, t)
            }
        }
        held.forEach { it.removeMetadata(HELD_VALUE) }
        held.clear()
        fieldOriginals = emptyList()
        restore = null
        liftedForSave = true
    }

    @Synchronized
    fun applyAfterSave() {
        if (!liftedForSave) return
        liftedForSave = false
        apply()
    }

    @Synchronized
    fun adopt(tree: Tree) {
        if (restore != null && onFields && isTree(tree)) reapply()
    }

    fun isTree(tree: Tree): Boolean = tree.id in treeIds && tree.isNative()

    fun isFor(configClassName: String): Boolean = configClassName == className

    fun masks(configClassName: String): Boolean = applied && isFor(configClassName)

    /** Whether YACL is what saves this config, which only a hook into YACL can keep the mask out of */
    fun savedByYacl(): Boolean = try {
        Class.forName(className, false, ConfigMask::class.java.classLoader).declaredFields.any(::isYaclHandler)
    } catch (_: Throwable) {
        false
    }

    private fun isYaclHandler(field: Field) = Modifier.isStatic(field.modifiers) && field.type.name == YACL_HANDLER

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
                val original = detached(prop.get())
                prop.setAsSilently(convert(value, prop.type))
                prop.addMetadata(HELD_VALUE, HeldValue(original))
                originals += prop to original
            } catch (t: Throwable) {
                LOGGER.warn("Could not mask {} of {}", name, className, t)
            }
        }
        refreshVisibility(tree)
        val customSave = tree.getMetadata<Any>(CUSTOM_SAVE).takeIf { it !== NO_SAVE }
        tree.addMetadata(CUSTOM_SAVE, NO_SAVE)
        maskedTree = tree
        return {
            maskedTree = null
            originals.forEach { (prop, value) ->
                prop.setAsSilently(value)
                prop.removeMetadata(HELD_VALUE)
            }
            refreshVisibility(tree)
            if (tree.getMetadata<Any>(CUSTOM_SAVE) === NO_SAVE) {
                if (customSave != null) tree.addMetadata(CUSTOM_SAVE, customSave) else tree.removeMetadata(CUSTOM_SAVE)
            }
        }
    }

    // an option backed by a field writes into the object it already holds, which would take the user's value with it
    private fun detached(value: Any?): Any? {
        val type = value?.javaClass ?: return null
        if (type.isEnum || WrappingUtils.isSimpleClass(type) || ObjectSerializer.isImmutable(type)) return value
        return try {
            when {
                value is PolyColor -> PolyColor(value.rawArgb, value.chroma, value.chromaSpeed)
                type.isArray -> ReflectArray.getLength(value).let { length ->
                    ReflectArray.newInstance(type.componentType, length).also { System.arraycopy(value, 0, it, 0, length) }
                }
                value is List<*> -> ArrayList(value)
                else -> ObjectSerializer.overwrite(type.getDeclaredConstructor().also { it.isAccessible = true }.newInstance(), value)
            }
        } catch (t: Throwable) {
            LOGGER.warn("Could not copy a {} of {} before masking it", type.simpleName, className, t)
            value
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
        val type = configClass() ?: return null

        val originals = ArrayList<Pair<String, Any?>>()
        for ((path, value) in values) {
            try {
                val (field, owner) = resolve(type, path) ?: continue
                val current = field.get(owner)
                val substitute = convert(value, field.type)
                originals += path to if (current == substitute && path in carried) carried[path] else current
                field.set(owner, substitute)
            } catch (t: Throwable) {
                LOGGER.warn("Could not mask {} of {}", path, className, t)
            }
        }
        fieldOriginals = originals
        return {
            for ((path, value) in fieldOriginals) {
                try {
                    resolve(type, path)?.let { (field, owner) -> field.set(owner, value) }
                } catch (t: Throwable) {
                    LOGGER.warn("Could not restore {} of {}", path, className, t)
                }
            }
            held.forEach { it.removeMetadata(HELD_VALUE) }
            held.clear()
            fieldOriginals = emptyList()
        }
    }

    private fun configClass(): Class<*>? = try {
        Class.forName(className, true, ConfigMask::class.java.classLoader)
    } catch (_: ClassNotFoundException) {
        null
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
        type.declaredFields.firstOrNull(::isYaclHandler)?.let { it.isAccessible = true; it.get(null) }

    private fun singleton(type: Class<*>): Any? =
        type.declaredFields.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.type == type && it.name in SINGLETON_FIELDS
        }?.let { it.isAccessible = true; it.get(null) }

    private fun Tree.isNative(): Boolean =
        getMetadata<Any>(Backend.UI_ONLY_METADATA) != true && getMetadata<Any>(CompatSnapshots.SNAPSHOT_METADATA) != true

    private companion object {
        val LOGGER = LogManager.getLogger("OneConfig/ModToggles")
        const val CUSTOM_SAVE = "custom_save"
        const val YACL_HANDLER = "dev.isxander.yacl3.config.v2.api.ConfigClassHandler"
        val NO_SAVE = Runnable { }
        val SINGLETON_FIELDS = setOf("INSTANCE", "instance", "CONFIG", "config")

        fun convert(value: Any?, type: Class<*>): Any? = when {
            value is Number && (type == Boolean::class.javaPrimitiveType || type == java.lang.Boolean::class.java) -> value.toInt() != 0
            value is Number && (type == Float::class.javaPrimitiveType || type == java.lang.Float::class.java) -> value.toFloat()
            value is Number && (type == Double::class.javaPrimitiveType || type == java.lang.Double::class.java) -> value.toDouble()
            value is Number && (type == Int::class.javaPrimitiveType || type == Integer::class.java) -> value.toInt()
            value is Number && (type == Long::class.javaPrimitiveType || type == java.lang.Long::class.java) -> value.toLong()
            value is Number && type == PolyColor::class.java -> PolyColor(value.toInt())
            value is Number && type == Color::class.java -> Color(value.toInt(), true)
            value is String && type.isEnum -> type.enumConstants.first { (it as Enum<*>).name == value }
            else -> value
        }
    }
}
