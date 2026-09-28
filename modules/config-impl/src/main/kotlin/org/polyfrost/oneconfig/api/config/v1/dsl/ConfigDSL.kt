package org.polyfrost.oneconfig.api.config.v1.dsl

import kotlin.jvm.java
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind

/**
 * Experimental DSL for creating config trees
 *
 * **Note that for Java compatability** unlike most Kotlin DSLs this one requires you to add `.apply { ... }`
 * instead of just `{ ... }` to the end of any of the functions in order to configure them
 */
@ApiStatus.Experimental
class ConfigDSL(id: String? = null, title: String? = null, description: String? = null) {
    val tree = Tree(id, title, description, null)


    fun subconfig(id: String, title: String? = null, description: String? = null): ConfigDSL {
        val out = ConfigDSL(id, title, description)
        tree.put(out.tree)
        return out
    }

    fun accordion(id: String, title: String? = null, description: String? = null): ConfigDSL {
        val out = ConfigDSL(id, title, description)
        tree.put(out.tree)
        return out
    }

    fun switch(default: Boolean): Prop<Boolean> = put(Prop(default, Boolean::class.java), Visualizer.SwitchVisualizer())

    fun checkbox(default: Boolean): Prop<Boolean> = put(Prop(default, Boolean::class.java), Visualizer.CheckboxVisualizer())

    fun button(function: Runnable): RunnableProp = put(RunnableProp(function), Visualizer.ButtonVisualizer())

    fun color(default: PolyColor): ColorProp = put(ColorProp(default), Visualizer.ColorVisualizer())

    fun text(default: String): TextProp = put(TextProp(default), Visualizer.TextVisualizer())

    fun slider(default: Float): FloatProp = put(FloatProp(default), Visualizer.SliderVisualizer())

    fun slider(default: Int): IntProp = put(IntProp(default), Visualizer.SliderVisualizer())

    fun number(default: Float): FloatProp = put(FloatProp(default), Visualizer.NumberVisualizer())

    fun number(default: Int): IntProp = put(IntProp(default), Visualizer.NumberVisualizer())
    fun keybind(default: OneConfigKeybind?): Prop<OneConfigKeybind> = put(Prop(default, OneConfigKeybind::class.java), Visualizer.KeybindVisualizer())

    /** [type] is one of `info`, `success`, `warning` or `error` */
    fun info(type: String = "info"): Prop<Void> = put(Prop(Properties.dummy()), Visualizer.InfoVisualizer()).also { it["type"] = type }

    /** Any number of [options] can be selected, the value holds one flag per option */
    fun multiSelectDropdown(vararg options: String): OptionsProp<BooleanArray> =
        put(OptionsProp(BooleanArray(options.size), BooleanArray::class.java, options), Visualizer.MultiSelectDropdownVisualizer()).also { it["checkable"] = true }

    /** Holds the selected index of [options] or -1 for none */
    fun singleSelectDropdown(defaultIndex: Int, vararg options: String): OptionsProp<Int> =
        put(OptionsProp(defaultIndex, Int::class.java, options), Visualizer.MultiSelectDropdownVisualizer()).also { it["checkable"] = false }

    /** A reorderable list, see [DraggableListProp.checkable] */
    fun draggableList(default: Array<String>, vararg options: String): DraggableListProp =
        put(DraggableListProp(default, options), Visualizer.DraggableListVisualizer())

    /** Holds `[start, end]` */
    fun rangeSlider(start: Float, end: Float): RangeProp = put(RangeProp(floatArrayOf(start, end)), Visualizer.RangeSliderVisualizer())

    fun file(default: String = ""): FileProp = put(FileProp(default), Visualizer.FileVisualizer())

    fun textList(vararg default: String): TextListProp = put(TextListProp(arrayOf(*default)), Visualizer.TextListVisualizer())

    fun numberList(vararg default: Float): NumberListProp = put(NumberListProp(default), Visualizer.NumberListVisualizer())

    fun sliderList(vararg default: Float): NumberListProp = put(NumberListProp(default), Visualizer.SliderListVisualizer())

    /** ARGB colours */
    fun colorList(vararg default: Int): ColorListProp = put(ColorListProp(default), Visualizer.ColorListVisualizer())

    fun fileList(vararg default: String): FileListProp = put(FileListProp(arrayOf(*default)), Visualizer.FileListVisualizer())

    /** Namespaced item IDs such as `minecraft:diamond`, a `maxEntries` of 1 makes a single selector */
    fun itemList(vararg default: String): ListProp<Array<String>> = put(ListProp(arrayOf(*default), Array<String>::class.java), Visualizer.ItemListVisualizer())

    /** Moves the saved value of [this] from any of [names] when loading older configs, needs [Prop.id] set first */
    fun Prop<*>.previousNames(vararg names: String) {
        val id = requireNotNull(id) { "previousNames needs the property id to be set first" }
        val map = tree.getOrPutMetadata("migrationMap") { HashMap<String, String>() }
        for (name in names) map[name] = id
    }

    @Deprecated("Binary compatibility, returns OptionsProp now", level = DeprecationLevel.HIDDEN)
    @JvmName("dropdown")
    fun dropdownCompat(defaultIndex: Int, vararg options: String): Prop<Int> = dropdown(defaultIndex, *options)

    @Deprecated("Binary compatibility, returns OptionsProp now", level = DeprecationLevel.HIDDEN)
    @JvmName("radiobutton")
    fun radiobuttonCompat(defaultIndex: Int, vararg options: String): Prop<Int> = radiobutton(defaultIndex, *options)

    /** options join [tree] once they have an id since the tree is keyed by it */
    @PublishedApi
    internal fun <P : Prop<*>> put(prop: P, visualizer: Visualizer): P {
        prop["visualizer"] = visualizer
        prop.owner = tree
        if (prop.id != null) tree.put(prop.property)
        return prop
    }

    fun dropdown(defaultIndex: Int, vararg options: String): OptionsProp<Int> =
        put(OptionsProp(defaultIndex, Int::class.java, options), Visualizer.DropdownVisualizer())

    inline fun <reified T : Enum<*>> dropdown(default: T): Prop<T> = put(Prop(default, T::class.java), Visualizer.DropdownVisualizer())

    fun radiobutton(defaultIndex: Int, vararg options: String): OptionsProp<Int> =
        put(OptionsProp(defaultIndex, Int::class.java, options), Visualizer.RadioVisualizer())

    inline fun <reified T : Enum<*>> radiobutton(default: T): Prop<T> = put(Prop(default, T::class.java), Visualizer.RadioVisualizer())



    class IntProp(default: Int) : Prop<Int>(default, Int::class.java) {
        var min: Int
            get() = (this["min"] as Float).toInt()
            set(value) { this["min"] = value.toFloat() }

        var max: Int
            get() = (this["max"] as Float).toInt()
            set(value) { this["max"] = value.toFloat() }

        var unit: String?
            get() = this["unit"] as String?
            set(value) { this["unit"] = value }

        var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }

        var unitKey: String?
            get() = this["unitKey"] as String?
            set(value) { this["unitKey"] = value }

        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }
    }

    class FloatProp(default: Float) : Prop<Float>(default, Float::class.java) {
        var min: Float
            get() = this["min"] as Float
            set(value) { this["min"] = value }

        var max: Float
            get() = this["max"] as Float
            set(value) { this["max"] = value }

        var unit: String?
            get() = this["unit"] as String?
            set(value) { this["unit"] = value }

        var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }

        var unitKey: String?
            get() = this["unitKey"] as String?
            set(value) { this["unitKey"] = value }

        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }
    }

    class RunnableProp(action: Runnable) : Prop<Runnable>(action, Runnable::class.java) {
        var action: Runnable?
            get() = value
            set(value) { this.value = value }

        var text: String?
            get() = this["text"] as String?
            set(value) { this["text"] = value }

        var textKey: String?
            get() = this["textKey"] as String?
            set(value) { this["textKey"] = value }
    }

    class ColorProp(default: PolyColor) : Prop<PolyColor>(default, PolyColor::class.java) {
        var alpha: Boolean
            get() = this["noAlpha"] == null
            set(value) { if (value) property.removeMetadata("noAlpha") else this["noAlpha"] = Unit }
    }

    class TextProp(default: String) : Prop<String>(default, String::class.java) {
        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }

        var multiline: Boolean
            get() = this["multiline"] as? Boolean ?: false
            set(value) { this["multiline"] = value }

        var regex: String?
            get() = this["regex"] as String?
            set(value) { this["regex"] = value }
    }

    open class OptionsProp<T : Any>(default: T, typeOfT: Class<T>, options: Array<out String>) : Prop<T>(default, typeOfT) {
        init { this["options"] = options }

        var optionKeys: Array<String>?
            get() = this["optionsKey"] as Array<String>?
            set(value) { this["optionsKey"] = value }
    }

    class DraggableListProp(default: Array<String>, options: Array<out String>) : OptionsProp<Array<String>>(default, Array<String>::class.java, options) {
        /** When true the value is the enabled subset of the options in order, otherwise the full ordered list */
        var checkable: Boolean
            get() = this["checkable"] as? Boolean ?: false
            set(value) { this["checkable"] = value }
    }

    class RangeProp(default: FloatArray) : Prop<FloatArray>(default, FloatArray::class.java) {
        var min: Float?
            get() = this["min"] as Float?
            set(value) { this["min"] = value }

        var max: Float?
            get() = this["max"] as Float?
            set(value) { this["max"] = value }

        var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }
    }

    class FileProp(default: String) : Prop<String>(default, String::class.java) {
        var types: Array<String>?
            get() = this["types"] as Array<String>?
            set(value) { this["types"] = value }

        var filterName: String?
            get() = this["filterName"] as String?
            set(value) { this["filterName"] = value }

        var directory: Boolean
            get() = this["directory"] as? Boolean ?: false
            set(value) { this["directory"] = value }

        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }
    }

    open class ListProp<T : Any>(default: T, typeOfT: Class<T>) : Prop<T>(default, typeOfT) {
        /** 0 for no limit */
        var maxEntries: Int
            get() = this["maxEntries"] as? Int ?: 0
            set(value) {
                require(value >= 0) { "list '$id' has a negative maxEntries ($value)" }
                this["maxEntries"] = value
            }

        var reorderable: Boolean
            get() = this["reorderable"] as? Boolean ?: true
            set(value) { this["reorderable"] = value }

        var addText: String?
            get() = this["addText"] as String?
            set(value) { this["addText"] = value }

        var addTextKey: String?
            get() = this["addTextKey"] as String?
            set(value) { this["addTextKey"] = value }
    }

    class TextListProp(default: Array<String>) : ListProp<Array<String>>(default, Array<String>::class.java) {
        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }

        var regex: String?
            get() = this["regex"] as String?
            set(value) { this["regex"] = value }
    }

    class NumberListProp(default: FloatArray) : ListProp<FloatArray>(default, FloatArray::class.java) {
        var min: Float?
            get() = this["min"] as Float?
            set(value) { this["min"] = value }

        var max: Float?
            get() = this["max"] as Float?
            set(value) { this["max"] = value }

        var step: Float?
            get() = this["step"] as Float?
            set(value) { this["step"] = value }
    }

    class ColorListProp(default: IntArray) : ListProp<IntArray>(default, IntArray::class.java) {
        var alpha: Boolean
            get() = this["noAlpha"] == null
            set(value) { if (value) property.removeMetadata("noAlpha") else this["noAlpha"] = Unit }
    }

    class FileListProp(default: Array<String>) : ListProp<Array<String>>(default, Array<String>::class.java) {
        var types: Array<String>?
            get() = this["types"] as Array<String>?
            set(value) { this["types"] = value }

        var filterName: String?
            get() = this["filterName"] as String?
            set(value) { this["filterName"] = value }

        var directory: Boolean
            get() = this["directory"] as? Boolean ?: false
            set(value) { this["directory"] = value }

        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }

        var placeholderKey: String?
            get() = this["placeholderKey"] as String?
            set(value) { this["placeholderKey"] = value }
    }

    open class Prop<T : Any>(val property: Property<T>) {
        constructor(default: T?, typeOfT: Class<T>) : this(Properties.simple(null, null, null, default, typeOfT))

        var value
            get() = property.get()
            set(value) { property.set(value) }

        @PublishedApi
        internal var owner: Tree? = null

        var id: String?
            get() = property.id
            set(value) {
                property.id = value
                owner?.put(property)
            }

        var title: Any?
            get() = property.title
            set(value) { property.title = value }

        var description: Any?
            get() = property.description
            set(value) { property.description = value }

        var titleKey: String?
            get() = this["titleKey"] as String?
            set(value) { this["titleKey"] = value }

        var descriptionKey: String?
            get() = this["descriptionKey"] as String?
            set(value) { this["descriptionKey"] = value }

        var icon: String?
            get() = this["icon"] as String?
            set(value) { this["icon"] = value }

        var category: String?
            get() = this["category"] as String?
            set(value) { this["category"] = value }

        var categoryKey: String?
            get() = this["categoryKey"] as String?
            set(value) { this["categoryKey"] = value }

        var subcategory: String?
            get() = this["subcategory"] as String?
            set(value) { this["subcategory"] = value }

        var subcategoryKey: String?
            get() = this["subcategoryKey"] as String?
            set(value) { this["subcategoryKey"] = value }

        /** Disables this option while [other] is off, or hides it when [hide] */
        fun dependsOn(other: Prop<Boolean>, hide: Boolean = false) {
            property.addDisplayCondition(other.property, hide)
        }

        operator fun set(key: String, value: Any?) {
            property.addMetadata(key, value)
            if (key == "min" || key == "max" || key == "step") requireStep()
        }

        /** checked on every change so it only fires once min, max and step are all set */
        private fun requireStep() {
            val min = (this["min"] as? Number)?.toFloat() ?: return
            val max = (this["max"] as? Number)?.toFloat() ?: return
            val step = (this["step"] as? Number)?.toFloat() ?: return
            require(step <= 0f || step <= max - min) { "slider '$id' has step ($step) larger than its range ($min to $max)" }
        }

        operator fun get(key: String): Any? = property.getMetadata(key)
    }

    @DslMarker
    private annotation class ConfigDSLMarker

    companion object {
        @ConfigDSLMarker
        @JvmStatic
        inline fun config(id: String? = null, title: String? = null, description: String? = null, block: ConfigDSL.() -> Unit) {
            ConfigDSL(id, title, description).apply(block)
        }
    }
}