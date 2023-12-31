/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.ui.screen;

import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.component.Drawable;

import java.util.function.Consumer;

public class PolyUIScreen {
    // expose width and height
    public final float width, height;

    public PolyUIScreen(float width, float height, Colors colors, Consumer<PolyUI> initFunction, Drawable... drawables) {
        this.width = width;
        this.height = height;
        throw new UnsupportedOperationException("implementation is intrinsic");
        // see versions/src/main/java/org/polyfrost/oneconfig/ui/screen/PolyUIScreen.java for the actual implementation //
    }

    public PolyUIScreen(float width, float height, Colors colors, Drawable... drawables) {
        this(width, height, colors, null, drawables);
    }

    public PolyUIScreen(float width, float height, Drawable... drawables) {
        this(width, height, null, null, drawables);
    }

    public final Drawable getMaster() {
        // intrinsic //
        return null;
    }

    public final PolyUI getPolyUI() {
        // intrinsic //
        return null;
    }
}
