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

import cc.polyfrost.oneconfig.internal.OneConfig
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.*

abstract class ShaderProgram(
    val program: Int = glCreateProgram(),
    val uniforms: MutableList<Uniform> = mutableListOf(),
    var active: Boolean = false
) {
    abstract fun register()
    init {
        register()
        glLinkProgram(program)
        glValidateProgram(program)
    }

    protected val genesis = System.currentTimeMillis()
    protected val resolution = Uniform2f("u_resolution")
    protected val time = Uniform1f("u_time")
    open fun begin() {
        active = true
        glUseProgram(program)
    }
    open fun applyUniforms(width: Float, height: Float) {
        resolution.x = width
        resolution.y = height

        time.x = (System.currentTimeMillis() - genesis) / 1000f

        uniforms.forEach { it.apply() }
    }
    open fun end() {
        if (!active) return
        active = false
        glUseProgram(0)
    }

    fun registerShader(
        location: String,
        type: Int
    ) {
        val source = OneConfig.INSTANCE::class.java.classLoader.getResource(location) ?: throw IllegalStateException("Failed to fetch resource $location")
        val shader = initShader(
            location.substringAfterLast("/").substringBeforeLast("."),
            source.readBytes().toString(Charsets.UTF_8),
            type
        )

        glAttachShader(program, shader)
    }
    private fun initShader(name: String, shaderSource: String, type: Int) : Int {
        var shader = 0

        try {
            shader = glCreateShader(type)
            glShaderSource(shader, shaderSource)
            glCompileShader(shader)

            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw Exception(
                    "Error in creating shader $name :${
                        glGetShaderInfoLog(shader, Short.MAX_VALUE.toInt())
                    }"
                )

            return shader
        } catch (ex: Exception) {
            glDeleteShader(shader)
            throw ex
        }
    }

    fun getUniform(name: String) = glGetUniformLocation(program, name)
    abstract inner class Uniform(
        name: String,
        var location: Int = 0
    ) {
        init {
            location = getUniform(name)
            registerUniform()
        }
        private fun registerUniform() = uniforms.add(this)
        internal abstract fun apply()
    }

    open inner class Uniform1f(
        name: String,
        var x: Float = 0f
    ) : Uniform(name) {
        override fun apply() {
            glUniform1f(location, x)
        }
    }

    open inner class Uniform2f(
        name: String,
        var x: Float = 0f,
        var y: Float = 0f
    ) : Uniform(name) {
        override fun apply() {
            glUniform2f(location, x, y)
        }
    }

    open inner class Uniform4f(
        name: String,
        var x: Float = 0f,
        var y: Float = 0f,
        var z: Float = 0f,
        var w: Float = 0f
    ) : Uniform(name) {
        override fun apply() {
            glUniform4f(location, x, y, z, w)
        }
    }

    open inner class UniformSampler(
        name: String,
        var textureId: Int = 0
    ): Uniform(name) {
        override fun apply() {
            glUniform1i(this.location, this.textureId)
        }
    }

}
//#endif