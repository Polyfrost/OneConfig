/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1

import org.polyfrost.oneconfig.api.config.v1.visualize.Visualizer
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.utils.rgba
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Kotlin config class. allows to use the `by` keyword to create properties.
 *
 * **Do not use in Java sources**.
 */
open class KtConfig(id: String, title: String, category: Category, icon: PolyImage? = null) : Config(id, icon, title, category) {

    final override fun makeTree(id: String) = Tree.tree(id)

    /**
     * return the property with the given id by a kotlin property reference.
     */
    @Suppress("UNCHECKED_CAST")
    protected val <V> KProperty<V>.property: Property<V> get() = (tree.getProp(this.name) as Property<V>)

    /**
     * create a new delegate for the given property.
     */
    @JvmSynthetic
    protected inline fun <reified T> property(def: T? = null, name: String? = null, description: String? = null, visualizer: Class<out Visualizer>) = Provider(def, name, description, T::class.java, visualizer)

    @JvmSynthetic
    protected fun switch(def: Boolean = false, name: String? = null, description: String? = null) = Provider(def, name, description, Boolean::class.java, Visualizer.SwitchVisualizer::class.java)

    @JvmSynthetic
    protected fun color(def: PolyColor = rgba(0, 0, 0, 1f), name: String? = null, description: String? = null) = Provider(def, name, description, PolyColor::class.java, Visualizer.ColorVisualizer::class.java)

    @JvmSynthetic
    protected fun slider(min: Float = 0f, max: Float = 0f, def: Float = 0f, name: String? = null, description: String? = null) = Provider(def, name, description, Float::class.java, Visualizer.SliderVisualizer::class.java) {
        addMetadata("min", min)
        addMetadata("max", max)
    }

    @JvmSynthetic
    protected fun text(def: String = "", name: String? = null, description: String? = null) = Provider(def, name, description, String::class.java, Visualizer.TextVisualizer::class.java)

    @JvmSynthetic
    protected fun keybind(def: KeyBinder.Bind? = null, name: String? = null, description: String? = null) = Provider(def, name, description, KeyBinder.Bind::class.java, Visualizer.KeybindVisualizer::class.java)

    @JvmSynthetic
    protected fun radiobutton(options: Array<String>, def: Int = 0, name: String? = null, description: String? = null) = Provider(def, name, description, Int::class.java, Visualizer.RadioVisualizer::class.java) {
        addMetadata("options", options)
    }

    @JvmSynthetic
    protected fun dropdown(options: Array<String>, def: Int = 0, name: String? = null, description: String? = null) = Provider(def, name, description, Int::class.java, Visualizer.DropdownVisualizer::class.java) {
        addMetadata("options", options)
    }


    /**
     * provider for the [PropertyDelegate]. for some reason this has to be a class to avoid having to pass the reference directly.
     */
    protected class Provider<T>(
        private val def: T?,
        private val name: String?,
        private val description: String?,
        private val type: Class<T>,
        private val visualizer: Class<out Visualizer>,
        private val extra: (Property<T>.() -> Unit)? = null
    ) : PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, T>> {
        override operator fun provideDelegate(thisRef: KtConfig, property: KProperty<*>): ReadWriteProperty<KtConfig, T> {
            val p = Property.prop<T>(property.name, name ?: property.name, def, type)
            extra?.invoke(p)
            if (description != null) p.description = description
            p.addMetadata("visualizer", visualizer)
            thisRef.tree.put(p)
            return PropertyDelegate(p)
        }
    }

    /**
     * The actual delegate property. very simple.
     */
    private class PropertyDelegate<T>(val property: Property<T>) : ReadWriteProperty<KtConfig, T> {
        override operator fun getValue(thisRef: KtConfig, property: KProperty<*>): T {
            return this.property.get()!!
        }

        override operator fun setValue(thisRef: KtConfig, property: KProperty<*>, value: T) {
            this.property.set(value)
        }
    }
}