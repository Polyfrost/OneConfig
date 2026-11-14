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
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.IdentityHashMap
import java.util.LinkedList
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

private const val MAX_NESTED_DEPTH = 4

private class CloneField(val field: Field, val option: Boolean)

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

internal fun deepCopyHudOptions(hud: Hud) {
    val seen = IdentityHashMap<Any, Any>()
    for (cf in cloneFields.get(hud.javaClass)) {
        try {
            val value = cf.field.get(hud) ?: continue
            val copy = deepCopyValue(value, cf.option, 0, seen)
            if (copy !== value) cf.field.set(hud, copy)
        } catch (_: Exception) {
        }
    }
}

private fun deepCopyValue(value: Any, nested: Boolean, depth: Int, seen: IdentityHashMap<Any, Any>): Any {
    seen[value]?.let { return it }
    seen[value] = value
    val result = deepCopyUncached(value, nested, depth, seen)
    seen[value] = result
    return result
}

@Suppress("UNCHECKED_CAST")
private fun deepCopyUncached(value: Any, nested: Boolean, depth: Int, seen: IdentityHashMap<Any, Any>): Any = when (value) {
    is String, is Number, is Boolean, is Char, is Enum<*> -> value
    is PolyColor -> value.copy()
    is OneConfigKeybind ->
        if (KeybindManager.isRegistered(value)) value
        else value.copyWith(value.keyCodes?.copyOf(), value.mouseBtns?.copyOf(), value.mods)
    is MutableIntState -> mutableIntStateOf(value.intValue)
    is MutableLongState -> mutableLongStateOf(value.longValue)
    is MutableFloatState -> mutableFloatStateOf(value.floatValue)
    is MutableDoubleState -> mutableDoubleStateOf(value.doubleValue)
    is SnapshotMutableState<*> ->
        mutableStateOf(value.value?.let { deepCopyValue(it, nested, depth, seen) }, value.policy as SnapshotMutationPolicy<Any?>)
    is SnapshotStateList<*> -> mutableStateListOf<Any?>().also { out ->
        seen[value] = out
        for (e in value) out.add(e?.let { deepCopyValue(it, nested, depth, seen) })
    }
    is SnapshotStateMap<*, *> -> mutableStateMapOf<Any?, Any?>().also { out ->
        seen[value] = out
        for ((k, v) in value) out[k] = v?.let { deepCopyValue(it, nested, depth, seen) }
    }
    else -> copyContainer(value, nested, depth, seen) ?: if (nested) copyNested(value, depth, seen) else value
}

@Suppress("UNCHECKED_CAST")
private fun copyContainer(value: Any, nested: Boolean, depth: Int, seen: IdentityHashMap<Any, Any>): Any? {
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
                    ReflectArray.set(out, i, deepCopyValue(e, nested, depth, seen))
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        return out
    }
    fun copy(e: Any?): Any? = e?.let { deepCopyValue(it, nested, depth, seen) }
    fun <C : MutableCollection<Any?>> fill(out: C, src: Collection<Any?>): C {
        seen[value] = out
        for (e in src) out.add(copy(e))
        return out
    }
    fun <M : MutableMap<Any?, Any?>> fillMap(out: M, src: Map<Any?, Any?>): M {
        seen[value] = out
        for ((k, v) in src) out[k] = copy(v)
        return out
    }
    return when (cls) {
        ArrayList::class.java -> fill(ArrayList<Any?>((value as List<Any?>).size), value)
        LinkedList::class.java -> fill(LinkedList<Any?>(), value as List<Any?>)
        ArrayDeque::class.java -> fill(ArrayDeque<Any?>((value as Collection<Any?>).size), value)
        HashSet::class.java -> fill(HashSet<Any?>((value as Set<Any?>).size), value)
        LinkedHashSet::class.java -> fill(LinkedHashSet<Any?>((value as Set<Any?>).size), value)
        HashMap::class.java -> fillMap(HashMap<Any?, Any?>((value as Map<Any?, Any?>).size), value)
        LinkedHashMap::class.java -> fillMap(LinkedHashMap<Any?, Any?>((value as Map<Any?, Any?>).size), value)
        else -> null
    }
}

private fun copyNested(value: Any, depth: Int, seen: IdentityHashMap<Any, Any>): Any {
    if (depth >= MAX_NESTED_DEPTH || !nestedConfigClass.get(value.javaClass)) return value
    val copy = MHUtils.instantiate(value.javaClass, true).getOrNull() ?: return value
    seen[value] = copy
    try {
        var cls: Class<*>? = value.javaClass
        while (cls != null && cls != Any::class.java) {
            for (f in cls.declaredFields) {
                val mods = f.modifiers
                if (Modifier.isStatic(mods)) continue
                f.setAccessible(true)
                val v = f.get(value)
                val keep = f.type.isPrimitive || f.isSynthetic || Modifier.isTransient(mods)
                f.set(copy, if (keep || v == null) v else deepCopyValue(v, true, depth + 1, seen))
            }
            cls = cls.superclass
        }
    } catch (_: Exception) {
        return value
    }
    return copy
}
