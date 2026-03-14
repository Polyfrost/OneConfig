package org.polyfrost.oneconfig.api.config.v1.dsl

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer

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

var Node.icon: String?
    get() = metadata?.get("icon") as? String // path to the image
    set(value) = addMetadata("icon", value)

var Tree.saveFunction: Runnable?
    get() = metadata?.get("custom_save") as? Runnable
    set(value) = addMetadata("custom_save", value)

var Tree.noCache: Boolean
    get() = metadata?.getOrDefault("no_cache", false) as Boolean
    set(value) = addMetadata("no_cache", value)