package org.polyfrost.oneconfig.api.config.v1.dsl

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer

@Suppress("UNCHECKED_CAST")
public var Property<*>.visualizer: Class<out Visualizer>?
    get() = metadata?.get("visualizer") as? Class<out Visualizer>
    set(value) = addMetadata("visualizer", value)

public var Node.category: String?
    get() = metadata?.get("category") as? String
    set(value) = addMetadata("category", value)

public var Node.subcategory: String?
    get() = metadata?.get("subcategory") as? String
    set(value) = addMetadata("subcategory", value)

public var Node.index: Int?
    get() = metadata?.get("index") as? Int
    set(value) = addMetadata("index", value)

public var Node.icon: String?
    get() = metadata?.get("icon") as? String // path to the image
    set(value) = addMetadata("icon", value)

public var Tree.saveFunction: Runnable?
    get() = metadata?.get("custom_save") as? Runnable
    set(value) = addMetadata("custom_save", value)

public var Tree.noCache: Boolean
    get() = metadata?.getOrDefault("no_cache", false) as Boolean
    set(value) = addMetadata("no_cache", value)

/** when true this tree is shown in the UI but is not loaded from or saved to disk */
public var Tree.uiOnly: Boolean
    get() = metadata?.get("ui_only") == true
    set(value) {
        if (value) addMetadata("ui_only", true) else removeMetadata("ui_only")
    }