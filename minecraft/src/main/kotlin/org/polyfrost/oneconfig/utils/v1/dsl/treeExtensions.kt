package org.polyfrost.oneconfig.utils.v1.dsl

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.polyui.data.PolyImage

@Suppress("UNCHECKED_CAST")
var Property<*>.visualizer: Class<out Visualizer>?
    get() = metadata?.get("visualizer") as? Class<out Visualizer>
    set(value) = addMetadata("visualizer", value)

var Node.category: String?
    get() = metadata?.get("category") as? String
    set(value) = addMetadata("category", value)

var Node.subcategory: String?
    get() = metadata?.get("subcategory") as? String
    set(value) = addMetadata("subcategory", value)

var Node.index: Int?
    get() = metadata?.get("index") as? Int
    set(value) = addMetadata("index", value)

var Node.icon: PolyImage?
    get() = metadata?.get("icon") as? PolyImage
    set(value) = addMetadata("icon", value)

var Tree.saveFunction: Runnable?
    get() = metadata?.get("custom_save") as? Runnable
    set(value) = addMetadata("custom_save", value)

var Tree.noCache: Boolean?
    get() = metadata?.get("no_cache") as? Boolean
    set(value) = addMetadata("no_cache", value)