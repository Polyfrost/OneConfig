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

package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.image


object Notifications {
    private const val MAX = 5
    private const val PADDING = 8f

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    private inline val polyUI get() = UIManager.INSTANCE.defaultInstance
    private val current = arrayOfNulls<Component>(MAX)
    private val queue = ArrayList<Component>(5)
    private var lastY = 0f
    private var i = 0

    fun enqueueCustom(vararg components: Component, progressFunc: Animation) {
        val out = Block(children = components, at = Vec2(polyUI.size.x + PADDING, 0f)).withBoarder().withStates()
        var failedToFinishNormally = false
        out.afterInit {
            val HEIGHT = (height / 12f).coerceAtMost(8f)
            val radii = out.radii
            val rad = if (radii == null) 0f else if (radii.size > 3) radii[2] else radii[0]
            addChild(
                Block(
                    at = Vec2(x, y + height - HEIGHT),
                    size = Vec2(0f, HEIGHT),
                    radii = floatArrayOf(0f, 0f, rad, rad),
                ).ignoreLayout().setPalette(polyUI.colors.brand.fg).also {
                    Resize(it, out.width, add = false, animation = progressFunc, onFinish = {
                        if (out.inputState > INPUT_NONE) {
                            failedToFinishNormally = true
                        } else finish(out)
                    }).add()
                }, recalculate = false
            )
        }
        out.apply {
            on(Event.Mouse.Exited) {
                if (failedToFinishNormally) finish(out)
                false
            }
        }
        out.setup(polyUI)
        queue.add(out)
        pop()
    }

    fun enqueue(type: Type, title: String = type.title, description: String, durationNanos: Long) = enqueueCustom(
        Block(
            Image("polyui/chevron-down.svg".image(Vec2(32f, 32f))),
            Text(title, fontSize = 14f).setFont { medium },
            Text(description, fontSize = 12f),
            size = Vec2(235f, 100f),
        ),
        progressFunc = Animations.Linear.create(durationNanos)
    )

    private fun pop() {
        if (queue.isEmpty() || i > current.size - 1) return
        val out = queue.removeAt(0)
        lastY -= out.height + PADDING
        out.y = polyUI.size.y + lastY
        Move(out, x = polyUI.size.x - out.width - PADDING, add = false, animation = Animations.Default.create(0.6.seconds)).add()
        current[i] = out
        i++
        polyUI.master.addChild(out, recalculate = false)
        polyUI.master.needsRedraw = true
    }

    private fun finish(old: Component) {
        i--
        current[0] = null
        lastY += old.height + PADDING
        Move(old, x = polyUI.size.x + PADDING, add = false, animation = Animations.Default.create(0.5.seconds), onFinish = {
            parent.removeChild(this, recalculate = false)
        }).add()
        for (i in current.indices) {
            val it = current[i] ?: continue
            Move(it, x = 0f, y = old.height + PADDING, add = true, animation = Animations.Default.create(0.5.seconds)).add()
        }
        pop()
    }

    enum class Type(val title: String) {
        Info("polyui.notify.info"), Warning("polyui.notify.warn"), Error("polyui.notify.error")
    }
}
