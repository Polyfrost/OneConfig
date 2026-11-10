package org.polyfrost.oneconfig.api.config.v1.dsl

import kotlin.jvm.java
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.ValueFormatter
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("OneConfig/Config")

/**
 * Experimental DSL for creating config trees
 *
 * **Note that for Java compatability** unlike most Kotlin DSLs this one requires you to add `.apply { ... }`
 * instead of just `{ ... }` to the end of any of the functions in order to configure them
 */
@ApiStatus.Experimental
public class ConfigDSL(id: String? = null, title: String? = null, description: String? = null) {
    public val tree: Tree = Tree(id, title, description, null)


    public fun subconfig(id: String, title: String? = null, description: String? = null): ConfigDSL {
        val out = ConfigDSL(id, title, description)
        tree.put(out.tree)
        return out
    }

    public fun accordion(id: String, title: String? = null, description: String? = null): ConfigDSL {
        val out = ConfigDSL(id, title, description)
        tree.put(out.tree)
        return out
    }

    public fun switch(default: Boolean): Prop<Boolean> = put(Prop(default, Boolean::class.java), Visualizer.SwitchVisualizer())

    public fun checkbox(default: Boolean): Prop<Boolean> = put(Prop(default, Boolean::class.java), Visualizer.CheckboxVisualizer())

    public fun button(function: Runnable): RunnableProp = put(RunnableProp(function), Visualizer.ButtonVisualizer())

    public fun color(default: PolyColor): ColorProp = put(ColorProp(default), Visualizer.ColorVisualizer())

    public fun text(default: String): TextProp = put(TextProp(default), Visualizer.TextVisualizer())

    public fun slider(default: Float): FloatProp = put(FloatProp(default), Visualizer.SliderVisualizer())

    public fun slider(default: Int): IntProp = put(IntProp(default), Visualizer.SliderVisualizer())

    public fun number(default: Float): FloatProp = put(FloatProp(default), Visualizer.NumberVisualizer())

    public fun number(default: Int): IntProp = put(IntProp(default), Visualizer.NumberVisualizer())
    public fun keybind(default: OneConfigKeybind?): Prop<OneConfigKeybind> = put(Prop(default, OneConfigKeybind::class.java), Visualizer.KeybindVisualizer())

    /** [type] is one of `info`, `success`, `warning` or `error` */
    public fun info(type: String = "info"): Prop<Void> = put(Prop(Properties.dummy()), Visualizer.InfoVisualizer()).also { it["type"] = type }

    /** Any number of [options] can be selected, the value holds one flag per option */
    public fun multiSelectDropdown(vararg options: String): OptionsProp<BooleanArray> =
        put(OptionsProp(BooleanArray(options.size), BooleanArray::class.java, options), Visualizer.MultiSelectDropdownVisualizer()).also { it["checkable"] = true }

    /** Holds the selected index of [options] or -1 for none */
    public fun singleSelectDropdown(defaultIndex: Int, vararg options: String): OptionsProp<Int> =
        put(OptionsProp(defaultIndex, Int::class.java, options), Visualizer.MultiSelectDropdownVisualizer()).also { it["checkable"] = false }

    /** A reorderable list, see [DraggableListProp.checkable] */
    public fun draggableList(default: Array<String>, vararg options: String): DraggableListProp =
        put(DraggableListProp(default, options), Visualizer.DraggableListVisualizer())

    /** Holds `[start, end]` */
    public fun rangeSlider(start: Float, end: Float): RangeProp = put(RangeProp(floatArrayOf(start, end)), Visualizer.RangeSliderVisualizer())

    /** Holds `[start, end]`, a [rangeSlider] without the track */
    public fun rangeNumber(start: Float, end: Float): RangeProp = put(RangeProp(floatArrayOf(start, end)), Visualizer.RangeNumberVisualizer())

    public fun file(default: String = ""): FileProp = put(FileProp(default), Visualizer.FileVisualizer())

    public fun textList(vararg default: String): TextListProp = put(TextListProp(arrayOf(*default)), Visualizer.TextListVisualizer())

    public fun numberList(vararg default: Float): NumberListProp = put(NumberListProp(default), Visualizer.NumberListVisualizer())

    public fun sliderList(vararg default: Float): NumberListProp = put(NumberListProp(default), Visualizer.SliderListVisualizer())

    /** Each range's start then end so `rangeSliderList(0f, 10f, 20f, 30f)` holds 0-10 and 20-30 */
    public fun rangeSliderList(vararg default: Float): NumberListProp {
        require(default.size % 2 == 0) { "rangeSliderList needs a start and end for every range, got ${default.size} values" }
        return put(NumberListProp(default), Visualizer.RangeSliderListVisualizer())
    }

    /** Stored the same way as [rangeSliderList] */
    public fun rangeNumberList(vararg default: Float): NumberListProp {
        require(default.size % 2 == 0) { "rangeNumberList needs a start and end for every range, got ${default.size} values" }
        return put(NumberListProp(default), Visualizer.RangeNumberListVisualizer())
    }

    /** ARGB colours */
    public fun colorList(vararg default: Int): ColorListProp = put(ColorListProp(default), Visualizer.ColorListVisualizer())

    public fun fileList(vararg default: String): FileListProp = put(FileListProp(arrayOf(*default)), Visualizer.FileListVisualizer())

    /** Namespaced item IDs such as `minecraft:diamond`, a `maxEntries` of 1 makes a single selector */
    public fun itemList(vararg default: String): ListProp<Array<String>> = put(ListProp(arrayOf(*default), Array<String>::class.java), Visualizer.ItemListVisualizer())

    /** Namespaced status effect IDs such as `minecraft:speed`, a `maxEntries` of 1 makes a single selector */
    public fun effectList(vararg default: String): ListProp<Array<String>> = put(ListProp(arrayOf(*default), Array<String>::class.java), Visualizer.EffectListVisualizer())

    /** Moves the saved value of [this] from any of [names] when loading older configs, needs [Prop.id] set first */
    public fun Prop<*>.previousNames(vararg names: String) {
        val id = requireNotNull(id) { "previousNames needs the property id to be set first" }
        val map = tree.getOrPutMetadata("migrationMap") { HashMap<String, String>() }
        for (name in names) map[name] = id
    }

    @Deprecated("Binary compatibility, returns OptionsProp now", level = DeprecationLevel.HIDDEN)
    @JvmName("dropdown")
    public fun dropdownCompat(defaultIndex: Int, vararg options: String): Prop<Int> = dropdown(defaultIndex, *options)

    @Deprecated("Binary compatibility, returns OptionsProp now", level = DeprecationLevel.HIDDEN)
    @JvmName("radiobutton")
    public fun radiobuttonCompat(defaultIndex: Int, vararg options: String): Prop<Int> = radiobutton(defaultIndex, *options)

    /** options join [tree] once they have an id since the tree is keyed by it */
    @PublishedApi
    internal fun <P : Prop<*>> put(prop: P, visualizer: Visualizer): P {
        prop["visualizer"] = visualizer
        prop.owner = tree
        if (prop.id != null) tree.put(prop.property)
        return prop
    }

    public fun dropdown(defaultIndex: Int, vararg options: String): OptionsProp<Int> =
        put(OptionsProp(defaultIndex, Int::class.java, options), Visualizer.DropdownVisualizer())

    public inline fun <reified T : Enum<*>> dropdown(default: T): Prop<T> = put(Prop(default, T::class.java), Visualizer.DropdownVisualizer())

    public fun radiobutton(defaultIndex: Int, vararg options: String): OptionsProp<Int> =
        put(OptionsProp(defaultIndex, Int::class.java, options), Visualizer.RadioVisualizer())

    public inline fun <reified T : Enum<*>> radiobutton(default: T): Prop<T> = put(Prop(default, T::class.java), Visualizer.RadioVisualizer())



    public class IntProp(default: Int) : Prop<Int>(default, Int::class.java) {
        public var min: Int
            get() = (this["min"] as Float).toInt()
            set(value) { this["min"] = value.toFloat() }

        public var max: Int
            get() = (this["max"] as Float).toInt()
            set(value) { this["max"] = value.toFloat() }

        public var unit: String?
            get() = this["unit"] as String?
            set(value) { this["unit"] = value }

        public var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }

        public var unitKey: String?
            get() = this["unitKey"] as String?
            set(value) { this["unitKey"] = value }

        public var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        public var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }

        public var formatter: ValueFormatter?
            get() = this["formatter"] as ValueFormatter?
            set(value) { this["formatter"] = value }
    }

    public class FloatProp(default: Float) : Prop<Float>(default, Float::class.java) {
        public var min: Float
            get() = this["min"] as Float
            set(value) { this["min"] = value }

        public var max: Float
            get() = this["max"] as Float
            set(value) { this["max"] = value }

        public var unit: String?
            get() = this["unit"] as String?
            set(value) { this["unit"] = value }

        public var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }

        public var unitKey: String?
            get() = this["unitKey"] as String?
            set(value) { this["unitKey"] = value }

        public var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        public var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }

        public var formatter: ValueFormatter?
            get() = this["formatter"] as ValueFormatter?
            set(value) { this["formatter"] = value }
    }

    public class RunnableProp(action: Runnable) : Prop<Runnable>(action, Runnable::class.java) {
        init {
            this["runnable"] = Runnable {
                runCatching { value?.run() }.onFailure { LOGGER.error("Failed to run button {}", id, it) }
            }
        }

        public var action: Runnable?
            get() = value
            set(value) { this.value = value }

        public var text: String?
            get() = this["text"] as String?
            set(value) { this["text"] = value }

        public var textKey: String?
            get() = this["textKey"] as String?
            set(value) { this["textKey"] = value }
    }

    public class ColorProp(default: PolyColor) : Prop<PolyColor>(default, PolyColor::class.java) {
        public var alpha: Boolean
            get() = this["noAlpha"] == null
            set(value) { if (value) property.removeMetadata("noAlpha") else this["noAlpha"] = Unit }

        public var swatch: Boolean
            get() = this["swatch"] == true
            set(value) { this["swatch"] = value }
    }

    public class TextProp(default: String) : Prop<String>(default, String::class.java) {
        public var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        public var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }

        public var multiline: Boolean
            get() = this["multiline"] as? Boolean ?: false
            set(value) { this["multiline"] = value }

        public var regex: String?
            get() = this["regex"] as String?
            set(value) { this["regex"] = value }
    }

    public open class OptionsProp<T : Any>(default: T, typeOfT: Class<T>, options: Array<out String>) : Prop<T>(default, typeOfT) {
        init { this["options"] = options }

        public var optionKeys: Array<String>?
            // only ever set by the setter below
            @Suppress("UNCHECKED_CAST")
            get() = this["optionsKey"] as Array<String>?
            set(value) { this["optionsKey"] = value }
    }

    public class DraggableListProp(default: Array<String>, options: Array<out String>) : OptionsProp<Array<String>>(default, Array<String>::class.java, options) {
        /** When true the value is the enabled subset of the options in order, otherwise the full ordered list */
        public var checkable: Boolean
            get() = this["checkable"] as? Boolean ?: false
            set(value) { this["checkable"] = value }
    }

    public class RangeProp(default: FloatArray) : Prop<FloatArray>(default, FloatArray::class.java) {
        public var min: Float?
            get() = this["min"] as Float?
            set(value) { this["min"] = value }

        public var max: Float?
            get() = this["max"] as Float?
            set(value) { this["max"] = value }

        public var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }

        public var formatter: ValueFormatter?
            get() = this["formatter"] as ValueFormatter?
            set(value) { this["formatter"] = value }
    }

    public class FileProp(default: String) : Prop<String>(default, String::class.java) {
        public var types: Array<String>?
            // only ever set by the setter below
            @Suppress("UNCHECKED_CAST")
            get() = this["types"] as Array<String>?
            set(value) { this["types"] = value }

        public var filterName: String?
            get() = this["filterName"] as String?
            set(value) { this["filterName"] = value }

        public var directory: Boolean
            get() = this["directory"] as? Boolean ?: false
            set(value) { this["directory"] = value }

        public var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        public var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }
    }

    public open class ListProp<T : Any>(default: T, typeOfT: Class<T>) : Prop<T>(default, typeOfT) {
        /** 0 for no limit */
        public var maxEntries: Int
            get() = this["maxEntries"] as? Int ?: 0
            set(value) {
                require(value >= 0) { "list '$id' has a negative maxEntries ($value)" }
                this["maxEntries"] = value
            }

        public var reorderable: Boolean
            get() = this["reorderable"] as? Boolean ?: true
            set(value) { this["reorderable"] = value }

        public var addText: String?
            get() = this["addText"] as String?
            set(value) { this["addText"] = value }

        public var addTextKey: String?
            get() = this["addTextKey"] as String?
            set(value) { this["addTextKey"] = value }
    }

    public class TextListProp(default: Array<String>) : ListProp<Array<String>>(default, Array<String>::class.java) {
        public var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        public var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }

        public var regex: String?
            get() = this["regex"] as String?
            set(value) { this["regex"] = value }
    }

    public class NumberListProp(default: FloatArray) : ListProp<FloatArray>(default, FloatArray::class.java) {
        public var min: Float?
            get() = this["min"] as Float?
            set(value) { this["min"] = value }

        public var max: Float?
            get() = this["max"] as Float?
            set(value) { this["max"] = value }

        public var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }

        public var formatter: ValueFormatter?
            get() = this["formatter"] as ValueFormatter?
            set(value) { this["formatter"] = value }
    }

    public class ColorListProp(default: IntArray) : ListProp<IntArray>(default, IntArray::class.java) {
        public var alpha: Boolean
            get() = this["noAlpha"] == null
            set(value) { if (value) property.removeMetadata("noAlpha") else this["noAlpha"] = Unit }
    }

    public class FileListProp(default: Array<String>) : ListProp<Array<String>>(default, Array<String>::class.java) {
        public var types: Array<String>?
            // only ever set by the setter below
            @Suppress("UNCHECKED_CAST")
            get() = this["types"] as Array<String>?
            set(value) { this["types"] = value }

        public var filterName: String?
            get() = this["filterName"] as String?
            set(value) { this["filterName"] = value }

        public var directory: Boolean
            get() = this["directory"] as? Boolean ?: false
            set(value) { this["directory"] = value }

        public var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        public var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }
    }

    public open class Prop<T : Any>(public val property: Property<T>) {
        public constructor(default: T?, typeOfT: Class<T>) : this(Properties.simple(null, null, null, default, typeOfT))

        public var value: T?
            get() = property.get()
            set(value) { property.set(value) }

        @PublishedApi
        internal var owner: Tree? = null

        public var id: String?
            get() = property.id
            set(value) {
                property.id = value
                owner?.put(property)
            }

        public var title: Any?
            get() = property.title
            set(value) { property.title = value }

        public var description: Any?
            get() = property.description
            set(value) { property.description = value }

        public var titleKey: String?
            get() = this["titleKey"] as String?
            set(value) { this["titleKey"] = value }

        public var descriptionKey: String?
            get() = this["descriptionKey"] as String?
            set(value) { this["descriptionKey"] = value }

        public var icon: String?
            get() = this["icon"] as String?
            set(value) { this["icon"] = value }

        public var category: String?
            get() = this["category"] as String?
            set(value) { this["category"] = value }

        public var categoryKey: String?
            get() = this["categoryKey"] as String?
            set(value) { this["categoryKey"] = value }

        public var subcategory: String?
            get() = this["subcategory"] as String?
            set(value) { this["subcategory"] = value }

        public var subcategoryKey: String?
            get() = this["subcategoryKey"] as String?
            set(value) { this["subcategoryKey"] = value }

        public var fullWidth: Boolean
            get() = this["fullWidth"] as? Boolean ?: false
            set(value) { this["fullWidth"] = value }

        /** Disables this option while [other] is off, or hides it when [hide] */
        public fun dependsOn(other: Prop<Boolean>, hide: Boolean = false) {
            property.addDisplayCondition(other.property, hide)
        }

        public operator fun set(key: String, value: Any?) {
            if (key == "min" || key == "max" || key == "step") requireStep(key, value)
            property.addMetadata(key, value)
        }

        private fun requireStep(key: String, value: Any?) {
            val vis = this["visualizer"]
            if (vis !is Visualizer.SliderVisualizer && vis !is Visualizer.RangeSliderVisualizer &&
                vis !is Visualizer.SliderListVisualizer && vis !is Visualizer.RangeSliderListVisualizer) return
            fun num(k: String) = ((if (k == key) value else this[k]) as? Number)?.toFloat()
            val min = num("min") ?: return
            val max = num("max") ?: return
            val step = num("step") ?: return
            require(step <= 0f || step <= max - min) { "slider '$id' has step ($step) larger than its range ($min to $max)" }
        }

        public operator fun get(key: String): Any? = property.getMetadata(key)
    }

    public companion object {
        @JvmStatic
        public inline fun config(id: String? = null, title: String? = null, description: String? = null, block: ConfigDSL.() -> Unit) {
            ConfigDSL(id, title, description).apply(block)
        }
    }
}