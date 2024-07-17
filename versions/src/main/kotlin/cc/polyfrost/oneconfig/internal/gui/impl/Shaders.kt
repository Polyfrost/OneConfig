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

//#if FORGE==1 && MC<=11202
package cc.polyfrost.oneconfig.internal.gui.impl

import cc.polyfrost.oneconfig.libs.universal.UResolution
import cc.polyfrost.oneconfig.utils.dsl.mc
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20
import kotlin.math.ceil

val blurProgram = BlurProgram()

class BlurProgram: ShaderProgram() {
    val texture = UniformSampler("u_texture")
    val texelSize = Uniform2f("u_texelSize")
    val pass = Uniform1f("u_pass")
    val blurRadius = Uniform1f("u_blurRadius")
    val rectRadius = Uniform4f("u_rectRadius")
    val location = Uniform4f("u_location")
    override fun register() {
        registerShader("assets/shaders/blur.fsh", GL20.GL_FRAGMENT_SHADER)
        registerShader("assets/shaders/blur.vsh", GL20.GL_VERTEX_SHADER)
    }

    private fun renderFrameBufferTexture(frameBuffer: Framebuffer) {
        val scaledRes = UResolution
        val texX = frameBuffer.framebufferWidth.toFloat() / frameBuffer.framebufferTextureWidth.toFloat()
        val texY = frameBuffer.framebufferHeight.toFloat() / frameBuffer.framebufferTextureHeight.toFloat()

        glBegin(GL_QUADS)
        glTexCoord2f(0f, 0f)
        glVertex3f(0f, scaledRes.scaledHeight.toFloat(), 0f)
        glTexCoord2f(texX, 0f)
        glVertex3f(scaledRes.scaledWidth.toFloat(), scaledRes.scaledHeight.toFloat(), 0f)
        glTexCoord2f(texX, texY)
        glVertex3f(scaledRes.scaledWidth.toFloat(), 0f, 0f)
        glTexCoord2f(0f, texY)
        glVertex3f(0f, 0f, 0f)
        glEnd()
    }

    var blurBuffer = Framebuffer(Display.getWidth(), Display.getHeight(), false)
    fun render(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = UResolution.scaledWidth.toFloat(),
        height: Float = UResolution.scaledHeight.toFloat(),
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
        blurRadius: Float = 18f
    ) {
        begin()

        blurBuffer.framebufferClear()

        texelSize.x = 1f / Display.getWidth()
        texelSize.y = 1f / Display.getHeight()
        texture.textureId = 0
        this.blurRadius.x = ceil(2 * blurRadius)

        val sr = UResolution
        val scale = sr.scaleFactor.toFloat()
        val trueScale = (Display.getWidth().toFloat() / sr.scaledWidth.toFloat()) / 2f

        location.x = x * scale
        location.y = Display.getHeight() - (y + height) * scale
        location.z = width * trueScale
        location.w = height * trueScale

        rectRadius.x = topRightRadius * scale
        rectRadius.y = bottomRightRadius * scale
        rectRadius.z = topLeftRadius * scale
        rectRadius.w = bottomLeftRadius * scale

        pass.x = 1f
        applyUniforms(Display.getWidth().toFloat(), Display.getHeight().toFloat())

        blurBuffer.bindFramebuffer(true)
        mc.framebuffer.bindFramebufferTexture()
        renderFrameBufferTexture(blurBuffer)

        pass.x = 2f
        applyUniforms(Display.getWidth().toFloat(), Display.getHeight().toFloat())

        mc.framebuffer.bindFramebuffer(true)
        blurBuffer.bindFramebufferTexture()
        renderFrameBufferTexture(mc.framebuffer)

        end()
    }

}
//#endif