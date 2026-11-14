@file:JvmName("TreeBuilders")

package org.polyfrost.oneconfig.api.config.v1

import java.util.function.Supplier
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind

private val LOGGER = LogManager.getLogger("OneConfig/Config")

private const val TEXT_PLACEHOLDER_KEY = "oneconfig.textinput.placeholder"

private const val GROUP_TOGGLE_ID = "enabled"

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.switch(
    id: String,
    title: String,
    default: Boolean,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<Boolean> =
    option(Properties.simple(id, title, description, default, Boolean::class.java), Visualizer.SwitchVisualizer::class.java, category, subcategory)

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.slider(
    id: String,
    title: String,
    default: Float,
    min: Float,
    max: Float,
    step: Float = 0f,
    unit: String? = null,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<Float> {
    require(step <= 0f || step <= max - min) { "slider '$id' has step ($step) larger than its range ($min to $max)" }
    return option(Properties.simple(id, title, description, default, Float::class.java), Visualizer.SliderVisualizer::class.java, category, subcategory) {
        this[MetaKeys.MIN] = min
        this[MetaKeys.MAX] = max
        if (step != 0f) this[MetaKeys.STEP] = step
        this[MetaKeys.UNIT] = unit
    }
}

@ApiStatus.Experimental
@JvmOverloads
public fun <E : Enum<E>> Tree.dropdown(
    id: String,
    title: String,
    default: E,
    labels: Map<E, String> = emptyMap(),
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<E> =
    option(Properties.simple(id, title, description, default, default.declaringJavaClass), Visualizer.DropdownVisualizer::class.java, category, subcategory) {
        if (labels.isNotEmpty()) this[MetaKeys.LABELS] = default.declaringJavaClass.enumConstants.map { labels[it] ?: it.name }
    }

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.dropdown(
    id: String,
    title: String,
    default: Int,
    options: List<String>,
    labels: Map<String, String> = emptyMap(),
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<Int> =
    option(Properties.simple(id, title, description, default, Int::class.java), Visualizer.DropdownVisualizer::class.java, category, subcategory) {
        this[MetaKeys.OPTIONS] = options.toList()
        if (labels.isNotEmpty()) this[MetaKeys.LABELS] = options.map { labels[it] ?: it }
    }

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.color(
    id: String,
    title: String,
    default: PolyColor,
    alpha: Boolean = true,
    swatch: Boolean = false,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<PolyColor> =
    option(Properties.simple(id, title, description, default, PolyColor::class.java), Visualizer.ColorVisualizer::class.java, category, subcategory) {
        if (!alpha) addMetadata("noAlpha", Unit)
        if (swatch) addMetadata("swatch", true)
    }

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.button(
    id: String,
    title: String,
    text: String,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
    action: () -> Unit,
): Property<Void> =
    option(Properties.dummy(id, title, description), Visualizer.ButtonVisualizer::class.java, category, subcategory) {
        addMetadata("text", text)
        addMetadata("runnable", Runnable {
            runCatching(action).onFailure { LOGGER.error("Failed to run button {}", id, it) }
        })
    }

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.text(
    id: String,
    title: String,
    default: String = "",
    placeholder: String? = null,
    multiline: Boolean = false,
    regex: String? = null,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<String> =
    option(Properties.simple(id, title, description, default, String::class.java), Visualizer.TextVisualizer::class.java, category, subcategory) {
        addMetadata("placeholder", placeholder)
        if (placeholder.isNullOrEmpty()) addMetadata("placeholderKey", TEXT_PLACEHOLDER_KEY)
        addMetadata("multiline", multiline)
        addMetadata("regex", regex)
    }

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.keybind(
    id: String,
    title: String,
    default: OneConfigKeybind? = null,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
): Property<OneConfigKeybind> =
    option(Properties.simple(id, title, description, default, OneConfigKeybind::class.java), Visualizer.KeybindVisualizer::class.java, category, subcategory)

@ApiStatus.Experimental
public fun <T : Any> Tree.hidden(id: String, default: T): Property<T> {
    val cls = default.javaClass
    @Suppress("UNCHECKED_CAST")
    val type = (if (!cls.isEnum && cls.superclass?.isEnum == true) cls.superclass else cls) as Class<T>
    return option(Properties.simple(id, null, null, default, type), null, null, null) {
        this[MetaKeys.HIDDEN] = true
    }
}

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.group(
    id: String,
    title: String,
    collapsed: Boolean = false,
    enableToggle: Boolean = false,
    enabledByDefault: Boolean = true,
    description: String = "",
    category: String? = null,
    subcategory: String? = null,
    block: Tree.() -> Unit,
): Tree {
    val group = Tree(id, title, description, null)
    group[MetaKeys.CATEGORY] = category
    group[MetaKeys.SUBCATEGORY] = subcategory
    if (collapsed) group[MetaKeys.COLLAPSED] = true
    val toggle = if (enableToggle) {
        Properties.simple(GROUP_TOGGLE_ID, title, null, enabledByDefault, Boolean::class.java).also {
            group.put(it)
            group.addMetadata(ENABLE_TOGGLE_KEY, it)
        }
    } else null
    group.block()
    group.onAll { childId, node ->
        check(node !is Tree) { "Group $id contains the nested group $childId, but groups cannot be nested; put the options in the outer group or use a submenu" }
    }
    if (toggle != null) {
        check(group.get(GROUP_TOGGLE_ID) === toggle) { "Group $id uses an enable toggle, so its options cannot use the id $GROUP_TOGGLE_ID" }
        group.disableUnless(toggle)
    }
    put(group)
    return group
}

private fun Tree.disableUnless(toggle: Property<Boolean>) {
    onAll { _, node ->
        when (node) {
            is Tree -> node.disableUnless(toggle)
            is Property<*> -> if (node !== toggle) {
                node.addDisplayCondition(Supplier { if (toggle.get() == false) Property.Display.DISABLED else Property.Display.SHOWN })
                toggle.addDependent(node)
            }
        }
    }
}

private inline fun <P : Property<*>> Tree.option(
    property: P,
    visualizer: Class<out Visualizer>?,
    category: String?,
    subcategory: String?,
    extra: P.() -> Unit = {},
): P {
    if (visualizer != null) property[MetaKeys.VISUALIZER] = visualizer
    property[MetaKeys.CATEGORY] = category
    property[MetaKeys.SUBCATEGORY] = subcategory
    property.extra()
    put(property)
    return property
}
