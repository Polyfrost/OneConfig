package org.polyfrost.oneconfig.utils.v1.dsl

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
internal var Property<*>.visualizer: Class<out Visualizer>?
    get() = metadata?.get("visualizer") as? Class<out Visualizer>
    set(value) = addMetadata("visualizer", value)

internal var Property<*>.visualizerKt: KClass<out Visualizer>?
    get() = null
    set(value) { visualizer = value?.java }

internal var Node.category: String?
    get() = metadata?.get("category") as? String
    set(value) = addMetadata("category", value)

internal var Node.subcategory: String?
    get() = metadata?.get("subcategory") as? String
    set(value) = addMetadata("subcategory", value)

internal var Node.index: Int?
    get() = metadata?.get("index") as? Int
    set(value) = addMetadata("index", value)

internal var Node.icon: String?
    get() = metadata?.get("icon") as? String
    set(value) = addMetadata("icon", value)

internal var Tree.saveFunction: Runnable?
    get() = metadata?.get("custom_save") as? Runnable
    set(value) = addMetadata("custom_save", value)