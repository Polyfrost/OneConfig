/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.hud

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.Property
import org.polyfrost.oneconfig.api.config.Tree
import org.polyfrost.oneconfig.api.hud.HudManager.LOGGER
import org.polyfrost.oneconfig.api.hud.elements.InferringCComponent
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.utils.fastEach
import java.lang.invoke.MethodHandle

abstract class Hud() {
    @ApiStatus.Internal
    lateinit var self: InferringCComponent

    @ApiStatus.Internal
    @Transient
    lateinit var tree: Tree

    @ApiStatus.Internal
    @Transient
    protected val customs = ArrayList<MethodHandle>(0)

    @ApiStatus.Internal
    protected val groupIndex = -1

    @ApiStatus.Internal
    protected val ver = -1

    /**
     * Initialize the HUD.
     *
     * Calling this method yourself will crash.
     */
    @ApiStatus.Internal
    fun init(tree: Tree, polyUI: PolyUI) {
        if (::tree.isInitialized) throw IllegalStateException("Hud already initialised!")
        this.tree = tree
        tree.map.forEach { (_, it) ->
            val mh = it.getMetadata<MethodHandle>("render")
            if (mh != null) {
                require(getDefaultSize() != null) { "Hud with @CustomComponent must specify a default size" }
                customs.add(mh)
                return@forEach
            }
        }
        // we were de-serialized
        if (::self.isInitialized) {
            if (ver != getVersion()) {
                if (getVersion() < 0) throw IllegalStateException("Hud ${this::class.simpleName} has invalid version number ${getVersion()} (cannot be negative)")
                LOGGER.warn("Update detected for HUD $this, re-initialising...")
                tree.map.forEach { (_, it) ->
                    if (it !is Property<*>) throw IllegalArgumentException("HUDs cannot have children!")
                    self.children.clear()
                    if (it.getMetadata<String>("isHud") != null) {
                        self.children.add(it.getAs())
                    }
                }
            }
        } else {
            val components = ArrayList<Component>()
            tree.map.forEach { (_, it) ->
                if (it !is Property<*>) throw IllegalArgumentException("HUDs cannot have children!")
                if (it.getMetadata<String>("isHud") != null) {
                    val out = it.getAs<Component>()
                    components.add(out)
                    if (out !is Block && out !is Text && out !is Image) {
                        LOGGER.warn("Hud ${tree.id} is using non-standard components. For maximum compatibility with the HUD designer, please only use Block, Image and Text components.")
                    }
                }
            }
            self = InferringCComponent(
                at = getDefaultPosition(),
                size = getDefaultSize(),
                children = components.toTypedArray(),
            )
        }
        @Suppress("KotlinConstantConditions")
        if (groupIndex == -1) {
            polyUI.master.add(self)
        } else {
            polyUI.master.children[groupIndex].add(self)
        }
        val freq = getUpdateFrequency()
        if (freq == -1L) return
        polyUI.every(freq) {
            update()
        }
    }

    /**
     * Render the custom components of this HUD.
     *
     * **do not** call this method yourself!
     */
    @ApiStatus.Internal
    fun renderCustoms(/* matrixStack: UMatrixStack? */) {
        customs.fastEach {
            val self = this.self
            it.invokeExact(0, self.x, self.y, self.width, self.height, self.scaleX, self.scaleY, self.rotation)
        }
        customRender(/* matrixStack, */ self.x, self.y, self.width, self.height, self.scaleX, self.scaleY, self.rotation)
    }

    /**
     * Custom render script.
     *
     * Note that it is called **after** rendering of [traditional][org.polyfrost.oneconfig.api.hud.annotations.HudComponent] components.
     *
     * Provided as an alternative to [custom components][org.polyfrost.oneconfig.api.hud.annotations.CustomComponent] with the annotation.
     */
    open fun customRender(/* matrixStack: UMatrixStack?, */ x: Float, y: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Double) {
        // no-op
    }

    /**
     * Return the desired update frequency for this HUD. This is the frequency at which [update] is called.
     *
     * This method is called once, and only once, at initialization.
     *
     * The update frequency should be as high as possible, as to not run unnecessary code.
     *
     * @return the value, in nanoseconds, for the update frequency. Return -1 to disable updates.
     * @see update
     */
    open fun getUpdateFrequency(): Long = -1L

    /**
     * Update the HUD.
     *
     * This method is called every [getUpdateFrequency] nanoseconds.
     * @see getUpdateFrequency
     */
    open fun update() {}

    /**
     * Return the default position for this HUD, if it is not specified in the config.
     */
    open fun getDefaultPosition(): Vec2<Unit> = origin

    /**
     * Return the default size for this HUD, if it is not specified in the config.
     *
     * You can return null to enable auto-inference of the size **if you have no [custom components][org.polyfrost.oneconfig.api.hud.annotations.CustomComponent] in this hud.**
     *
     * This method may also fail if you specified components cannot be auto-inferred. If so, specify a default size.
     */
    open fun getDefaultSize(): Vec2<Unit>? = null

    /**
     * Return the version number (must be non-negative) for this HUD.
     *
     * This method is called once, and only once, at initialization.
     *
     * You should update this every time you change the contents of your HUD. This is used to update the HUD with the new elements upon an update.
     *
     * A good idea is to start with 0, and increment it every time you change the contents of your HUD.
     */
    abstract fun getVersion(): Int
}
