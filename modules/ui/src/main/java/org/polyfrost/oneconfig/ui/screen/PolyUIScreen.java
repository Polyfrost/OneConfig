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

package org.polyfrost.oneconfig.ui.screen;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.DeclaredInPlatform;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.event.InputManager;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.property.Settings;
import org.polyfrost.polyui.unit.Align;
import org.polyfrost.polyui.unit.Vec2;

@DeclaredInPlatform
public class PolyUIScreen {
    // PolyUIScreen.h
    // version-specific workaround //
    // see versions/src/main/java/org/polyfrost/oneconfig/ui/screen/PolyUIScreen.java for the actual implementation //
    @NotNull
    public final InputManager inputManager;
    @Nullable
    public final PolyUI polyUI;
    @Nullable
    public final Vec2 desiredResolution;
    public boolean pauses, blurs;

    @Contract("_, null, _, _, _, _, _, _, null -> fail")
    public PolyUIScreen(@Nullable Settings settings,
                        @Nullable InputManager inputManager,
                        @Nullable Translator translator,
                        @Nullable Align alignment,
                        @Nullable Colors colors,
                        @Nullable PolyColor backgroundColor,
                        @Nullable Vec2 desiredResolution,
                        @Nullable Vec2 size,
                        Drawable... drawables) {
        throw new UnsupportedOperationException("implementation is intrinsic");
    }

    public PolyUIScreen(Drawable... drawables) {
        this(null, null, null, null, null, null, null, null, drawables);
    }

    public PolyUIScreen(@Nullable Align alignment, Vec2 size, Drawable... drawables) {
        this(null, null, null, alignment, null, null, null, size, drawables);
    }

    public PolyUIScreen(@NotNull InputManager inputManager) {
        this(null, inputManager, null, null, null, null, null, null);
    }

    public PolyUIScreen(@NotNull PolyUI polyUI) {
        this((Settings) null, null, null, null, null, null, null, null);
    }
}
