package org.polyfrost.oneconfig.api.config.v1

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
public class MetaKey<T> internal constructor(public val name: String, private val convert: (Any) -> T?) {
    @Suppress("UNCHECKED_CAST")
    public constructor(name: String, type: Class<T>) : this(name, { value ->
        if ((type as Class<Any>).kotlin.javaObjectType.isInstance(value)) value as T else null
    })

    internal fun read(value: Any?): T? = value?.let(convert)

    override fun equals(other: Any?): Boolean = other is MetaKey<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "MetaKey($name)"
}

@ApiStatus.Experimental
public object MetaKeys {
    @JvmField public val TITLE: MetaKey<String> = MetaKey("title", String::class.java)

    @JvmField public val DESCRIPTION: MetaKey<String> = MetaKey("description", String::class.java)

    @JvmField public val CATEGORY: MetaKey<String> = MetaKey("category", String::class.java)
    @JvmField public val SUBCATEGORY: MetaKey<String> = MetaKey("subcategory", String::class.java)

    @JvmField public val MIN: MetaKey<Number> = MetaKey("min", Number::class.java)

    @JvmField public val MAX: MetaKey<Number> = MetaKey("max", Number::class.java)

    @JvmField public val STEP: MetaKey<Number> = MetaKey("step", Number::class.java)

    @JvmField public val UNIT: MetaKey<String> = MetaKey("unit", String::class.java)

    @JvmField public val OPTIONS: MetaKey<List<String>> = MetaKey("options", ::stringList)

    @JvmField public val LABELS: MetaKey<List<String>> = MetaKey("optionLabels", ::stringList)

    @JvmField public val VISUALIZER: MetaKey<Any> = MetaKey("visualizer", Any::class.java)

    @JvmField public val COLLAPSED: MetaKey<Boolean> = MetaKey("collapsed", Boolean::class.java)

    @JvmField public val ICON: MetaKey<Any> = MetaKey("icon", Any::class.java)

    @JvmField public val ORDER: MetaKey<Int> = MetaKey("index", Int::class.java)

    @JvmField public val HIDDEN: MetaKey<Boolean> = MetaKey("hidden", Boolean::class.java)

    @JvmField public val FULL_WIDTH: MetaKey<Boolean> = MetaKey("fullWidth", Boolean::class.java)

    @JvmField public val HIDE_LABEL: MetaKey<Boolean> = MetaKey("hideLabel", Boolean::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun stringList(value: Any): List<String>? {
        val items = when (value) {
            is Array<*> -> value.asList()
            is Iterable<*> -> value.toList()
            else -> return null
        }
        return if (items.all { it is String }) items as List<String> else null
    }

    internal fun storedValue(key: MetaKey<*>, value: Any): Any = when (key.name) {
        MIN.name, MAX.name, STEP.name -> (value as? Number)?.toFloat() ?: value
        else -> value
    }
}

@ApiStatus.Experimental
public operator fun <T> Node.get(key: MetaKey<T>): T? = when (key.name) {
    MetaKeys.TITLE.name -> key.read(title)
    MetaKeys.DESCRIPTION.name -> key.read(description)
    else -> key.read(getMetadata<Any?>(key.name))
}

@ApiStatus.Experimental
public operator fun <T> Node.set(key: MetaKey<T>, value: T?) {
    when (key.name) {
        MetaKeys.TITLE.name -> replaceTitle(value)
        MetaKeys.DESCRIPTION.name -> description = value
        else -> if (value == null) removeMetadata(key.name) else addMetadata(key.name, MetaKeys.storedValue(key, value))
    }
}

@get:ApiStatus.Experimental
public val Tree.enableToggle: Property<Boolean>?
    @Suppress("UNCHECKED_CAST")
    get() = getMetadata<Any?>(ENABLE_TOGGLE_KEY) as? Property<Boolean>

public const val ENABLE_TOGGLE_KEY: String = "enableToggle"
