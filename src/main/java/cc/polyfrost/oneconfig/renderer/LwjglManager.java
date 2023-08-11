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

package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.renderer.asset.AssetHelper;
import cc.polyfrost.oneconfig.renderer.font.FontHelper;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import cc.polyfrost.polyui.renderer.Renderer;

import java.util.ServiceLoader;

/**
 * Abstraction over the LWJGL3 implementation & loading.
 */
@SuppressWarnings("DeprecatedIsStillUsed"/*, reason = "Methods are still used internally in their respective interfaces" */)
public interface LwjglManager {
    LwjglManager INSTANCE = ServiceLoader.load(
            LwjglManager.class,
            LwjglManager.class.getClassLoader()
    ).iterator().next();

    Renderer getRenderer(float width, float height);

    /**
     * @return the {@link NanoVGHelper} platform implementation.
     * @deprecated Use {@link NanoVGHelper#INSTANCE} instead.
     */
    @Deprecated
    NanoVGHelper getNanoVGHelper();

    /**
     * @return the {@link ScissorHelper} platform implementation.
     * @deprecated Use {@link ScissorHelper#INSTANCE} instead.
     */
    @Deprecated
    ScissorHelper getScissorHelper();

    /**
     * @return the {@link AssetHelper} platform implementation.
     * @deprecated Use {@link AssetHelper#INSTANCE} instead.
     */
    @Deprecated
    AssetHelper getAssetHelper();

    @Deprecated
    FontHelper getFontHelper();

    TinyFD getTinyFD();
}
