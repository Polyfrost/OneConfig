@file:JvmName("ShareCodes")

package org.polyfrost.oneconfig.api.config.v1

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.NullObject
import com.electronwill.nightconfig.core.UnmodifiableConfig
import com.electronwill.nightconfig.json.JsonFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OptionWidgets
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.utils.v1.WrappingUtils
import java.lang.reflect.Array as JArray

@ApiStatus.Experimental
public sealed class ShareCodeResult {
    public val isSuccess: Boolean get() = this is Success

    public data class Success(public val applied: List<String>, public val skipped: List<String>) : ShareCodeResult()

    public data object WrongPrefix : ShareCodeResult()

    public data class Corrupt(public val cause: Throwable) : ShareCodeResult()
}

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.toShareCode(prefix: String, include: (Property<*>) -> Boolean = { true }): String {
    val json = encodeTree(this, include) ?: Config.inMemoryUniversal()
    val text = JsonFormat.minimalInstance().createWriter().writeToString(json)
    val bytes = ByteArrayOutputStream()
    GZIPOutputStream(bytes).use { it.write(text.toByteArray(Charsets.UTF_8)) }
    return prefix + Base64.getEncoder().encodeToString(bytes.toByteArray())
}

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.applyShareCode(code: String, prefix: String, include: (Property<*>) -> Boolean = { true }): ShareCodeResult {
    val trimmed = code.trim()
    if (!trimmed.startsWith(prefix)) return ShareCodeResult.WrongPrefix
    val data = try {
        readPayload(trimmed.substring(prefix.length))
    } catch (e: Exception) {
        return ShareCodeResult.Corrupt(e)
    }
    val applied = ArrayList<String>()
    val skipped = ArrayList<String>()
    applyTree(this, data, "", include, applied, skipped)
    return ShareCodeResult.Success(applied, skipped)
}

private const val MAX_PAYLOAD_BYTES = 4 shl 20

private fun encodeTree(tree: Tree, include: (Property<*>) -> Boolean): Config? {
    val out = Config.inMemoryUniversal()
    tree.onAll { key, node ->
        val value = when (node) {
            is Tree -> encodeTree(node, include)
            is Property<*> -> if (isButton(node) || !include(node)) null else node.get()?.let { encodeValue(node, it) }
            else -> null
        }
        if (value != null) out.add(listOf(key), value)
    }
    return if (out.isEmpty) null else out
}

private fun isButton(prop: Property<*>): Boolean = prop.type == Void.TYPE || prop.type == Void::class.java

private fun encodeValue(prop: Property<*>, value: Any): Any? = runCatching {
    when {
        value is Float && !value.isFinite() -> null
        value is Double && !value.isFinite() -> null
        value is Enum<*> -> value.name
        value is Char -> value.toString()
        WrappingUtils.isSimpleObject(value) -> value
        else -> {
            val adapter = adapterFor(value.javaClass)
            if (adapter != null && value.javaClass == WrappingUtils.getWrapped(prop.type)) {
                toJsonValue(adapter.serialize(value))
            } else {
                toJsonValue(ObjectSerializer.INSTANCE.serialize(value, true))
            }
        }
    }
}.getOrNull()

internal fun toJsonValue(value: Any?): Any? = when (value) {
    null -> null
    is Map<*, *> -> Config.inMemoryUniversal().also { config ->
        for ((key, item) in value) toJsonValue(item)?.let { config.add(listOf(key.toString()), it) }
    }
    is Collection<*> -> value.map(::toJsonValue)
    is Enum<*> -> value.name
    is Char -> value.toString()
    is Number, is Boolean, is CharSequence -> value
    else -> if (value.javaClass.isArray) List(JArray.getLength(value)) { toJsonValue(JArray.get(value, it)) } else null
}

internal fun toPlain(raw: Any?): Any? = when {
    raw == null || raw === NullObject.NULL_OBJECT -> null
    raw is UnmodifiableConfig -> LinkedHashMap<String, Any?>().also { map ->
        for (entry in raw.entrySet()) map[entry.key] = toPlain(entry.getRawValue<Any?>())
    }
    raw is List<*> -> raw.map(::toPlain)
    else -> raw
}

private fun readPayload(body: String): UnmodifiableConfig {
    val bytes = Base64.getDecoder().decode(body.filterNot(Char::isWhitespace))
    val text = GZIPInputStream(ByteArrayInputStream(bytes)).use { readCapped(it) }.toString(Charsets.UTF_8)
    return JsonFormat.minimalInstance().createParser().parse(text)
}

private fun readCapped(input: InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        out.write(buffer, 0, read)
        if (out.size() > MAX_PAYLOAD_BYTES) throw IOException("share code payload is larger than $MAX_PAYLOAD_BYTES bytes")
    }
    return out.toByteArray()
}

@OptIn(ExperimentalOneConfigUiApi::class)
private fun applyTree(
    target: Tree,
    data: UnmodifiableConfig,
    path: String,
    include: (Property<*>) -> Boolean,
    applied: MutableList<String>,
    skipped: MutableList<String>,
) {
    for (entry in data.entrySet()) {
        val key = entry.key
        val raw = entry.getRawValue<Any?>()
        val full = if (path.isEmpty()) key else "$path.$key"
        when (val node = target.get(key)) {
            is Tree -> if (raw is UnmodifiableConfig) applyTree(node, raw, full, include, applied, skipped) else skipped.add(full)
            is Property<*> -> if (!isButton(node) && include(node) && applyValue(node, raw)) {
                applied.add(full)
                OptionWidgets.refresh(node)
            } else skipped.add(full)
            else -> skipped.add(full)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun applyValue(prop: Property<*>, raw: Any?): Boolean = try {
    val current = prop.get()
    val value = decodeAs(WrappingUtils.getWrapped(prop.type), toPlain(raw), current)
    when {
        value == null -> false
        value is OneConfigKeybind && current is OneConfigKeybind -> applyKeybind(prop as Property<Any?>, current, value)
        else -> !(prop as Property<Any?>).set(value)
    }
} catch (e: Exception) {
    false
}

private fun applyKeybind(prop: Property<Any?>, current: OneConfigKeybind, imported: OneConfigKeybind): Boolean {
    val next = current.copyWith(imported.keyCodes, imported.mouseBtns, imported.mods)
    next.durationNanos = imported.durationNanos
    if (prop.set(next)) return false
    val applied = prop.get() as? OneConfigKeybind ?: next
    applied.mergeOverwrite(imported)
    KeybindManager.replace(current, applied)
    return true
}

private fun decodeAs(target: Class<*>, plain: Any?, current: Any?): Any? {
    if (plain == null) return null
    if (plain is Map<*, *> && Map::class.java.isAssignableFrom(target)) return decodeMap(target, plain, current)
    classTagged(plain)?.let { return deserializeTagged(target, it, taggedClass(it)) }
    if (Enum::class.java.isAssignableFrom(target)) return enumConstant(target, plain)
    if (plain is List<*> && (target.isArray || Collection::class.java.isAssignableFrom(target))) {
        val element = target.componentType?.let(WrappingUtils::getWrapped) ?: elementClass(current)
        val items = plain.map { decodeElement(element, it) }
        return when {
            target.isArray -> WrappingUtils.richCast(items, target)
            target.isInstance(ArrayList<Any?>()) -> ArrayList(items)
            target.isInstance(LinkedHashSet<Any?>()) -> LinkedHashSet(items)
            else -> throw IllegalArgumentException("cannot build a ${target.name} from a share code")
        }
    }
    if (target.isInstance(plain)) return plain
    adapterFor(target)?.let { return it.deserialize(WrappingUtils.richCast(plain, it.outputClass)) }
    return WrappingUtils.richCast(plain, target).takeIf { target.isInstance(it) }
        ?: throw IllegalArgumentException("cannot read ${plain.javaClass.name} as ${target.name}")
}

private fun decodeMap(target: Class<*>, plain: Map<*, *>, current: Any?): Map<Any?, Any?> {
    val existing = current as? Map<*, *>
    val keyType = elementClass(existing?.keys)
    require(keyType == String::class.java || keyType == Any::class.java) { "cannot read the keys of a ${target.name} from a share code" }
    val valueType = elementClass(existing?.values)
    val out = LinkedHashMap<Any?, Any?>()
    for ((key, item) in plain) out[key] = decodeElement(valueType, item)
    require(target.isInstance(out)) { "cannot build a ${target.name} from a share code" }
    return out
}

private fun decodeElement(element: Class<*>, item: Any?): Any? {
    if (item == null) return null
    if (item is Map<*, *> && Map::class.java.isAssignableFrom(element)) return decodeMap(element, item, null)
    classTagged(item)?.let { tagged ->
        val cls = if (element == Any::class.java) runCatching { taggedClass(tagged) }.getOrNull() else taggedClass(tagged)
        if (cls != null) return deserializeTagged(element, tagged, cls)
    }
    if (Enum::class.java.isAssignableFrom(element)) return enumConstant(element, item)
    if (element.isInstance(item) || element == Any::class.java) return item
    adapterFor(element)?.let { return it.deserialize(WrappingUtils.richCast(item, it.outputClass)) }
    return WrappingUtils.richCast(item, element)
}

@Suppress("UNCHECKED_CAST")
private fun adapterFor(type: Class<*>): Adapter<Any?, Any?>? =
    ObjectSerializer.INSTANCE.getAdapter(type as Class<Any?>) as Adapter<Any?, Any?>?

@Suppress("UNCHECKED_CAST")
private fun classTagged(plain: Any): Map<String, Any?>? =
    (plain as? Map<String, Any?>)?.takeIf { it["class"] is String }

private fun taggedClass(map: Map<String, Any?>): Class<*> =
    Class.forName(map["class"] as String, false, ObjectSerializer::class.java.classLoader)

private fun deserializeTagged(target: Class<*>, map: Map<String, Any?>, cls: Class<*>): Any {
    val allowed = if (target == Any::class.java) cls.isEnum || adapterFor(cls)?.targetClass == cls else target.isAssignableFrom(cls)
    require(allowed) { "${cls.name} is not a ${target.name}" }
    return ObjectSerializer.INSTANCE.deserialize(map)
}

private fun enumConstant(type: Class<*>, value: Any): Any {
    val constants = (if (type.isEnum) type else type.superclass).enumConstants
        ?: throw IllegalArgumentException("${type.name} has no constants")
    return when (value) {
        is Number -> constants.getOrNull(value.toInt())
        is String -> constants.firstOrNull { (it as Enum<*>).name == value }
            ?: constants.firstOrNull { (it as Enum<*>).name.equals(value, ignoreCase = true) }
        else -> null
    } ?: throw IllegalArgumentException("$value is not a constant of ${type.name}")
}

private fun elementClass(current: Any?): Class<*> {
    val items: Iterable<*> = when (current) {
        is Iterable<*> -> current
        is kotlin.Array<*> -> current.asIterable()
        else -> return Any::class.java
    }
    val classes = items.mapNotNull { it?.javaClass }.distinct()
    return classes.singleOrNull() ?: Any::class.java
}
