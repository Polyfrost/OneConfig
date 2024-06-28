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
import cc.polyfrost.oneconfig.renderer.TextRenderer.TextType
import cc.polyfrost.oneconfig.utils.dsl.mc
import net.minecraft.client.resources.IReloadableResourceManager
import net.minecraft.client.resources.IResourceManager
import net.minecraft.client.resources.IResourceManagerReloadListener
import org.lwjgl.opengl.GL11

object BorderedTextRenderer : IResourceManagerReloadListener {
    private val asciiTexture = CachedTexture()
    private val unicodeTexture = Array(256) { page -> CachedTexture(page) }
    private var textType = TextType.NONE

    fun initialize() {
        (mc.resourceManager as IReloadableResourceManager).registerReloadListener(this)
    }

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        asciiTexture.load()
        for (texture in unicodeTexture) {
            texture.load()
        }
    }

    fun drawString(text: String, x: Float, y: Float, color: Int, textType: TextType): Float {
        BorderedTextRenderer.textType = textType
        val i = mc.fontRendererObj.drawString(text, x, y, color, false)
        BorderedTextRenderer.textType = TextType.NONE
        return i.toFloat() + 1
    }

    fun renderDefaultChar(ch: Int, italic: Boolean, charWidth: IntArray, posX: Float, posY: Float) = when (textType) {
        TextType.NONE -> null
        TextType.SHADOW -> renderDefaultCharShadowed(ch, italic, charWidth, posX, posY)
        TextType.FULL -> renderDefaultCharBordered(ch, italic, charWidth, posX, posY)
    }

    fun renderUnicodeChar(ch: Char, italic: Boolean, glyphWidth: ByteArray, posX: Float, posY: Float) = when (textType) {
        TextType.NONE -> null
        TextType.SHADOW -> renderUnicodeCharShadowed(ch, italic, glyphWidth, posX, posY)
        TextType.FULL -> renderUnicodeCharBordered(ch, italic, glyphWidth, posX, posY)
    }

    private fun renderDefaultCharShadowed(ch: Int, italic: Boolean, charWidth: IntArray, posX: Float, posY: Float): Float {
        val width = charWidth[ch].toFloat()
        val widthShrunk = width - 0.01f
        val height = 9f - 0.01f
        val u = (ch and 0xF) * 9 / 144f
        val v = (ch ushr 4) * 9 / 144f

        renderChar(
            texture = asciiTexture.shadowed,
            italic = italic,
            u = u,
            u2 = u + widthShrunk / 144f,
            v = v,
            v2 = v + height / 144f,
            x = posX,
            x2 = posX + widthShrunk,
            y = posY,
            y2 = posY + height,
        )

        return width
    }

    private fun renderUnicodeCharShadowed(char: Char, italic: Boolean, glyphWidth: ByteArray, posX: Float, posY: Float): Float {
        val ch = char.code
        val widthBits = glyphWidth[ch].toInt()
        if (widthBits == 0) return 0f

        val column = ch and 0xF
        val row = (ch and 0xFF) ushr 4
        val first4bits = widthBits ushr 4
        val last4bits = (widthBits and 0xF) + 1
        val width = (last4bits - first4bits - 0.02f)
        val height = 17 - 0.02f
        val u = (column * 17 + first4bits) / 272f
        val v = (row * 17) / 272f

        renderChar(
            texture = unicodeTexture[ch ushr 8].shadowed,
            italic = italic,
            u = u,
            u2 = u + width / 272f,
            v = v,
            v2 = v + height / 272f,
            x = posX,
            x2 = posX + width / 2f,
            y = posY,
            y2 = posY + height / 2f,
        )

        return (last4bits - first4bits) / 2.0f + 1.0f
    }

    private fun renderDefaultCharBordered(ch: Int, italic: Boolean, charWidth: IntArray, posX: Float, posY: Float): Float {
        val width = charWidth[ch]

        val column = ch and 0xF
        val row = (ch and 0xFF) ushr 4
        val width20x = width * 2 - 2 + 4f - 0.02f
        val height20x = 20 - 0.02f
        val u = column * 20 / 320f
        val v = row * 20 / 320f
        val x = posX - 1f
        val y = posY - 1f

        renderChar(
            texture = asciiTexture.bordered,
            italic = italic,
            u = u,
            u2 = u + width20x / 320f,
            v = v,
            v2 = v + height20x / 320f,
            x = x,
            x2 = x + width20x / 2f,
            y = y,
            y2 = y + height20x / 2f,
        )

        return width.toFloat()
    }

    private fun renderUnicodeCharBordered(char: Char, italic: Boolean, glyphWidth: ByteArray, posX: Float, posY: Float): Float {
        val ch = char.code
        val widthBits = glyphWidth[ch].toInt()
        if (widthBits == 0) return 0f

        val first4bits = widthBits ushr 4
        val last4bits = (widthBits and 0xF) + 1
        val column = ch and 0xF
        val row = (ch and 0xFF) ushr 4
        val width20x = last4bits - first4bits + 4f - 0.02f
        val height20x = 20 - 0.02f
        val u = (column * 20 + first4bits) / 320f
        val v = row * 20 / 320f
        val x = posX - 0.5f
        val y = posY - 0.5f

        renderChar(
            texture = unicodeTexture[ch ushr 8].bordered,
            italic = italic,
            u = u,
            u2 = u + width20x / 320f,
            v = v,
            v2 = v + height20x / 320f,
            x = x,
            x2 = x + width20x / 2f,
            y = y,
            y2 = y + height20x / 2f,
        )

        return (last4bits - first4bits) / 2.0f + 1.0f
    }

    private fun renderChar(texture: Texture, italic: Boolean, u: Float, u2: Float, v: Float, v2: Float, x: Float, x2: Float, y: Float, y2: Float) {
        val italicShift = if (italic) 1.0f else 0.0f
        texture.bind()
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
        GL11.glTexCoord2f(u, v)
        GL11.glVertex3f(x + italicShift, y, 0.0f)
        GL11.glTexCoord2f(u, v2)
        GL11.glVertex3f(x - italicShift, y2, 0.0f)
        GL11.glTexCoord2f(u2, v)
        GL11.glVertex3f(x2 + italicShift, y, 0.0f)
        GL11.glTexCoord2f(u2, v2)
        GL11.glVertex3f(x2 - italicShift, y2, 0.0f)
        GL11.glEnd()
    }
}
//#endif