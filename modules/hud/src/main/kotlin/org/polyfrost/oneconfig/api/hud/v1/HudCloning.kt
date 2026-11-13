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
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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
    for (cf in cloneFields.get(hud.javaClass)) {
        try {
            val value = cf.field.get(hud) ?: continue
            val copy = deepCopyValue(value, cf.option, 0)
            if (copy !== value) cf.field.set(hud, copy)
        } catch (_: Exception) {
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun deepCopyValue(value: Any, nested: Boolean, depth: Int): Any = when (value) {
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
        mutableStateOf(value.value?.let { deepCopyValue(it, nested, depth) }, value.policy as SnapshotMutationPolicy<Any?>)
    is SnapshotStateList<*> -> mutableStateListOf<Any?>().also { out ->
        for (e in value) out.add(e?.let { deepCopyValue(it, nested, depth) })
    }
    is SnapshotStateMap<*, *> -> mutableStateMapOf<Any?, Any?>().also { out ->
        for ((k, v) in value) out[k] = v?.let { deepCopyValue(it, nested, depth) }
    }
    else -> copyContainer(value, nested, depth) ?: if (nested) copyNested(value, depth) else value
}

@Suppress("UNCHECKED_CAST")
private fun copyContainer(value: Any, nested: Boolean, depth: Int): Any? {
    val cls = value.javaClass
    if (cls.isArray) {
        val length = java.lang.reflect.Array.getLength(value)
        val out = java.lang.reflect.Array.newInstance(cls.componentType, length)
        System.arraycopy(value, 0, out, 0, length)
        if (!cls.componentType.isPrimitive) {
            for (i in 0 until length) {
                val e = java.lang.reflect.Array.get(out, i) ?: continue
                try {
                    java.lang.reflect.Array.set(out, i, deepCopyValue(e, nested, depth))
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        return out
    }
    fun copy(e: Any?): Any? = e?.let { deepCopyValue(it, nested, depth) }
    return when (cls) {
        ArrayList::class.java -> (value as List<Any?>).mapTo(ArrayList(value.size), ::copy)
        java.util.LinkedList::class.java -> (value as List<Any?>).mapTo(java.util.LinkedList(), ::copy)
        java.util.ArrayDeque::class.java -> (value as Collection<Any?>).mapTo(java.util.ArrayDeque(value.size), ::copy)
        HashSet::class.java -> (value as Set<Any?>).mapTo(HashSet(value.size), ::copy)
        LinkedHashSet::class.java -> (value as Set<Any?>).mapTo(LinkedHashSet(value.size), ::copy)
        HashMap::class.java -> (value as Map<Any?, Any?>).mapValuesTo(HashMap(value.size)) { copy(it.value) }
        LinkedHashMap::class.java -> (value as Map<Any?, Any?>).mapValuesTo(LinkedHashMap(value.size)) { copy(it.value) }
        else -> null
    }
}

private fun copyNested(value: Any, depth: Int): Any {
    if (depth >= MAX_NESTED_DEPTH || !nestedConfigClass.get(value.javaClass)) return value
    val copy = MHUtils.instantiate(value.javaClass, true).getOrNull() ?: return value
    try {
        var cls: Class<*>? = value.javaClass
        while (cls != null && cls != Any::class.java) {
            for (f in cls.declaredFields) {
                val mods = f.modifiers
                if (Modifier.isStatic(mods)) continue
                f.setAccessible(true)
                val v = f.get(value)
                val keep = f.type.isPrimitive || f.isSynthetic || Modifier.isTransient(mods)
                f.set(copy, if (keep || v == null) v else deepCopyValue(v, true, depth + 1))
            }
            cls = cls.superclass
        }
    } catch (_: Exception) {
        return value
    }
    return copy
}
