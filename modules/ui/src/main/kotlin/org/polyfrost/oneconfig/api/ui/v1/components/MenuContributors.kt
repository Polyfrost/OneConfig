@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OptionWidgets
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import org.polyfrost.oneconfig.utils.v1.WrappingUtils

private val CONTRIBUTOR_LOGGER = LogManager.getLogger("OneConfig/MenuContributors")

@ExperimentalOneConfigUiApi
public object OptionMenuContributors {
    private val contributors = CopyOnWriteArrayList<(Property<*>) -> List<MenuEntry>>()

    @JvmStatic
    public fun register(contributor: (Property<*>) -> List<MenuEntry>) {
        contributors.addIfAbsent(contributor)
    }

    @JvmStatic
    public fun unregister(contributor: (Property<*>) -> List<MenuEntry>): Boolean = contributors.remove(contributor)

    @JvmStatic
    public fun entriesFor(property: Property<*>): List<MenuEntry> = collectEntries(contributors, property)

    @JvmStatic
    @JvmOverloads
    public fun builtInEntries(property: Property<*>, onReset: (() -> Unit)? = null): List<MenuEntry> {
        val locked = property.display == Property.Display.DISABLED
        val value = runCatching { property.get() }.getOrNull()
        val resettable = onReset != null || (value !is OneConfigKeybind && hasDefault(property, HashSet()))
        return buildList {
            add(
                MenuAction(
                    kitString("oneconfig.menu.option.reset", "Reset to default"),
                    { if (onReset != null) onReset() else resetToDefault(property) },
                    icon = "refresh",
                    enabled = !locked && resettable,
                ),
            )
            if (value is PolyColor) {
                add(MenuAction(kitString("oneconfig.menu.option.copy_hex", "Copy hex"), { ClipboardHelper.setString(colorHex(value)) }, icon = "copy"))
                val pasted = ClipboardHelper.getString()?.let(::parseColor)
                add(
                    MenuAction(
                        kitString("oneconfig.menu.option.paste_hex", "Paste hex"),
                        { if (pasted != null) writeValue(property, pasted) },
                        icon = "clipboard",
                        enabled = !locked && pasted != null,
                    ),
                )
                return@buildList
            }
            val codec = codecFor(property.type) ?: return@buildList
            val copied = value?.let(codec.format)
            add(
                MenuAction(
                    kitString("oneconfig.menu.option.copy_value", "Copy value"),
                    { if (copied != null) ClipboardHelper.setString(copied) },
                    icon = "copy",
                    enabled = copied != null,
                ),
            )
            val pasted = ClipboardHelper.getString()?.let(codec.parse)
            add(
                MenuAction(
                    kitString("oneconfig.menu.option.paste_value", "Paste value"),
                    { if (pasted != null) writeValue(property, pasted) },
                    icon = "clipboard",
                    enabled = !locked && pasted != null && pasted != value,
                ),
            )
        }
    }
}

@ExperimentalOneConfigUiApi
public object ModCardMenuContributors {
    private val contributors = CopyOnWriteArrayList<(String) -> List<MenuEntry>>()

    @JvmStatic
    public fun register(contributor: (modId: String) -> List<MenuEntry>) {
        contributors.addIfAbsent(contributor)
    }

    @JvmStatic
    public fun unregister(contributor: (modId: String) -> List<MenuEntry>): Boolean = contributors.remove(contributor)

    @JvmStatic
    public fun entriesFor(modId: String): List<MenuEntry> = collectEntries(contributors, modId)
}

private fun <T> collectEntries(contributors: List<(T) -> List<MenuEntry>>, target: T): List<MenuEntry> {
    val result = ArrayList<MenuEntry>()
    for (contributor in contributors) {
        val entries = try {
            contributor(target)
        } catch (t: Throwable) {
            CONTRIBUTOR_LOGGER.warn("A menu contributor failed for {}", target, t)
            continue
        }
        if (entries.isEmpty()) continue
        if (result.isNotEmpty()) result += MenuSeparator
        result += entries
    }
    return result
}

private class ValueCodec(val format: (Any) -> String?, val parse: (String) -> Any?)

private fun codecFor(type: Class<*>?): ValueCodec? {
    if (type == null) return null
    if (type.isEnum) {
        val constants = type.enumConstants ?: return null
        return ValueCodec({ (it as Enum<*>).name }) { text ->
            val wanted = text.trim()
            constants.firstOrNull { (it as Enum<*>).name.equals(wanted, ignoreCase = true) }
        }
    }
    return when (type.kotlin.javaObjectType) {
        Boolean::class.javaObjectType -> ValueCodec({ it.toString() }) { text ->
            when (text.trim().lowercase(Locale.ROOT)) {
                "true", "on", "yes", "1" -> true
                "false", "off", "no", "0" -> false
                else -> null
            }
        }
        Int::class.javaObjectType -> ValueCodec({ it.toString() }) { it.trim().toIntOrNull() }
        Long::class.javaObjectType -> ValueCodec({ it.toString() }) { it.trim().toLongOrNull() }
        Short::class.javaObjectType -> ValueCodec({ it.toString() }) { it.trim().toShortOrNull() }
        Byte::class.javaObjectType -> ValueCodec({ it.toString() }) { it.trim().toByteOrNull() }
        Float::class.javaObjectType -> ValueCodec({ it.toString() }) { it.trim().toFloatOrNull()?.takeIf(Float::isFinite) }
        Double::class.javaObjectType -> ValueCodec({ it.toString() }) { it.trim().toDoubleOrNull()?.takeIf(Double::isFinite) }
        String::class.java -> ValueCodec({ it as String }) { it }
        else -> null
    }
}

private fun colorHex(color: PolyColor): String {
    val argb = color.rawArgb
    return if (argb ushr 24 == 0xFF) "#%06X".format(argb and 0xFFFFFF) else "#%08X".format(argb.toLong() and 0xFFFFFFFFL)
}

private fun parseColor(text: String): PolyColor? = runCatching { PolyColor.hex(text.trim()) }.getOrNull()

@Suppress("UNCHECKED_CAST")
private fun writeValue(property: Property<*>, value: Any) {
    (property as Property<Any?>).set(value)
    OptionWidgets.refresh(property)
}

private fun hasDefault(property: Property<*>, seen: MutableSet<Property<*>>): Boolean =
    seen.add(property) && (property.getMetadata<Any?>("default") != null ||
        property.getMetadata<Collection<Property<*>>>("resetWith")?.any { hasDefault(it, seen) } == true)

private fun resetToDefault(property: Property<*>, seen: MutableSet<Property<*>> = HashSet()) {
    if (!seen.add(property)) return
    val default = property.getMetadata<Any?>("default")
    if (default != null) {
        @Suppress("UNCHECKED_CAST")
        (property as Property<Any?>).setReferential(copyOf(default))
    }
    val linked = property.getMetadata<Collection<Property<*>>>("resetWith")
    linked?.forEach { resetToDefault(it, seen) }
    if (default != null || linked != null) OptionWidgets.refresh(property)
}

@Suppress("UNCHECKED_CAST")
private fun copyOf(value: Any): Any = when {
    WrappingUtils.isSimpleClass(value.javaClass) -> value
    value is PolyColor -> PolyColor(value.rawArgb, value.chroma, value.chromaSpeed).also {
        it.gradientEnabled = value.gradientEnabled
        it.gradientArgb = value.gradientArgb
    }
    value.javaClass.isArray -> java.lang.reflect.Array.newInstance(value.javaClass.componentType, java.lang.reflect.Array.getLength(value)).also {
        System.arraycopy(value, 0, it, 0, java.lang.reflect.Array.getLength(value))
    }
    value is List<*> -> ArrayList(value)
    value is Set<*> -> LinkedHashSet(value)
    value is Map<*, *> -> LinkedHashMap(value)
    else -> runCatching {
        (ObjectSerializer.INSTANCE.serialize(value, false, false) as? Map<String, Any>)?.let(ObjectSerializer.INSTANCE::deserialize)
    }.getOrNull() ?: value
}
