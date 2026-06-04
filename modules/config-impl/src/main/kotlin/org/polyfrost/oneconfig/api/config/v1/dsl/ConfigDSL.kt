package org.polyfrost.oneconfig.api.config.v1.dsl

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import kotlin.jvm.java

/**
 * Experimental DSL for creating config trees.
 *
 * **Note that for Java compatability,** unlike most Kotlin DSLs, this on requires you to add `.apply { ... }`
 * instead of just `{ ... }` to the end of any the functions in order to configure them.
 */
@ApiStatus.Experimental
class ConfigDSL(id: String? = null, title: String? = null, description: String? = null) {
    val tree = Tree(id, title, description, null)


    fun subconfig(id: String? = null, title: String? = null, description: String? = null): ConfigDSL {
        val out = ConfigDSL(id, title, description)
        tree.put(out.tree)
        return out
    }

    fun accordion(id: String? = null, title: String? = null, description: String? = null): ConfigDSL {
        val out = ConfigDSL(id, title, description)
        tree.put(out.tree)
        return out
    }

    fun switch(default: Boolean): Prop<Boolean> {
        val prop = Prop(default, Boolean::class.java)
        prop["visualizer"] = Visualizer.SwitchVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun checkbox(default: Boolean): Prop<Boolean> {
        val prop = Prop(default, Boolean::class.java)
        prop["visualizer"] = Visualizer.CheckboxVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun button(function: Runnable): RunnableProp {
        val prop = RunnableProp(function)
        prop["visualizer"] = Visualizer.ButtonVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun color(default: PolyColor): ColorProp {
        val prop = ColorProp(default)
        prop["visualizer"] = Visualizer.ColorVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun text(default: String): TextProp {
        val prop = TextProp(default)
        prop["visualizer"] = Visualizer.TextVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun slider(default: Float): FloatProp {
        val prop = FloatProp(default)
        prop["visualizer"] = Visualizer.SliderVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun slider(default: Int): IntProp {
        val prop = IntProp(default)
        prop["visualizer"] = Visualizer.SliderVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun number(default: Float): FloatProp {
        val prop = FloatProp(default)
        prop["visualizer"] = Visualizer.NumberVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun number(default: Int): IntProp {
        val prop = IntProp(default)
        prop["visualizer"] = Visualizer.NumberVisualizer()
        tree.put(prop.property)
        return prop
    }
//      TODO: migrate
//    fun keybind(default: PolyBind): Prop<PolyBind> {
//        val prop = Prop(default, PolyBind::class.java)
//        prop["visualizer"] = Visualizer.KeybindVisualizer::class.java
//        tree.put(prop.property)
//        return prop
//    }

    fun dropdown(defaultIndex: Int, vararg options: String): Prop<Int> {
        val prop = Prop(defaultIndex, Int::class.java)
        prop["visualizer"] = Visualizer.DropdownVisualizer()
        prop["options"] = options
        tree.put(prop.property)
        return prop
    }

    inline fun <reified T : Enum<*>> dropdown(default: T): Prop<T> {
        val prop = Prop(default, T::class.java)
        prop["visualizer"] = Visualizer.DropdownVisualizer()
        tree.put(prop.property)
        return prop
    }

    fun radiobutton(defaultIndex: Int, vararg options: String): Prop<Int> {
        val prop = Prop(defaultIndex, Int::class.java)
        prop["visualizer"] = Visualizer.RadioVisualizer()
        prop["options"] = options
        tree.put(prop.property)
        return prop
    }

    inline fun <reified T : Enum<*>> radiobutton(default: T): Prop<T> {
        val prop = Prop(default, T::class.java)
        prop["visualizer"] = Visualizer.RadioVisualizer()
        tree.put(prop.property)
        return prop
    }



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
    }

    class RunnableProp(action: Runnable) : Prop<Runnable>(action, Runnable::class.java) {
        var action: Runnable?
            get() = value
            set(value) { this.value = value }

        var text: String?
            get() = this["text"] as String?
            set(value) { this["text"] = value }
    }

    class ColorProp(default: PolyColor) : Prop<PolyColor>(default, PolyColor::class.java) {
        var alpha: Boolean
            get() = this["alpha"] as Boolean
            set(value) { this["alpha"] = value }
    }

    class TextProp(default: String) : Prop<String>(default, String::class.java) {
        var placeholder: String?
            get() = this["placeholder"] as String?
            set(value) { this["placeholder"] = value }
    }


    open class Prop<T : Any>(default: T?, typeOfT: Class<T>) {
        val property = Properties.simple(null, null, null, default, typeOfT)

        var value
            get() = property.get()
            set(value) { property.set(value) }

        var id: String?
            get() = property.id
            set(value) { property.id = value }

        var title: Any?
            get() = property.title
            set(value) { property.title = value }

        var description: Any?
            get() = property.description
            set(value) { property.description = value }

        operator fun set(key: String, value: Any?) {
            property.addMetadata(key, value)
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