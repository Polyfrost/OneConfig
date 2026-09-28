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

import kotlin.jvm.java
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind

/**
 * Kotlin config class which allows using the `by` keyword to create properties
 *
 * **Do not use in Java sources**
 */
open class KtConfig(id: String, title: String, category: Category, icon: String? = null) :
    Config(id, icon, title, category) {

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
     * return the property with the given id by a kotlin property reference
     */
    @Suppress("UNCHECKED_CAST")
    protected val <V> KProperty<V>.property: Property<V>
        get() {
            val t = tree ?: pendingTree()
            return t.getProp(this.name) as Property<V>
        }

    @JvmSynthetic
    protected inline fun <reified T : Any> property(
        def: T? = null,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null,
        visualizer: Visualizer
    ) =
        Provider(def, name, description, category, subcategory, T::class.java, visualizer)

    @JvmSynthetic
    protected fun switch(
        def: Boolean = false,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null
    ) =
        Provider(def, name, description, category, subcategory, Boolean::class.java, Visualizer.SwitchVisualizer())

    protected fun color(
        name: String,
        def: PolyColor = PolyColor.rgba(0, 0, 0, 255),
        alpha: Boolean = true,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
    ) = Provider(def, name, description, category, subcategory, PolyColor::class.java, Visualizer.ColorVisualizer()) {
        addMetadata(
            name,
            nameKey,
            description,
            descriptionKey,
            icon,
            category,
            categoryKey,
            subcategory,
            subcategoryKey
        )
        if (!alpha) this.addMetadata("noAlpha", Unit)
    }

    @JvmSynthetic
    protected fun color(
        def: PolyColor = PolyColor.rgba(0, 0, 0, 255),
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null
    ) =
        Provider(def, name, description, category, subcategory, PolyColor::class.java, Visualizer.ColorVisualizer())

    @JvmSynthetic
    protected fun slider(
        min: Float = 0f,
        max: Float = 0f,
        def: Float = 0f,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null
    ) =
        Provider(def, name, description, category, subcategory, Float::class.java, Visualizer.SliderVisualizer()) {
            addMetadata("min", min)
            addMetadata("max", max)
        }

    @JvmSynthetic
    @Deprecated(message = "Use other text method.", level = DeprecationLevel.HIDDEN)
    protected fun text(
        def: String = "",
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null
    ) =
        Provider(def, name, description, category, subcategory, String::class.java, Visualizer.TextVisualizer())

    private fun Node.addMetadata(
        name: String,
        nameKey: String?,
        description: String?,
        descriptionKey: String?,
        icon: String?,
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
        placeholderKey: String? = "oneconfig.textinput.placeholder",
        regex: String? = null
    ) = Provider(def, name, description, category, subcategory, String::class.java, Visualizer.TextVisualizer()) {
        addMetadata(
            name,
            nameKey,
            description,
            descriptionKey,
            icon,
            category,
            categoryKey,
            subcategory,
            subcategoryKey
        )
        this.addMetadata("placeholder", placeholder)
        this.addMetadata("multiline", multiline)
        this.addMetadata("placeholderKey", placeholderKey)
        this.addMetadata("regex", regex)
    }

    @JvmSynthetic
    @Deprecated(message = "Binary compatibility for mods compiled before regex was added", level = DeprecationLevel.HIDDEN)
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
        placeholderKey: String? = "oneconfig.textinput.placeholder"
    ) = text(name, def, nameKey, description, descriptionKey, icon, multiline, category, categoryKey, subcategory, subcategoryKey, placeholder, placeholderKey, null)

    @JvmSynthetic
    protected fun checkbox(
        name: String,
        def: Boolean,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
    ) = Provider(def, name, description, category, subcategory, Boolean::class.java, Visualizer.CheckboxVisualizer()) {
        addMetadata(
            name,
            nameKey,
            description,
            descriptionKey,
            icon,
            category,
            categoryKey,
            subcategory,
            subcategoryKey
        )
    }

    @JvmSynthetic
    protected fun switch(
        name: String,
        def: Boolean,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
    ) = Provider(def, name, description, category, subcategory, Boolean::class.java, Visualizer.SwitchVisualizer()) {
        addMetadata(
            name,
            nameKey,
            description,
            descriptionKey,
            icon,
            category,
            categoryKey,
            subcategory,
            subcategoryKey
        )
    }

    @JvmSynthetic
    protected fun number(
        name: String,
        def: Float,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,

        unit: String? = null,
        unitKey: String? = null,
        min: Float = -10f,
        max: Float = 100f,
        placeholder: String? = null,
        placeholderKey: String? = "oneconfig.numberinput.placeholder"
    ) = Provider(def, name, description, category, subcategory, Float::class.java, Visualizer.NumberVisualizer()) {
        addMetadata(
            name,
            nameKey,
            description,
            descriptionKey,
            icon,
            category,
            categoryKey,
            subcategory,
            subcategoryKey
        )
        this.addMetadata("unit", unit)
        this.addMetadata("unitKey", unitKey)
        this.addMetadata("min", min)
        this.addMetadata("max", max)
        this.addMetadata("placeholder", placeholder)
        this.addMetadata("placeholderKey", placeholderKey)
    }

    @JvmSynthetic
    protected fun slider(
        name: String,
        def: Float,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,

        unit: String? = null,
        unitKey: String? = null,
        min: Float = -10f,
        max: Float = 100f,
        step: Float = 1f,
    ) = Provider(def, name, description, category, subcategory, Float::class.java, Visualizer.SliderVisualizer()) {
        requireStep(name, min, max, step)
        addMetadata(
            name,
            nameKey,
            description,
            descriptionKey,
            icon,
            category,
            categoryKey,
            subcategory,
            subcategoryKey
        )
        this.addMetadata("unit", unit)
        this.addMetadata("unitKey", unitKey)
        this.addMetadata("min", min)
        this.addMetadata("max", max)
        this.addMetadata("step", step)
    }

    @JvmSynthetic
    protected fun keybind(
        name: String,
        def: OneConfigKeybind? = null,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
    ) =
        Provider(
            def,
            name,
            description,
            category,
            subcategory,
            OneConfigKeybind::class.java,
            Visualizer.KeybindVisualizer()
        ) {
            addMetadata(
                name,
                nameKey,
                description,
                descriptionKey,
                icon,
                category,
                categoryKey,
                subcategory,
                subcategoryKey
            )
        }

    @JvmSynthetic
    protected fun radiobutton(
        options: Array<String>,
        def: Int = 0,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null
    ) =
        Provider(def, name, description, category, subcategory, Int::class.java, Visualizer.RadioVisualizer()) {
            addMetadata("options", options)
        }

    @JvmSynthetic
    protected fun dropdown(
        options: Array<String>,
        def: Int = 0,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        subcategory: String? = null
    ) =
        Provider(def, name, description, category, subcategory, Int::class.java, Visualizer.DropdownVisualizer()) {
            addMetadata("options", options)
        }

    protected fun <Type : Any> dropdown(
        name: String,
        defaultOption: Type,
        options: Array<Type>,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        optionKeys: Array<String> = arrayOf(),
        stringTransformer: (Type) -> String = { it.toString() },
    ): PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>> {
        val selectedIndex = options.indexOf(defaultOption)

        val property = Provider(
            selectedIndex,
            name,
            description,
            category,
            subcategory,
            Int::class.java,
            Visualizer.DropdownVisualizer()
        ) {
            addMetadata(
                name,
                nameKey,
                description,
                descriptionKey,
                icon,
                category,
                categoryKey,
                subcategory,
                subcategoryKey
            )
            addMetadata("options", options.map { stringTransformer(it) })
            addMetadata("optionsKey", optionKeys)
        }
        return CachedTransformedProvider(property, options::get, options::indexOf)
    }

    protected fun <Type : Any> radiobutton(
        name: String,
        defaultOption: Type,
        options: Array<Type>,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        optionKeys: Array<String> = arrayOf(),
        stringTransformer: (Type) -> String = { it.toString() },
    ): PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>> {
        val selectedIndex = options.indexOf(defaultOption)

        val property = Provider(
            selectedIndex,
            name,
            description,
            category,
            subcategory,
            Int::class.java,
            Visualizer.RadioVisualizer()
        ) {
            addMetadata(
                name,
                nameKey,
                description,
                descriptionKey,
                icon,
                category,
                categoryKey,
                subcategory,
                subcategoryKey
            )
            addMetadata("options", options.map { stringTransformer(it) })
            addMetadata("optionsKey", optionKeys)
        }
        return CachedTransformedProvider(property, options::get, options::indexOf)
    }

    /**
     * A button which runs [action] when clicked
     *
     * The delegated property holds no value, use as `val openWiki by button("Open Wiki") { ... }`
     */
    @JvmSynthetic
    protected fun button(
        name: String,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        text: String? = null,
        textKey: String? = null,
        action: () -> Unit,
    ) = DummyProvider(name, description, category, subcategory, Visualizer.ButtonVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        this.addMetadata("runnable", Runnable {
            runCatching(action).onFailure { ConfigManager.LOGGER.error("Failed to run button {}", name, it) }
        })
        this.addMetadata("text", text)
        this.addMetadata("textKey", textKey)
    }

    /**
     * A read-only info card, [type] is one of `info`, `success`, `warning` or `error`
     *
     * When [name] is null the first line of [description] is used as the heading
     */
    @JvmSynthetic
    protected fun info(
        name: String? = null,
        description: String? = null,
        type: String = "info",
        nameKey: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
    ) = DummyProvider(name, description, category, subcategory, Visualizer.InfoVisualizer()) {
        addMetadata("titleKey", nameKey)
        addMetadata("descriptionKey", descriptionKey)
        addMetadata("icon", icon)
        addMetadata("categoryKey", categoryKey)
        addMetadata("subcategoryKey", subcategoryKey)
        addMetadata("type", type)
    }

    /** A dropdown where any number of [options] can be selected, the value holds one flag per option */
    @JvmSynthetic
    protected fun multiSelectDropdown(
        name: String,
        options: Array<String>,
        def: BooleanArray = BooleanArray(options.size),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        optionKeys: Array<String> = arrayOf(),
    ) = Provider(def, name, description, category, subcategory, BooleanArray::class.java, Visualizer.MultiSelectDropdownVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        this.addMetadata("options", options)
        this.addMetadata("optionsKey", optionKeys)
        this.addMetadata("checkable", true)
    }

    /** A selectable-list dropdown holding the selected index of [options] or -1 for none */
    @JvmSynthetic
    protected fun singleSelectDropdown(
        name: String,
        options: Array<String>,
        def: Int = -1,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        optionKeys: Array<String> = arrayOf(),
    ) = Provider(def, name, description, category, subcategory, Int::class.java, Visualizer.MultiSelectDropdownVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        this.addMetadata("options", options)
        this.addMetadata("optionsKey", optionKeys)
        this.addMetadata("checkable", false)
    }

    /** A reorderable list, when [checkable] the value is the enabled subset of [options] in order */
    @JvmSynthetic
    protected fun draggableList(
        name: String,
        def: Array<String>,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        options: Array<String> = arrayOf(),
        optionKeys: Array<String> = arrayOf(),
        checkable: Boolean = false,
    ) = Provider(def, name, description, category, subcategory, Array<String>::class.java, Visualizer.DraggableListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        this.addMetadata("options", options)
        this.addMetadata("optionsKey", optionKeys)
        this.addMetadata("checkable", checkable)
    }

    /** A two-handle slider holding `[start, end]` */
    @JvmSynthetic
    protected fun rangeSlider(
        name: String,
        def: FloatArray,
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        min: Float = 0f,
        max: Float = 100f,
        step: Float = 1f,
    ) = Provider(def, name, description, category, subcategory, FloatArray::class.java, Visualizer.RangeSliderVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        require(def.size == 2) { "rangeSlider '$name' needs a start and end, got ${def.size} values" }
        requireStep(name, min, max, step)
        this.addMetadata("min", min)
        this.addMetadata("max", max)
        this.addMetadata("step", step)
    }

    /** A file or folder picker holding the chosen path, [types] are extensions such as `.png` */
    @JvmSynthetic
    protected fun file(
        name: String,
        def: String = "",
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        types: Array<String> = arrayOf(),
        filterName: String? = null,
        directory: Boolean = false,
        placeholder: String? = null,
        placeholderKey: String? = "oneconfig.filepicker.placeholder",
    ) = Provider(def, name, description, category, subcategory, String::class.java, Visualizer.FileVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        this.addMetadata("types", types)
        this.addMetadata("filterName", filterName)
        this.addMetadata("directory", directory)
        this.addMetadata("placeholder", placeholder)
        this.addMetadata("placeholderKey", placeholderKey)
    }

    @JvmSynthetic
    protected fun textList(
        name: String,
        def: Array<String> = arrayOf(),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        maxEntries: Int = 0,
        reorderable: Boolean = true,
        addText: String? = null,
        addTextKey: String? = null,
        placeholder: String? = null,
        placeholderKey: String? = "oneconfig.textinput.placeholder",
        regex: String? = null,
    ) = Provider(def, name, description, category, subcategory, Array<String>::class.java, Visualizer.TextListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
            addListMetadata(maxEntries, reorderable, addText, addTextKey)
        this.addMetadata("placeholder", placeholder)
        this.addMetadata("placeholderKey", placeholderKey)
        this.addMetadata("regex", regex)
    }

    @JvmSynthetic
    protected fun numberList(
        name: String,
        def: FloatArray = FloatArray(0),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        maxEntries: Int = 0,
        reorderable: Boolean = true,
        addText: String? = null,
        addTextKey: String? = null,
        min: Float = 0f,
        max: Float = 100f,
        step: Float = 0f,
    ) = Provider(def, name, description, category, subcategory, FloatArray::class.java, Visualizer.NumberListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
            addListMetadata(maxEntries, reorderable, addText, addTextKey)
        this.addMetadata("min", min)
        this.addMetadata("max", max)
        this.addMetadata("step", step)
    }

    @JvmSynthetic
    protected fun sliderList(
        name: String,
        def: FloatArray = FloatArray(0),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        maxEntries: Int = 0,
        reorderable: Boolean = true,
        addText: String? = null,
        addTextKey: String? = null,
        min: Float = 0f,
        max: Float = 100f,
        step: Float = 1f,
    ) = Provider(def, name, description, category, subcategory, FloatArray::class.java, Visualizer.SliderListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
            addListMetadata(maxEntries, reorderable, addText, addTextKey)
        requireStep(name, min, max, step)
        this.addMetadata("min", min)
        this.addMetadata("max", max)
        this.addMetadata("step", step)
    }

    /** A list of ARGB colours */
    @JvmSynthetic
    protected fun colorList(
        name: String,
        def: IntArray = IntArray(0),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        maxEntries: Int = 0,
        reorderable: Boolean = true,
        addText: String? = null,
        addTextKey: String? = null,
        alpha: Boolean = true,
    ) = Provider(def, name, description, category, subcategory, IntArray::class.java, Visualizer.ColorListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
            addListMetadata(maxEntries, reorderable, addText, addTextKey)
        if (!alpha) this.addMetadata("noAlpha", Unit)
    }

    @JvmSynthetic
    protected fun fileList(
        name: String,
        def: Array<String> = arrayOf(),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        maxEntries: Int = 0,
        reorderable: Boolean = true,
        addText: String? = null,
        addTextKey: String? = null,
        types: Array<String> = arrayOf(),
        filterName: String? = null,
        directory: Boolean = false,
        placeholder: String? = null,
        placeholderKey: String? = "oneconfig.filepicker.placeholder",
    ) = Provider(def, name, description, category, subcategory, Array<String>::class.java, Visualizer.FileListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
            addListMetadata(maxEntries, reorderable, addText, addTextKey)
        this.addMetadata("types", types)
        this.addMetadata("filterName", filterName)
        this.addMetadata("directory", directory)
        this.addMetadata("placeholder", placeholder)
        this.addMetadata("placeholderKey", placeholderKey)
    }

    /** Namespaced item IDs such as `minecraft:diamond`, a [maxEntries] of 1 makes a single selector */
    @JvmSynthetic
    protected fun itemList(
        name: String,
        def: Array<String> = arrayOf(),
        nameKey: String? = null,
        description: String? = null,
        descriptionKey: String? = null,
        icon: String? = null,
        category: String? = "General",
        categoryKey: String? = null,
        subcategory: String? = "General",
        subcategoryKey: String? = null,
        maxEntries: Int = 0,
        reorderable: Boolean = true,
        addText: String? = null,
        addTextKey: String? = "oneconfig.itemlist.add",
    ) = Provider(def, name, description, category, subcategory, Array<String>::class.java, Visualizer.ItemListVisualizer()) {
        addMetadata(name, nameKey, description, descriptionKey, icon, category, categoryKey, subcategory, subcategoryKey)
        require(maxEntries >= 0) { "itemList '$name' has a negative maxEntries ($maxEntries)" }
            addListMetadata(maxEntries, reorderable, addText, addTextKey)
    }

    private fun Node.addListMetadata(maxEntries: Int, reorderable: Boolean, addText: String?, addTextKey: String?) {
        this.addMetadata("maxEntries", maxEntries)
        this.addMetadata("reorderable", reorderable)
        this.addMetadata("addText", addText)
        this.addMetadata("addTextKey", addTextKey)
    }

    private fun requireStep(name: String, min: Float, max: Float, step: Float) =
        require(step <= 0f || step <= max - min) { "slider '$name' has step ($step) larger than its range ($min to $max)" }

    fun hideIf(option: KProperty<*>, condition: () -> Boolean) {
        if (tree == null) initialize(false)
        hideIf(option.name) { condition() }
    }

    fun hideIf(option: KProperty<*>, condition: KProperty0<Boolean>) {
        if (tree == null) initialize(false)
        hideIf(option.name) { condition.get() }
    }

    fun <T> addCallback(option: KProperty<T>, callback: (T?) -> Boolean) {
        option.property.addCallback { callback(when (option) {
            is KProperty0<T> -> option.get()
            is KProperty1<*, T> -> (option as KProperty1<Any, T>).get(this)
            else -> option.call(this)
        }) }
    }

    /** provider for the [PropertyDelegate] which must be a class to avoid passing the reference directly */
    protected class Provider<T : Any>(
        private val def: T?,
        private val name: String?,
        private val description: String?,
        private val category: String?,
        private val subcategory: String?,
        private val type: Class<T>,
        private val visualizer: Visualizer,
        private val extra: (Property<T>.() -> Unit)? = null
    ) : PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, T>> {
        override operator fun provideDelegate(
            thisRef: KtConfig,
            property: KProperty<*>
        ): ReadWriteProperty<KtConfig, T> {
            val p = Properties.simple(property.name, name ?: property.name, description, def, type)
            extra?.invoke(p)
            p.addMetadata("visualizer", visualizer)
            p.addMetadata("category", category)
            p.addMetadata("subcategory", subcategory)
            (thisRef.tree ?: thisRef.pendingTree()).put(p)
            return PropertyDelegate(p)
        }
    }

    /** provider for value-less options such as buttons and info cards, backed by [Properties.dummy] */
    protected class DummyProvider(
        private val name: String?,
        private val description: String?,
        private val category: String?,
        private val subcategory: String?,
        private val visualizer: Visualizer,
        private val extra: Property<Void>.() -> Unit
    ) : PropertyDelegateProvider<KtConfig, ReadOnlyProperty<KtConfig, Unit>> {
        override operator fun provideDelegate(thisRef: KtConfig, property: KProperty<*>): ReadOnlyProperty<KtConfig, Unit> {
            val p = Properties.dummy(property.name, name, description)
            p.extra()
            p.addMetadata("visualizer", visualizer)
            p.addMetadata("category", category)
            p.addMetadata("subcategory", subcategory)
            (thisRef.tree ?: thisRef.pendingTree()).put(p)
            return ReadOnlyProperty { _, _ -> }
        }
    }

    fun <Type : Any> observable(
        property: PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>>,
        callback: (Type) -> Unit
    ): PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>> = callback(property) {
        callback(it)
        false
    }

    fun <Type : Any> callback(
        property: PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>>,
        callback: (Type) -> Boolean
    ): PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>> =
        ObservableProvider(property, callback)

    fun <Source : Any, Target : Any> transformed(
        property: PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Source>>,
        to: (Source) -> Target,
        from: (Target) -> Source,
    ) : PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Target>> = CachedTransformedProvider(property, to, from)

    @JvmName("withObservable")
    fun <Type : Any> PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>>.onChange(callback: (Type) -> Unit) = observable(this, callback)
    fun <Type : Any> PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>>.withCallback(callback: (Type) -> Boolean) = callback(this, callback)

    private data class CachedTransformedProvider<Source : Any, Target : Any>(
        val delegate: PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Source>>,
        val to: (Source) -> Target,
        val from: (Target) -> Source
    ) : PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Target>> {
        data class TransformedPropertyDelegate<Source : Any, Target : Any>(
            val thisRef: KtConfig,
            val property: KProperty<*>,
            val delegate: ReadWriteProperty<KtConfig, Source>,
            val to: (Source) -> Target,
            val from: (Target) -> Source
        ) : ReadWriteProperty<KtConfig, Target> {
            var sourceValue: Source = delegate.getValue(thisRef, property)
            var targetValue: Target = to(sourceValue)

            fun updateValues() {
                val currentSourceValue = delegate.getValue(thisRef, property)
                if (sourceValue != currentSourceValue) {
                    sourceValue = currentSourceValue
                    targetValue = to(sourceValue)
                }
            }

            override fun getValue(thisRef: KtConfig, property: KProperty<*>): Target {
                updateValues()
                return targetValue
            }

            override fun setValue(
                thisRef: KtConfig,
                property: KProperty<*>,
                value: Target
            ) {
                this.targetValue = value
                this.sourceValue = from(value)
            }
        }

        override fun provideDelegate(thisRef: KtConfig, property: KProperty<*>): ReadWriteProperty<KtConfig, Target> =
            TransformedPropertyDelegate(thisRef, property, delegate.provideDelegate(thisRef, property), to, from)
    }

    private data class ObservableProvider<Type : Any>(
        val delegate: PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>>,
        val callback: (Type) -> Boolean
    ) : PropertyDelegateProvider<KtConfig, ReadWriteProperty<KtConfig, Type>> {
        override fun provideDelegate(
            thisRef: KtConfig,
            property: KProperty<*>
        ): ReadWriteProperty<KtConfig, Type> {
            val delegate = delegate.provideDelegate(thisRef, property)

            thisRef.addCallback(property) { _ ->
                callback(delegate.getValue(thisRef, property))
            }

            return delegate
        }

    }

    private class PropertyDelegate<T : Any>(val property: Property<T>) : ReadWriteProperty<KtConfig, T> {
        override operator fun getValue(thisRef: KtConfig, property: KProperty<*>): T = this.property.get() as T

        override operator fun setValue(thisRef: KtConfig, property: KProperty<*>, value: T) {
            this.property.set(value)
        }
    }
}