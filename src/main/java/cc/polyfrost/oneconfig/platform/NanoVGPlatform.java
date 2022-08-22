/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.platform;

import org.jetbrains.annotations.Nullable;
import org.lwjgl3.nanovg.NVGLUFramebuffer;

public interface NanoVGPlatform {
    int NVG_ANTIALIAS = 1;
    int NVG_STENCIL_STROKES = 2;
    int NVG_DEBUG = 4;
    int NVG_IMAGE_NODELETE = 65536;

    int nnvglCreateImageFromHandle(long var0, int var2, int var3, int var4, int var5);

    int nvglCreateImageFromHandle(long ctx, int textureId, int w, int h, int flags);

    int nnvglImageHandle(long var0, int var2);

    int nvglImageHandle(long ctx, int image);

    long nnvgCreate(int var0);

    long nvgCreate(int flags);

    void nnvgDelete(long var0);

    void nvgDelete(long ctx);

    long nnvgluCreateFramebuffer(long var0, int var2, int var3, int var4);

    @Nullable
    NVGLUFramebuffer nvgluCreateFramebuffer(long ctx, int w, int h, int imageFlags);

    void nnvgluBindFramebuffer(long var0, long var2);

    void nvgluBindFramebuffer(long ctx, @Nullable NVGLUFramebuffer fb);

    void nnvgluDeleteFramebuffer(long var0, long var2);

    void nvgluDeleteFramebuffer(long ctx, NVGLUFramebuffer fb);

    void triggerStaticInitialization();
}
