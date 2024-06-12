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

import org.polyfrost.oneconfig.api.config.v1.annotations.Text
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.utils.resourceExists

class ImageHud(@Text(title = "Image Path") val address: String) : Hud<Image>() {
    override fun id() = "image_hud.toml"

    override fun title() = "Image Hud"

    override fun category() = Category.INFO

    override fun create() = Image(address)

    override fun update() = true

    override fun initialize() {
        if (isReal) {
            addCallback<String>("address") {
                if (!resourceExists(it)) return@addCallback false
                return@addCallback try {
                    get().image = PolyImage(it)
                    false
                } catch (e: Exception) {
                    true
                }
            }
        }
    }

    override fun updateFrequency() = -1L
}
