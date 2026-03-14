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

import androidx.compose.runtime.Composable
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties.ktProperty
import org.polyfrost.oneconfig.api.config.v1.Properties.simple
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.HudManager.LOGGER

@Suppress("EqualsOrHashCode", "UnstableApiUsage")
abstract class Hud(id: String, title: String, val category: Category) : Cloneable, Config(id, null, title, null) {

    @Switch(title = "Static Width")
    var staticWidth = false

    @Switch(title = "Show in F3")
    var showInF3 = true

    @Switch(title = "Show in Tab")
    var showInTab = true

    @Switch(title = "Show in GUIs")
    var showInScreens = true

    var toggleKey: Int = -1
    var showKey: Int = -1

    var x: Float = 0f
    var y: Float = 0f
    var hidden: Boolean = false

    override fun addToInitQueue() {}

    val isReal get() = tree != null

    @Transient
    internal var _runtime: PolyComposeRuntime? = null

    val runtime: PolyComposeRuntime
        get() = _runtime ?: PolyComposeRuntime().also {
            _runtime = it
            it.setContent { Content() }
        }

    @Composable
    abstract fun Content()

    @ApiStatus.Internal
    fun make(with: Tree? = null): Hud {
        if (tree != null) throw IllegalArgumentException("HUD is already made, it cannot be made again")
        val out = if (multipleInstancesAllowed()) clone() else this
        val treeId = with?.id ?: "huds/${(1000..9999).random()}-$id"
        val tree = ConfigManager.collect(out, treeId)
        out.apply {
            tree.title = title
            tree.addMetadata("category", category)
            tree.addMetadata("hidden", true)
            tree["x"] = ktProperty(out::x)
            tree["y"] = ktProperty(out::y)
            tree["toggleKey"] = ktProperty(out::toggleKey)
            tree["showKey"] = ktProperty(out::showKey)
            addToSerialized(tree)
            tree["hudClass"] = simple(value = out::class.java.name)
            addCallbacks(tree)
            if (with == null) LOGGER.info("generated new HUD config for $title -> ${tree.id}")
            ConfigManager.active().register(tree)
            if (showKey != -1) hidden = true
            this.tree = tree
        }
        return out
    }

    protected open fun addCallbacks(tree: Tree) {}
    protected open fun addToSerialized(tree: Tree) {}

    protected fun updateWhenChanged(optionName: String) {
        if (isReal) addCallback(optionName, this::updateAndRecalculate)
        else LOGGER.warn("attempted to add callback to {}'s option '{}', but it is not real.", title, optionName)
    }

    protected fun updateAndRecalculate() {
        update()
    }

    open fun setup() {}
    abstract fun update(): Boolean
    open fun updateFrequency(): Long = -1L
    open fun defaultPosition(): Pair<Float, Float> = 0f to 0f
    open fun hasBackground(): Boolean = true
    open fun backgroundColor(): PolyColor? = null
    open fun multipleInstancesAllowed(): Boolean = true
    open fun minimumSize(): Pair<Float, Float> = 0f to 0f
    open fun remove() {}

    @MustBeInvokedByOverriders
    @Suppress("UNCHECKED_CAST")
    override fun clone(): Hud = (super.clone() as Hud).apply {
        _runtime = null
        showKey = -1
        toggleKey = -1
    }

    class Category(val name: String, val id: Byte) {
        companion object {
            @JvmStatic val COMBAT = Category("oneconfig.combat", 1)
            @JvmStatic val INFO = Category("oneconfig.info", 2)
            @JvmStatic val PLAYER = Category("oneconfig.player", 3)
        }

        override fun toString() = name
        override fun hashCode() = id.toInt()
    }
}
