package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.annotations.Accordion
import org.polyfrost.oneconfig.api.config.v1.annotations.Include
import org.polyfrost.oneconfig.api.config.v1.annotations.MasterToggle
import org.polyfrost.oneconfig.api.config.v1.annotations.Option
import org.polyfrost.oneconfig.api.config.v1.copy
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.utils.v1.MHUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.EnumMap
import java.util.EnumSet
import java.util.IdentityHashMap
import java.util.LinkedList
import java.util.PriorityQueue
import java.util.SortedMap
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.lang.reflect.Array as ReflectArray

private const val MAX_NESTED_DEPTH = 4
private const val MAX_REBIND_DEPTH = 3

private class CloneField(val field: Field, val option: Boolean)

private class CloneContext(val clone: Hud, val source: Hud?) {
    val seen = IdentityHashMap<Any, Any>()
}

private fun Field.isOptionBacked(): Boolean = declaredAnnotations.any {
    val type = it.annotationClass.java
    type == Include::class.java || type == Accordion::class.java || type == MasterToggle::class.java ||
        type.isAnnotationPresent(Option::class.java)
}

private val cloneFields = object : ClassValue<List<CloneField>>() {
    override fun computeValue(type: Class<*>): List<CloneField> {
        val out = ArrayList<CloneField>()
        var cls: Class<*>? = type
        while (cls != null && cls != Hud::class.java && cls != Any::class.java) {
            for (f in cls.declaredFields) {
                val mods = f.modifiers
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic || f.type.isPrimitive) continue
                if (runCatching { f.setAccessible(true) }.isFailure) continue
                out += CloneField(f, f.isOptionBacked())
            }
            cls = cls.superclass
        }
        return out
    }
}

private val nestedConfigClass = object : ClassValue<Boolean>() {
    override fun computeValue(type: Class<*>): Boolean {
        if (type.isInterface || type.isEnum || type.isArray || Config::class.java.isAssignableFrom(type)) return false
        val name = type.name
        if (name.startsWith("java.") || name.startsWith("kotlin.") || name.startsWith("androidx.")) return false
        var cls: Class<*>? = type
        while (cls != null && cls != Any::class.java) {
            if (cls.declaredFields.any { it.isOptionBacked() }) return true
            cls = cls.superclass
        }
        return false
    }
}

private val functionFields = object : ClassValue<List<Field>?>() {
    override fun computeValue(type: Class<*>): List<Field>? {
        val out = ArrayList<Field>()
        var cls: Class<*>? = type
        while (cls != null && cls != Any::class.java) {
            for (f in cls.declaredFields) {
                if (Modifier.isStatic(f.modifiers)) continue
                if (runCatching { f.setAccessible(true) }.isFailure) return null
                out += f
            }
            cls = cls.superclass
        }
        val indexed = out.map { f -> LAMBDA_ARG.matchEntire(f.name)?.groupValues?.get(1)?.toIntOrNull() }
        if (out.isNotEmpty() && indexed.all { it != null }) return out.indices.sortedBy { indexed[it] }.map { out[it] }
        return out
    }
}

private val LAMBDA_ARG = Regex("arg\\$(\\d+)")

private val keybindAction: Field? by lazy {
    runCatching { OneConfigKeybind::class.java.getDeclaredField("action").also { it.isAccessible = true } }.getOrNull()
}

internal fun deepCopyHudOptions(hud: Hud) {
    deepCopyHudOptions(hud, null)
}

internal fun deepCopyHudOptions(hud: Hud, source: Hud?) {
    val ctx = CloneContext(hud, source)
    for (cf in cloneFields.get(hud.javaClass)) {
        try {
            val value = cf.field.get(hud) ?: continue
            val copy = deepCopyValue(value, cf.option, 0, ctx)
            if (copy !== value) cf.field.set(hud, copy)
        } catch (_: Exception) {
        }
    }
}

private fun deepCopyValue(value: Any, nested: Boolean, depth: Int, ctx: CloneContext): Any {
    val seen = ctx.seen
    seen[value]?.let { return it }
    seen[value] = value
    val result = deepCopyUncached(value, nested, depth, ctx)
    seen[value] = result
    return result
}

@Suppress("UNCHECKED_CAST")
private fun deepCopyUncached(value: Any, nested: Boolean, depth: Int, ctx: CloneContext): Any = when (value) {
    is String, is Number, is Boolean, is Char, is Enum<*> -> when (value) {
        is AtomicInteger -> AtomicInteger(value.get())
        is AtomicLong -> AtomicLong(value.get())
        else -> value
    }
    is PolyColor -> value.copy()
    is OneConfigKeybind -> copyKeybind(value, ctx)
    is MutableIntState -> mutableIntStateOf(value.intValue)
    is MutableLongState -> mutableLongStateOf(value.longValue)
    is MutableFloatState -> mutableFloatStateOf(value.floatValue)
    is MutableDoubleState -> mutableDoubleStateOf(value.doubleValue)
    is SnapshotMutableState<*> ->
        mutableStateOf(value.value?.let { deepCopyValue(it, nested, depth, ctx) }, value.policy as SnapshotMutationPolicy<Any?>)
    is SnapshotStateList<*> -> mutableStateListOf<Any?>().also { out ->
        ctx.seen[value] = out
        for (e in value) out.add(e?.let { deepCopyValue(it, nested, depth, ctx) })
    }
    is SnapshotStateMap<*, *> -> mutableStateMapOf<Any?, Any?>().also { out ->
        ctx.seen[value] = out
        for ((k, v) in value) out[k] = v?.let { deepCopyValue(it, nested, depth, ctx) }
    }
    else -> copyContainer(value, nested, depth, ctx) ?: if (nested) copyNested(value, depth, ctx) else value
}

private fun copyKeybind(bind: OneConfigKeybind, ctx: CloneContext): OneConfigKeybind {
    val registered = KeybindManager.isRegistered(bind)
    val source = ctx.source
    val action = if (source == null) bind.action else rebound(bind.action, source, ctx.clone, 0)
    if (action == null || registered && action === bind.action) {
        return if (registered) bind else bind.copyWith(bind.keyCodes?.copyOf(), bind.mouseBtns?.copyOf(), bind.mods)
    }
    val copy = bind.copyWith(bind.keyCodes?.copyOf(), bind.mouseBtns?.copyOf(), bind.mods)
    copy.unresolvedKeyInputs = bind.unresolvedKeyInputs
    copy.unresolvedMouseInputs = bind.unresolvedMouseInputs
    if (action !== bind.action) {
        val field = keybindAction ?: return if (registered) bind else copy
        try {
            field.set(copy, action)
        } catch (_: Exception) {
            return if (registered) bind else copy
        }
    }
    if (registered) ctx.clone.clonedKeybinds.add(copy)
    return copy
}

private fun isFunctionObject(cls: Class<*>): Boolean =
    Function::class.java.isAssignableFrom(cls) || cls.isSynthetic || cls.isAnonymousClass || cls.isHidden ||
        cls.name.contains("\$\$Lambda")

private fun rebound(value: Any, from: Any, to: Any, depth: Int): Any? {
    if (value === from) return to
    val cls = value.javaClass
    if (depth >= MAX_REBIND_DEPTH || !isFunctionObject(cls)) return value
    val fields = functionFields.get(cls) ?: return value
    if (fields.isEmpty()) return value
    val values = arrayOfNulls<Any>(fields.size)
    var changed = false
    try {
        for (i in fields.indices) {
            val f = fields[i]
            val v = f.get(value)
            if (v == null || f.type.isPrimitive) {
                values[i] = v
                continue
            }
            val r = rebound(v, from, to, depth + 1) ?: return null
            if (r !== v) changed = true
            values[i] = r
        }
    } catch (_: Exception) {
        return null
    }
    if (!changed) return value
    return rebuild(cls, fields, values)
}

private fun rebuild(cls: Class<*>, fields: List<Field>, values: Array<Any?>): Any? {
    if (fields.all { it.declaringClass == cls }) {
        val ctor = cls.declaredConstructors.firstOrNull { c ->
            val params = c.parameterTypes
            params.size == fields.size && params.indices.all { params[it] == fields[it].type }
        }
        if (ctor != null) {
            try {
                ctor.isAccessible = true
                return ctor.newInstance(*values)
            } catch (_: Exception) {
            }
        }
    }
    if (cls.isHidden) return null
    val out = MHUtils.instantiate(cls, true).getOrNull() ?: return null
    return try {
        for (i in fields.indices) fields[i].set(out, values[i])
        out
    } catch (_: Exception) {
        null
    }
}

@Suppress("UNCHECKED_CAST")
private fun copyContainer(value: Any, nested: Boolean, depth: Int, ctx: CloneContext): Any? {
    val seen = ctx.seen
    val cls = value.javaClass
    if (cls.isArray) {
        val length = ReflectArray.getLength(value)
        val out = ReflectArray.newInstance(cls.componentType, length)
        System.arraycopy(value, 0, out, 0, length)
        seen[value] = out
        if (!cls.componentType.isPrimitive) {
            for (i in 0 until length) {
                val e = ReflectArray.get(out, i) ?: continue
                try {
                    ReflectArray.set(out, i, deepCopyValue(e, nested, depth, ctx))
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        return out
    }
    fun copy(e: Any?): Any? = e?.let { deepCopyValue(it, nested, depth, ctx) }
    fun <C : MutableCollection<Any?>> fill(out: C, src: Collection<Any?>): C? {
        seen[value] = out
        return try {
            for (e in src) out.add(copy(e))
            out
        } catch (_: UnsupportedOperationException) {
            null
        }
    }
    fun <M : MutableMap<Any?, Any?>> fillMap(out: M, src: Map<Any?, Any?>): M? {
        seen[value] = out
        return try {
            for ((k, v) in src) out[k] = copy(v)
            out
        } catch (_: UnsupportedOperationException) {
            null
        }
    }
    return when (value) {
        is StringBuilder -> StringBuilder(value)
        is StringBuffer -> StringBuffer(value)
        is AtomicBoolean -> AtomicBoolean(value.get())
        is AtomicReference<*> -> AtomicReference(copy(value.get()))
        is Lazy<*> -> copyLazy(value, nested, depth, ctx)
        is EnumSet<*> -> value.clone()
        is EnumMap<*, *> -> EnumMap(value as EnumMap<Nothing, Any?>).also { out ->
            seen[value] = out
            for (e in out.entries) e.setValue(copy(e.value))
        }
        is Map<*, *> -> (value as Map<Any?, Any?>).let { src -> newMapLike(src)?.let { fillMap(it, src) } }
        is Collection<*> -> newCollectionLike(value)?.let { fill(it, value) }
        else -> null
    }
}

private fun copyLazy(value: Lazy<*>, nested: Boolean, depth: Int, ctx: CloneContext): Lazy<Any?> {
    if (value.isInitialized()) return lazyOf(value.value?.let { deepCopyValue(it, nested, depth, ctx) })
    val clone = ctx.clone
    val source = ctx.source
    return lazy { value.value?.let { deepCopyValue(it, nested, depth, CloneContext(clone, source)) } }
}

@Suppress("UNCHECKED_CAST")
private fun newMapLike(value: Map<Any?, Any?>): MutableMap<Any?, Any?>? = when (value.javaClass as Class<*>) {
    HashMap::class.java -> HashMap(value.size)
    LinkedHashMap::class.java -> LinkedHashMap(value.size)
    TreeMap::class.java -> TreeMap((value as SortedMap<Any?, Any?>).comparator())
    ConcurrentSkipListMap::class.java -> ConcurrentSkipListMap((value as SortedMap<Any?, Any?>).comparator())
    ConcurrentHashMap::class.java -> ConcurrentHashMap(value.size)
    else -> newLibraryInstance(value.javaClass) as? MutableMap<Any?, Any?>
}

@Suppress("UNCHECKED_CAST")
private fun newCollectionLike(value: Collection<Any?>): MutableCollection<Any?>? = when (value.javaClass as Class<*>) {
    ArrayList::class.java -> ArrayList(value.size)
    LinkedList::class.java -> LinkedList()
    ArrayDeque::class.java -> ArrayDeque(value.size)
    HashSet::class.java -> HashSet(value.size)
    LinkedHashSet::class.java -> LinkedHashSet(value.size)
    TreeSet::class.java -> TreeSet((value as SortedSet<Any?>).comparator())
    ConcurrentSkipListSet::class.java -> ConcurrentSkipListSet((value as SortedSet<Any?>).comparator())
    PriorityQueue::class.java -> PriorityQueue((value as PriorityQueue<Any?>).comparator())
    PriorityBlockingQueue::class.java ->
        PriorityBlockingQueue(maxOf(1, value.size), (value as PriorityBlockingQueue<Any?>).comparator())
    else -> if (value is ConcurrentHashMap.KeySetView<*, *>) ConcurrentHashMap.newKeySet()
    else newLibraryInstance(value.javaClass) as? MutableCollection<Any?>
}

private fun newLibraryInstance(cls: Class<*>): Any? {
    val name = cls.name
    if (!name.startsWith("java.util.") && !name.startsWith("kotlin.collections.")) return null
    if (!Modifier.isPublic(cls.modifiers) || Modifier.isAbstract(cls.modifiers)) return null
    return try {
        cls.getConstructor().newInstance()
    } catch (_: Exception) {
        null
    }
}

private fun copyNested(value: Any, depth: Int, ctx: CloneContext): Any {
    if (depth >= MAX_NESTED_DEPTH || !nestedConfigClass.get(value.javaClass)) return value
    val copy = MHUtils.instantiate(value.javaClass, true).getOrNull() ?: return value
    ctx.seen[value] = copy
    try {
        var cls: Class<*>? = value.javaClass
        while (cls != null && cls != Any::class.java) {
            for (f in cls.declaredFields) {
                val mods = f.modifiers
                if (Modifier.isStatic(mods)) continue
                f.setAccessible(true)
                val v = f.get(value)
                val keep = f.type.isPrimitive || f.isSynthetic || Modifier.isTransient(mods)
                f.set(copy, if (keep || v == null) v else deepCopyValue(v, true, depth + 1, ctx))
            }
            cls = cls.superclass
        }
    } catch (_: Exception) {
        return value
    }
    return copy
}
