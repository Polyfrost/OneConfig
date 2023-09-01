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

package org.polyfrost.oneconfig.ui;

import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.component.Drawable;

import java.util.function.Consumer;

public class PolyUIScreen extends UScreen implements GuiPause {
    public PolyUIScreen(Colors colors, Consumer<PolyUI> initFunction, Drawable... drawables) {
        super(true);
        throw new UnsupportedOperationException("implementation is intrinsic");
    }

    public PolyUIScreen(Colors colors, Drawable... drawables) {
        this(colors, null, drawables);
    }

    public PolyUIScreen(Drawable... drawables) {
        this(null, null, drawables);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
