/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.internal.renderer

//#if FORGE==1 && MC<=11202
import cc.polyfrost.oneconfig.utils.dsl.mc
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.util.ResourceLocation

class CachedTexture(path: String, private val isUnicode: Boolean) {
    constructor() : this("textures/font/ascii.png", false)
    constructor(page: Int) : this("textures/font/unicode_page_%02x.png".format(page), true)

    private val original = ResourceLocation(path)
    val bordered = Texture("bordered:$path")
    val shadowed = Texture("shadowed:$path")

    fun load() {
        val texture = runCatching {
            TextureUtil.readBufferedImage(mc.resourceManager.getResource(original).inputStream)
        }.getOrNull() ?: return
        assert(texture.width != texture.height) { "bro why ur font not square" }
        var imageWidth = texture.width
        val glyphs = texture.getRGB(0, 0, imageWidth, imageWidth, null, 0, imageWidth)
        val scale = imageWidth < 256
        if (scale) {
            imageWidth *= 2
        }

        val glyphSize = imageWidth / 16
        val pixelSize = glyphSize / 16
        val borderedSize = pixelSize * 20
        val borderedMapSize = borderedSize * 16

        val pixel = if (isUnicode) 16 else 8
        val shadowShiftBy = glyphSize / pixel
        val shadowedSize = glyphSize + shadowShiftBy
        val shadowedMapSize = shadowedSize * 16
        val shadowedMap = IntArray(shadowedMapSize * shadowedMapSize)
        val borderedMap = IntArray(borderedMapSize * borderedMapSize)

        for (row in 0..15) {
            val glyphY = row * glyphSize
            val outlineY = row * borderedSize
            val shadowY = row * shadowedSize
            for (column in 0..15) {
                val glyphX = column * glyphSize
                val outlineX = column * borderedSize
                val shadowX = column * shadowedSize
                for (glyphYOffset in 0..<glyphSize) {
                    for (glyphXOffset in 0..<glyphSize) {
                        val glyphIndex = if (scale) {
                            ((glyphY + glyphYOffset) / 2) * (imageWidth / 2) + (glyphX + glyphXOffset) / 2
                        } else {
                            (glyphY + glyphYOffset) * imageWidth + glyphX + glyphXOffset
                        }
                        val color = glyphs[glyphIndex]
                        val alpha = color and 0xFF000000.toInt()
                        if (alpha == 0) continue

                        val normalIndex = (shadowY + glyphYOffset) * shadowedMapSize + shadowX + glyphXOffset
                        val shadowedIndex = normalIndex + shadowShiftBy * shadowedMapSize + shadowShiftBy
                        val shadowColor = color and 0xFCFCFC ushr 2 or alpha
                        shadowedMap[normalIndex] = color
                        shadowedMap[shadowedIndex] = shadowColor

                        for (yOffset in 0..4) {
                            for (xOffset in 0..4) {
                                if (((xOffset == 0 || xOffset == 4) && (yOffset == 0 || yOffset == 4)) || (xOffset == 2 && yOffset == 2)) continue
                                val outlineIndex = (outlineY + glyphYOffset + yOffset * pixelSize) * borderedMapSize + outlineX + glyphXOffset + xOffset * pixelSize
                                borderedMap[outlineIndex] = borderedMap[outlineIndex] or shadowColor
                            }
                        }
                        val outlineIndex = (outlineY + glyphYOffset + 2 * pixelSize) * borderedMapSize + outlineX + glyphXOffset + 2 * pixelSize
                        borderedMap[outlineIndex] = color
                    }
                }
            }
        }

        shadowed.load(shadowedMap, shadowedMapSize, shadowedMapSize)
        bordered.load(borderedMap, borderedMapSize, borderedMapSize)
    }
}
//#endif