package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.system.MemoryUtil
import org.polyfrost.polyui.unit.Vec4
import java.nio.ByteBuffer

/**
 * An OpenGL Atlas Manager designed to efficiently place textures of different sizes into a texture atlas, of dimensions [atlasWidth]x[atlasHeight].
 *
 * Its ID is [atlas] and can be bound using [bind] and [unbind]. Use [cleanup] to delete it, and [insert] to add your textures.
 */
class GLAtlasManager(val atlasWidth: Int, val atlasHeight: Int) {
    val atlas: Int = glGenTextures()

    init {
        val prev = glGetInteger(GL_TEXTURE_BINDING_2D)
        val prevActive = glGetInteger(GL_ACTIVE_TEXTURE)
        glBindTexture(GL_TEXTURE_2D, atlas)
        glActiveTexture(GL_TEXTURE0)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            atlasWidth,
            atlasHeight,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null as ByteBuffer?
        )
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, prev)
        glActiveTexture(prevActive)
    }

    private val skyline = ArrayList<SkylineNode>().also {
        it.add(SkylineNode(0, 0, atlasWidth))
    }

    /**
     * Insert a given texture, a ByteBuffer of [GL_UNSIGNED_BYTE] pixels, of dimensions [width] and [height] into this atlas.
     *
     * Specify the format of your data with the [format] parameter.
     *
     * @return A normalised UVWH Vec4 object referring to the dimensions and position of the texture inserted into the atlas.
     */
    fun insert(
        width: Int,
        height: Int,
        pixels: ByteBuffer,
        format: Int = GL_RGBA
    ): Vec4 = insert(width, height, MemoryUtil.memAddress(pixels), format)

    /**
     * Insert a given texture, a ByteBuffer of [GL_UNSIGNED_BYTE] pixels, of dimensions [width] and [height] into this atlas.
     *
     * Specify the format of your data with the [format] parameter.
     *
     * @return A normalised UVWH Vec4 object referring to the dimensions and position of the texture inserted into the atlas.
     */
    fun insert(
        width: Int,
        height: Int,
        pixels: Long,
        format: Int = GL_RGBA
    ): Vec4 {
        val pos = findPosition(width, height)
            ?: throw IllegalStateException("Texture atlas full")

        addSkylineLevel(pos.index, pos.x, pos.y, width, height)

        val prev = glGetInteger(GL_TEXTURE_BINDING_2D)
        val prevActive = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, atlas)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
        nglTexSubImage2D(GL_TEXTURE_2D, 0,
            pos.x, pos.y, width, height,
            format, GL_UNSIGNED_BYTE,
            pixels
        )
        glBindTexture(GL_TEXTURE_2D, prev)
        glActiveTexture(prevActive)

        return Vec4.of(
            pos.x.toFloat() / atlasWidth,
            pos.y.toFloat() / atlasHeight,
            width.toFloat() / atlasWidth,
            height.toFloat() / atlasHeight
        )
    }

    private fun findPosition(w: Int, h: Int): Placement? {
        var bestY = Int.MAX_VALUE
        var bestX = Int.MAX_VALUE
        var bestIndex = -1

        for (i in skyline.indices) {
            val y = fitsAt(i, w, h)
            if (y >= 0) {
                if (y < bestY || (y == bestY && skyline[i].x < bestX)) {
                    bestY = y
                    bestX = skyline[i].x
                    bestIndex = i
                }
            }
        }

        return if (bestIndex == -1) null else Placement(bestX, bestY, bestIndex)
    }

    private fun fitsAt(index: Int, w: Int, h: Int): Int {
        val node = skyline[index]
        if (node.x + w > atlasWidth) return -1

        var widthLeft = w
        var y = node.y
        var i = index

        while (widthLeft > 0) {
            val n = skyline[i]
            y = maxOf(y, n.y)
            if (y + h > atlasHeight) return -1

            widthLeft -= n.w
            i++
            if (i >= skyline.size && widthLeft > 0) return -1
        }

        return y
    }

    private fun addSkylineLevel(index: Int, x: Int, y: Int, w: Int, h: Int) {
        val newNode = SkylineNode(x, y + h, w)

        while (index < skyline.size) {
            val node = skyline[index]
            // Stop once we're past the new rect
            if (node.x >= x + w) break

            val nodeEnd = node.x + node.w
            val overlap = nodeEnd - (x + w)
            if (overlap > 0) {
                // Shrink node from the left
                node.x = x + w
                node.w = overlap
                break
            } else {
                // Fully covered by new rect
                skyline.removeAt(index)
                continue
            }
        }

        skyline.add(index, newNode)
        mergeSkyline()
    }

    private fun mergeSkyline() {
        var i = 0
        while (i < skyline.size - 1) {
            val a = skyline[i]
            val b = skyline[i + 1]
            if (a.y == b.y) {
                a.w += b.w
                skyline.removeAt(i + 1)
            } else i++
        }
    }

    fun bind() = glBindTexture(GL_TEXTURE_2D, atlas)

    fun unbind() = glBindTexture(GL_TEXTURE_2D, 0)

    fun cleanup() = glDeleteTextures(atlas)

    private data class SkylineNode(var x: Int, var y: Int, var w: Int)
    private data class Placement(val x: Int, val y: Int, val index: Int)
}
