package org.polyfrost.oneconfig.api.hud.internal

import org.polyfrost.oneconfig.api.config.Node
import org.polyfrost.oneconfig.api.config.Property
import org.polyfrost.oneconfig.api.config.Property.prop
import org.polyfrost.oneconfig.api.config.Tree
import org.polyfrost.oneconfig.api.hud.Hud

fun Hud<*>.writeOut(): Tree {
    val tree = Tree.tree()
    val hud = hud
    tree["x"] = prop(hud.x)
    tree["y"] = prop(hud.y)
    tree["scaleX"] = prop(hud.scaleX)
    tree["scaleY"] = prop(hud.scaleY)
    tree["hidden"] = prop(hidden)
    tree["skewX"] = prop(hud.skewX)
    tree["skewY"] = prop(hud.skewY)
    tree["rotation"] = prop(hud.rotation)
    tree["opacity"] = prop(hud.alpha)
    return tree
}

fun Hud<*>.readIn(tree: Tree) {
    val hud = hud
    hud.x = tree["x"].get(0f)
    hud.y = tree["y"].get(0f)
    hud.scaleX = tree["scaleX"].get(1f)
    hud.scaleY = tree["scaleY"].get(1f)
    hidden = tree["hidden"].get(false)
    hud.skewX = tree["skewX"].get(0.0)
    hud.skewY = tree["skewY"].get(0.0)
    hud.rotation = tree["rotation"].get(0.0)
    hud.alpha = tree["opacity"].get(1f)
}

fun <T> Node?.get(def: T) = ((this as? Property<*>)?.get() as? T?) ?: def