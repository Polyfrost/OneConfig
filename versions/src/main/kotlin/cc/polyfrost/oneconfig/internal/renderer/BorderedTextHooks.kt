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

object BorderedTextHooks : IResourceManagerReloadListener {
    val asciiTexture = CachedTexture()
    val unicodeTexture = Array(256) { page -> CachedTexture(page) }
    var textType = TextType.NONE

    fun initialize() {
        (mc.resourceManager as IReloadableResourceManager).registerReloadListener(this)
    }

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        asciiTexture.load()
        for (texture in unicodeTexture) {
            texture.load()
        }
    }

    fun drawString(text: String, x: Float, y: Float, color: Int, type: TextType): Float {
        textType = type
        val i = mc.fontRendererObj.drawString(text, x, y, color, false)
        textType = TextType.NONE
        return i.toFloat() + 1
    }
}
//#endif