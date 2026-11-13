package org.polyfrost.oneconfig.api.config.v1

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
public class MetaKey<T>(public val name: String) {
    override fun equals(other: Any?): Boolean = other is MetaKey<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "MetaKey($name)"
}

@ApiStatus.Experimental
public object MetaKeys {
    @JvmField public val TITLE: MetaKey<String> = MetaKey("title")

    @JvmField public val DESCRIPTION: MetaKey<String> = MetaKey("description")

    @JvmField public val CATEGORY: MetaKey<String> = MetaKey("category")
    @JvmField public val SUBCATEGORY: MetaKey<String> = MetaKey("subcategory")

    @JvmField public val MIN: MetaKey<Number> = MetaKey("min")

    @JvmField public val MAX: MetaKey<Number> = MetaKey("max")

    @JvmField public val STEP: MetaKey<Number> = MetaKey("step")

    @JvmField public val UNIT: MetaKey<String> = MetaKey("unit")

    @JvmField public val OPTIONS: MetaKey<List<String>> = MetaKey("options")

    @JvmField public val LABELS: MetaKey<List<String>> = MetaKey("optionLabels")

    @JvmField public val VISUALIZER: MetaKey<Any> = MetaKey("visualizer")

    @JvmField public val COLLAPSED: MetaKey<Boolean> = MetaKey("collapsed")

    @JvmField public val ICON: MetaKey<Any> = MetaKey("icon")

    @JvmField public val ORDER: MetaKey<Int> = MetaKey("index")

    @JvmField public val HIDDEN: MetaKey<Boolean> = MetaKey("hidden")

    @JvmField public val FULL_WIDTH: MetaKey<Boolean> = MetaKey("fullWidth")

    @JvmField public val HIDE_LABEL: MetaKey<Boolean> = MetaKey("hideLabel")
}

@ApiStatus.Experimental
@Suppress("UNCHECKED_CAST")
public operator fun <T> Node.get(key: MetaKey<T>): T? = when (key.name) {
    MetaKeys.TITLE.name -> title as? T
    MetaKeys.DESCRIPTION.name -> description as? T
    else -> runCatching { getMetadata<Any?>(key.name) as? T }.getOrNull()
}

@ApiStatus.Experimental
public operator fun <T> Node.set(key: MetaKey<T>, value: T?) {
    when (key.name) {
        MetaKeys.TITLE.name -> title = value
        MetaKeys.DESCRIPTION.name -> description = value
        else -> if (value == null) removeMetadata(key.name) else addMetadata(key.name, value)
    }
}

@get:ApiStatus.Experimental
public val Tree.enableToggle: Property<Boolean>?
    @Suppress("UNCHECKED_CAST")
    get() = getMetadata<Any?>(ENABLE_TOGGLE_KEY) as? Property<Boolean>

public const val ENABLE_TOGGLE_KEY: String = "enableToggle"
