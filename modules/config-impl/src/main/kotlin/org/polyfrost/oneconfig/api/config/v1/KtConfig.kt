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

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import java.util.function.BooleanSupplier
import kotlin.jvm.java
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * Kotlin config class. allows to use the `by` keyword to create properties.
 *
 * **Do not use in Java sources**.
 */
open class KtConfig(id: String, title: String, category: Category, icon: String? = null) : Config(id, icon, title, category) {

    private var pendingTree: Tree? = null

    @JvmSynthetic
    internal fun pendingTree(): Tree {
        var p = pendingTree
        if (p == null) {
            p = Tree.tree(id)
            pendingTree = p
        }
        return p
    }

    final override fun makeTree(): Tree = pendingTree ?: Tree.tree(id)

    /**
     * return the property with the given id by a kotlin property reference.
     */
    @Suppress("UNCHECKED_CAST")
    protected val <V> KProperty<V>.property: Property<V>
        get() {
            val t = tree ?: pendingTree()
            return t.getProp(this.name) as Property<V>
        }

    /**
     * create a new delegate for the given property.
     */
    @JvmSynthetic
    protected inline fun <reified T> property(def: T? = null, name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null, visualizer: Visualizer) =
        Provider(def, name, description, category, subcategory, T::class.java, visualizer)

    @JvmSynthetic
    protected fun switch(def: Boolean = false, name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null) =
        Provider(def, name, description, category, subcategory, Boolean::class.java, Visualizer.SwitchVisualizer())

    @JvmSynthetic
    protected fun color(def: PolyColor = PolyColor.rgba(0, 0, 0, 255), name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null) =
        Provider(def, name, description, category, subcategory, PolyColor::class.java, Visualizer.ColorVisualizer())

    @JvmSynthetic
    protected fun slider(min: Float = 0f, max: Float = 0f, def: Float = 0f, name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null) =
        Provider(def, name, description, category, subcategory, Float::class.java, Visualizer.SliderVisualizer()) {
            addMetadata("min", min)
            addMetadata("max", max)
        }

    @JvmSynthetic
    @Deprecated(message = "Use other text method.", level = DeprecationLevel.HIDDEN)
    protected fun text(def: String = "", name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null) =
        Provider(def, name, description, category, subcategory, String::class.java, Visualizer.TextVisualizer())

    private fun Node.addMetadata(
        name: String,
        nameKey: String?,
        description: String?,
        descriptionKey: String?,
        icon: String?,
        multiline: Boolean,
        category: String?,
        categoryKey: String?,
        subcategory: String?,
        subcategoryKey: String?,
    ) {
        this.addMetadata("title", name)
        this.addMetadata("titleKey", nameKey)
        this.addMetadata("description", description)
        this.addMetadata("descriptionKey", descriptionKey)
        this.addMetadata("icon", icon)
        this.addMetadata("multiline", multiline)
        this.addMetadata("category", category)
        this.addMetadata("categoryKey", categoryKey)
        this.addMetadata("subcategory", subcategory)
        this.addMetadata("subcategoryKey", subcategoryKey)
    }

    @JvmSynthetic
    protected fun text(
        name: String,
        def: String = "",
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        multiline: Boolean = false,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        placeholder: String? = null,
        placeholderKey: String? = "polyui.textinput.placeholder"
    ) = Provider(def, name, description, category, subcategory, String::class.java, Visualizer.TextVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, multiline, category, categoryKey, subcategory, subcategoryKey)
        this.addMetadata("placeholder", placeholder)
        this.addMetadata("placeholderKey", placeholderKey)
    }

    @JvmSynthetic
    protected fun keybind(
        name: String,
        def: OneConfigKeybind? = null,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        multiline: Boolean = false,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
    ) =
        Provider(def, name, description, category, subcategory, OneConfigKeybind::class.java, Visualizer.KeybindVisualizer()) {
            addMetadata(name, nameKey, description, descriptionKey, icon, multiline, category, categoryKey, subcategory, subcategoryKey)
        }

    @JvmSynthetic
    protected fun radiobutton(options: Array<String>, def: Int = 0, name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null) =
        Provider(def, name, description, category, subcategory, Int::class.java, Visualizer.RadioVisualizer()) {
            addMetadata("options", options)
        }

    @JvmSynthetic
    protected fun dropdown(options: Array<String>, def: Int = 0, name: String? = null, description: String? = null, category: String? = null, subcategory: String? = null) =
        Provider(def, name, description, category, subcategory, Int::class.java, Visualizer.DropdownVisualizer()) {
            addMetadata("options", options)
        }

    fun hideIf(option: KProperty<*>, condition: () -> Boolean) {
        if (tree == null) initialize(false)
        hideIf(option.name, BooleanSupplier { condition() })
    }

    fun hideIf(option: KProperty<*>, condition: KProperty0<Boolean>) {
        if (tree == null) initialize(false)
        hideIf(option.name, BooleanSupplier { condition.get() })
    }

    fun <T> addCallback(option: KProperty<T>, callback: (T?) -> Boolean) {
        option.property.addCallback { callback(it) }
    }


    /**
     * provider for the [PropertyDelegate]. for some reason this has to be a class to avoid having to pass the reference directly.
     */
    protected class Provider<T>(
        private val def: T?,
        private val name: String?,
        private val description: String?,
        private val category: String?,
        private val subcategory: String?,
        private val type: Class<T>,
        private val visualizer: Visualizer,
        private val extra: (Property<T>.() -> Unit)? = null
    ) : PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, T>> {
        override operator fun provideDelegate(thisRef: KtConfig, property: KProperty<*>): ReadWriteProperty<KtConfig, T> {
            val p = Properties.simple(property.name, name ?: property.name, description, def, type)
            extra?.invoke(p)
            p.addMetadata("visualizer", visualizer)
            p.addMetadata("category", category)
            p.addMetadata("subcategory", subcategory)
            (if (thisRef.tree != null) thisRef.tree else thisRef.pendingTree()).put(p)
            return PropertyDelegate(p)
        }
    }

    /**
     * The actual delegate property. very simple.
     */
    private class PropertyDelegate<T>(val property: Property<T>) : ReadWriteProperty<KtConfig, T> {
        override operator fun getValue(thisRef: KtConfig, property: KProperty<*>): T = this.property.get() as T

        override operator fun setValue(thisRef: KtConfig, property: KProperty<*>, value: T) {
            this.property.set(value)
        }
    }
}