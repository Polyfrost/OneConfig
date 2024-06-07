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

package org.polyfrost.oneconfig.api.hud.v1

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties.ktProperty
import org.polyfrost.oneconfig.api.config.v1.Properties.simple
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.fastAll
import org.polyfrost.polyui.utils.fastEachIndexed
import kotlin.io.path.exists
import kotlin.random.Random

/**
 * HUD (Heads Up Display) is a component that is rendered on top of the screen. They are used for displaying information to the user, such as the time, or the player's health.
 *
 * - You need to register your HUD with [HudManager.register] in order for it to be available to the user.
 * - **Your HUD's size and positioning, if you are manually specifying it, needs to be designed for a `1920x1080` screen**.
 * - The instance you pass to [HudManager.register] is the instance that is used for the HUD picker screen. When a HUD is added to the screen, a new instance is created using [clone].
 * - HUD config files are stored in `{profile}/huds/{rnd}-`[id], e.g. `huds/42-my_hud.toml`.
 * - For a hud instance, the following methods are called (in order) [create], [initialize], and then [periodically][updateFrequency] [update] if required.
 * - Try not to do really long operations in [update]. The method is called on the render thread and so may cause lag. If you need to do a long operation, consider using an asynchronous task.
 * - The parent of your HUD is a [Block] which is controlled by the user. **You do not need to include a background in your HUD.**
 * - HUDs which are wider than 450px may have issues when displayed on the HUD picker screen, and HUDs this large are not recommended anyway as they may be very distracting.
 *
 * In this system, multiple of the same HUD can exist at once. In order for this to work correctly, there are a few rules:
 * - **Do not run any code inside an `init {}` block**. [clone] does not run constructors.
 * - For fields that are mutable references to other objects, you must set them yourself in [clone] in order for each HUD to be independent of each other.
 * - The easiest way to ensure that your HUD works when placed multiple times is to just not use `static` fields, so you don't abuse them.
 */
@Suppress("EqualsOrHashCode")
abstract class Hud<T : Drawable> : Cloneable, Config("null", null, "null", null) {
    // effectively lateinit because of how this works (great reason ik)
    // tree is null unless this is saved
    final override fun makeTree(id: String) = null

    /**
     * user facing title of this HUD. can be localized using translation key/values like in PolyUI.
     */
    abstract fun title(): String

    /**
     * return the ID base used for this HUD when creating instances. It should follow the same scheme as the id for a [Config] or [Tree].
     *
     * HUD config files are stored in `{profile}/huds/{rnd}-`[id], e.g. `huds/42-my_hud.toml`.
     * the random number is omitted for the first instance.
     */
    abstract fun id(): String

    abstract fun category(): Category

    /**
     * @return `true` if this property is a real HUD on the screen, and not the example instance.
     */
    val isReal get() = tree != null

    /**
     * clone, build the HUD drawable, build the tree for it, add any [custom information][addToSerialized],
     * write in data from [with], register the tree, and return the cloned hud instance.
     *
     * **note:** while this method is public, it is entirely internal API and invoking it yourself is probably a bad idea, right?
     */
    @ApiStatus.Internal
    fun make(with: Tree? = null): Hud<T> {
        if (tree != null) throw IllegalArgumentException("HUD already exists -> can only clone from root HUD object")
        val out = clone()
        val id = with?.id ?: genRid()
        val tree = ConfigManager.collect(out, id)
        tree.title = out.title()
        tree.addMetadata("category", out.category())
        tree.addMetadata("frontendIgnore", true)
        tree["x"] = ktProperty(out.hud::x)
        tree["y"] = ktProperty(out.hud::y)
        tree["hidden"] = ktProperty(out::hidden)
        inspect(out.hud, tree)
        out.addToSerialized(tree)
        tree["alpha"] = ktProperty(out.hud::alpha)
        tree["scaleX"] = ktProperty(out.hud::scaleX)
        tree["scaleY"] = ktProperty(out.hud::scaleY)
        tree["rotation"] = ktProperty(out.hud::rotation)
        tree["skewX"] = ktProperty(out.hud::skewX)
        tree["skewY"] = ktProperty(out.hud::skewY)
        tree["hudClass"] = simple(value = out::class.java.name)
        if (with != null) tree.overwrite(with)
        else HudManager.LOGGER.info("generated new HUD config for ${out.title()} -> ${tree.id}")
        out.tree = tree
        ConfigManager.active().register(tree)
        return out
    }

    private fun inspect(drawable: Drawable, tree: Tree) {
        tree["color"] = ktProperty(drawable::color)
        when(drawable) {
            is Block -> {
                tree["radii"] = ktProperty(drawable::radii)
                tree["boarderColor"] = ktProperty(drawable::boarderColor)
                tree["boarderWidth"] = ktProperty(drawable::boarderWidth)
            }
            is Text -> {
                tree["font"] = ktProperty(drawable::_font)
                tree["fontSize"] = ktProperty(drawable::uFontSize)
            }
        }
        drawable.children?.fastEachIndexed { i, it ->
            val child = Tree.tree("$i")
            inspect(it, child)
            tree.put(child)
        }
    }

    private fun genRid(): String {
        val folder = ConfigManager.active().folder
        val init = "huds/${id()}"
        if (folder.resolve(init).exists()) return init
        var p = "huds/${Random.Default.nextInt(0, 100)}-${id()}"
        var i = 0
        while (folder.resolve(p).exists()) {
            p = "huds/${Random.Default.nextInt(0, 999)}-${id()}"
            when (i++) {
                100 -> HudManager.LOGGER.warn("they all say that it gets better")
                500 -> HudManager.LOGGER.warn("yeah they all say that it gets better;; it gets better the more you grow")
                999 -> throw IllegalStateException("... but what if i dont?")
            }
        }
        return p
    }

    /**
     * Override this method to specify completely custom options that should be added to the serialized representation of this HUD.
     */
    protected open fun addToSerialized(tree: Tree) {}

    @Transient
    private var it: T? = null

    /**
     * Return the instance of your HUD element, made by calling [create].
     * @see get
     */
    inline val hud: T
        get() = get()

    /**
     * Hidden flag for this HUD.
     *
     * If `true`, the HUD will not be rendered, and, if this is in a HUD group, it will resize accordingly.
     */
    var hidden: Boolean
        get() = it?.enabled == false
        set(new) {
            val value = !new
            // useless null-safety checks, but I don't want to risk dumb errors
            val it = it ?: return
            val siblings = it.parent.children ?: return
            if (value == it.enabled) return

            it.enabled = value
            if (siblings.size == 1) {
                it.parent.enabled = value
            } else if (!value && siblings.fastAll { !it.enabled }) {
                it.parent.enabled = false
            }
            it.parent.recalculate()
        }

    /**
     * Return the instance of your HUD element, made by calling [create].
     * @see hud
     */
    fun get() = it ?: create().also { this.it = it }

    /**
     * Create a new instance of your HUD. This should be the complete unit of your hud, **excluding** a background.
     */
    protected abstract fun create(): T

    /**
     * initialize your HUD element.
     *
     * this method will be called once, and once only.
     */
    open fun initialize() {}

    /**
     * Update your HUD element.
     * Get the instance of your HUD element by calling [get] or [hud].
     * @see updateFrequency
     * @return if you have performed an operation that has changed the size of your HUD element, return `true`
     *         so the system will automatically resize the drawable.
     */
    abstract fun update(): Boolean

    /**
     * Return, in *nanoseconds*, how often the [update] method should be called.
     * Note that small values may be slightly inaccurate. See [PolyUI.every][org.polyfrost.polyui.PolyUI.every] documentation for more information.
     *
     * PolyUI bundles time units for you, such as `0.8.`[seconds] or `50.milliseconds`.
     *
     * Any negative number means [update] will never be called. A value of `0` means that [update] will be called every frame. This is not recommended.
     *
     * This method is called once when the HUD is added to the screen.
     * @see seconds
     */
    abstract fun updateFrequency(): Long

    /**
     * specify a position for this HUD to be placed at.
     *
     * **this position, as will all HUD methods, should be for a position on a `1920x1080` screen.**
     */
    open fun defaultPosition(): Vec2? = null

    /**
     * Set a custom default background color for this HUD.
     *
     * A value of `null` (default) means that the standard component background color will be used.
     *
     * To remove the background, use [PolyColor.TRANSPARENT].
     */
    open fun backgroundColor(): PolyColor? = null

    /**
     * This method will create a new instance of the HUD. It is key to the functionality of the HUD system.
     *
     * When a HUD is registered, the instance that was passed is the instance that is used for the HUD picker screen.
     *
     * Whenever a HUD is added to the screen, this method is called to create a new instance of the HUD, therefore making them
     * independent of each other.
     *
     * This method is not final as you may need to set some field values manually. This is because the default JVM implementation of
     * [java.lang.Object.clone] performs a **shallow copy** of the object, meaning that any fields that are references to other objects
     * will be copied to the new instance, but the references will be the same. Primitives or immutable values are therefore safe.
     * **Constructors are also not called**. DON'T perform any logic in the constructor of your HUD.
     *
     * Calling this method yourself is not a good idea.
     */
    @MustBeInvokedByOverriders
    @Suppress("unchecked_cast")
    override fun clone(): Hud<T> = (super.clone() as Hud<T>).apply { it = null }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Drawable) {
            return other == get()
        }
        return false
    }

    enum class Category {
        COMBAT,
        INFO,
        PLAYER
    }
}
