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

package org.polyfrost.oneconfig.internal

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.property.impl.BlockProperties.Companion.backgroundBlock
import org.polyfrost.polyui.property.impl.BlockProperties.Companion.brandBlock
import org.polyfrost.polyui.property.impl.BlockProperties.Companion.withStates
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.unit.times
import java.util.function.Function

@ApiStatus.Internal
fun create(title: Translator.Text, message: Translator.Text, icon: PolyImage?, progressFunction: Function<Long, Float>?, action: Runnable?): Layout {
    val l = PixelLayout(
        at = origin,
        size = 400.px * 200.px,
        drawables = drawables(
            Block(
                properties = backgroundBlock,
                at = origin,
                size = 400.px * 200.px,
                events = {
                    if (action != null) {
                        MouseClicked(0) to {
                            action.run()
                        }
                    }
                }
            ).also {
                it.consumesHover = false
                if (action != null) it.properties.withStates()
            },
            Block(
                properties = brandBlock,
                at = 0.px * 190.px,
                size = 400.px * 10.px,
            ),

            )
    )
    if (progressFunction != null) {
        l.every(0.seconds) {
            this.getComponent<Block>(1).width = 400f * progressFunction.apply(this.polyUI.delta)
        }
    }
    return l
}