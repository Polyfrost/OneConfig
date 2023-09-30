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

package org.polyfrost.oneconfig.api.hud.elements

import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit

class InferringCComponent(
    properties: Properties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>? = null,
    rawResize: Boolean = false,
    acceptInput: Boolean = true,
    children: Array<out Component>,
    events: EventDSL<ContainingComponent>.() -> kotlin.Unit = {},
) : ContainingComponent(properties, at, size, rawResize, acceptInput, children, events) {
    override fun calculateSize(): Size<Unit> {
        require(children.isNotEmpty()) { "cannot infer size of component with no children" }
        val mw = children.maxOf { it.x + (it.size?.width ?: 0f) }
        val mh = children.maxOf { it.y + (it.size?.height ?: 0f) }
        if (mw == 0f) throw IllegalStateException("unable to infer width of component, please specify a size")
        if (mh == 0f) throw IllegalStateException("unable to infer height of component, please specify a size")
        return mw.px * mh.px
    }
}
