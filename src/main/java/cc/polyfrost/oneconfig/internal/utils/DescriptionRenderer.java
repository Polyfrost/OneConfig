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

package cc.polyfrost.oneconfig.internal.utils;

import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutQuad;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DescriptionRenderer {
    private static final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

    public static void drawDescription(
            long vg,
            int x,
            int y,
            @NotNull
            String description,
            @Nullable String warningDescription,
            @NotNull Supplier<Animation> animationSupplier,
            @NotNull Consumer<Animation> animationSetter,
            boolean shouldDrawDescription,
            @NotNull DescriptionPosition position,
            @NotNull InputHandler inputHandler
    ) {

        if (animationSupplier.get().getEnd() != 1f && shouldDrawDescription) {
            animationSetter.accept(new EaseOutQuad(150, animationSupplier.get().get(0), 1f, false));
        } else if (animationSupplier.get().getEnd() != 0f && !shouldDrawDescription) {
            animationSetter.accept(new EaseOutQuad(150, animationSupplier.get().get(0), 0f, false));
        }

        if (!shouldDrawDescription && animationSupplier.get().isFinished()) return;
        float textHeight;
        float textWidth;
        float[] descriptionBounds = nanoVGHelper.getWrappedStringBounds(vg, description, 400, 16, Fonts.MEDIUM);
        float[] warningBounds = nanoVGHelper.getWrappedStringBounds(vg, warningDescription, 400, 16, Fonts.MEDIUM);
        if (warningDescription == null) {
            textHeight = descriptionBounds[3] - descriptionBounds[1];
            textWidth = descriptionBounds[2] - descriptionBounds[0];
        } else {
            textHeight = (descriptionBounds[3] - descriptionBounds[1]) + (warningBounds[3] - warningBounds[1]);
            textWidth = Math.max(descriptionBounds[2] - descriptionBounds[0], warningBounds[2] - warningBounds[0]);
        }
        nanoVGHelper.setAlpha(vg, animationSupplier.get().get());

        if (position == DescriptionPosition.RIGHT) {
            nanoVGHelper.translate(vg, -(textWidth + 68), 0);
        }
        if (warningDescription != null) {
            nanoVGHelper.drawDropShadow(vg, x - 1f, y - 42f - 47f, textWidth + 70f, 88f + 3f, 32f, 0f, 8f);
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 42f - 47f, textWidth + 70f, 88f + 3f, Colors.GRAY_700, 8f);
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 42f - 47f, textWidth + 70f, 88f + 3f, ColorUtils.getColor(204, 204, 204, 25), 8f);
            nanoVGHelper.drawRoundedRect(vg, x, y - 42f - 46f, textWidth + 68f, 88f + 1f, Colors.GRAY_700, 8f);
            nanoVGHelper.drawSvg(vg, SVGs.INFO_ARROW, x + 16, y - 30f - 45f, 20f, 20f, Colors.WHITE_80);
            nanoVGHelper.drawWrappedString(vg, description, x + 52, y - 19 - 45f, 200, Colors.WHITE_80, 16, Fonts.MEDIUM);
            nanoVGHelper.drawLine(vg, x + 16f, y + 44, x + textWidth + 68f - 16f, y, 1, ColorUtils.getColor(204, 204, 204, 25));
            nanoVGHelper.drawSvg(vg, SVGs.WARNING, x + 16, y - 30f, 20f, 20f, ColorUtils.getColor(223, 51, 39));
            nanoVGHelper.drawWrappedString(vg, warningDescription, x + 52, y - 19f, 200, ColorUtils.getColor(223, 51, 39), 16, Fonts.MEDIUM);
        } else {
            nanoVGHelper.drawDropShadow(vg, x - 1f, y - 41f, textWidth + 70f, 44f + 2f, 32f, 0f, 8f);
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 41f, textWidth + 70f, 44f + 2f, ColorUtils.getColor(204, 204, 204), 8f);
            nanoVGHelper.drawRoundedRect(vg, x, y - 42f, textWidth + 68f, 44f, Colors.GRAY_700, 8f);
            nanoVGHelper.drawSvg(vg, SVGs.INFO_ARROW, x + 16, y - 30f, 20f, 20f, Colors.WHITE_80);
            nanoVGHelper.drawWrappedString(vg, description, x + 52, y - 19, 200, Colors.WHITE_80, 16, Fonts.MEDIUM);
        }
        if (position == DescriptionPosition.RIGHT) {
            nanoVGHelper.translate(vg, textWidth + 68, 0);
        }
        nanoVGHelper.setAlpha(vg, 1f);
    }

    public enum DescriptionPosition {
        LEFT, MEDIUM, RIGHT
    }
}
