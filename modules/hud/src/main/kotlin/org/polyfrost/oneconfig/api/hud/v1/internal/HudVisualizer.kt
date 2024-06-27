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

package org.polyfrost.oneconfig.api.hud.v1.internal

import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.component.setFont
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.mapToArray

class HudVisualizer : ConfigVisualizer() {

    override fun createHeaders(categories: Map<String, Drawable>) = null

    override fun flattenSubcategories(options: Map<String, Map<String, ArrayList<Triple<String, String?, Drawable>>>>): Map<String, Drawable> {
        return if (options.values.size == 1 && options.values.first().size == 1) {
            mapOf(
                options.keys.first() to Group(
                    *options.values.first().values.first().mapToArray { it.third },
                    alignment = alignVNoPad,
                )
            )
        } else super.flattenSubcategories(options)
    }

    override fun wrap(drawable: Drawable, title: String, desc: String?, icon: PolyImage?): Drawable {
        return Group(
            if (icon != null) Image(icon) else null,
            Group(
                Text(title, fontSize = 16f).setFont { medium },
                if (desc != null) Text(desc, visibleSize = Vec2(240f, 12f)) else null,
                alignment = stdOpt,
            ),
            drawable,
            alignment = stdAlign,
            size = Vec2(480f, 48f),
        )
    }
}
