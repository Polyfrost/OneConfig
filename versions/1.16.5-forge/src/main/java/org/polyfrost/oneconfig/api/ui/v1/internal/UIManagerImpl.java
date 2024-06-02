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

package org.polyfrost.oneconfig.api.ui.v1.internal;

import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.TinyFD;
import org.polyfrost.polyui.renderer.Renderer;

public class UIManagerImpl implements UIManager {
    private static final TinyFD impl = new TinyFDImpl();
    //#if MC>=11700
    //$$ static {
    //$$     RendererImpl.INSTANCE.setGl3(true);
    //$$ }
    //#endif

    @Override
    public Renderer getRenderer() {
        return RendererImpl.INSTANCE;
    }

    @Override
    public TinyFD getTinyFD() {
        return impl;
    }
}
