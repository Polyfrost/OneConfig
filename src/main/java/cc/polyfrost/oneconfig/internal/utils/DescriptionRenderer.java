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

package cc.polyfrost.oneconfig.internal.utils;

import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutQuad;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DescriptionRenderer {
    private static final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

    public static int drawDescription(
            long vg,
            int x,
            int y,
            @NotNull String description,
            @Nullable String warningDescription,
            @NotNull Supplier<Animation> animationSupplier,
            @NotNull Consumer<Animation> animationSetter,
            @Nullable SVG icon,
            boolean shouldDrawDescription,
            @NotNull DescriptionPosition position,
            @NotNull InputHandler inputHandler
    ) {
        Animation animation = animationSupplier.get();

        Animation targetAnim = null;
        if (animation.getEnd() != 1f && shouldDrawDescription) {
            targetAnim = new EaseOutQuad(150, animation.get(0), 1f, false);
        } else if (animation.getEnd() != 0f && !shouldDrawDescription) {
            targetAnim = new EaseOutQuad(150, animation.get(0), 0f, false);
        }
        if (targetAnim != null) {
            animation = targetAnim;
            animationSetter.accept(targetAnim);
        }

        if (!shouldDrawDescription && animation.isFinished()) return -1;

        float textHeight;
        float textWidth;
        float[] descriptionBounds = nanoVGHelper.getWrappedStringBounds(vg, description, 400, 16, 1f, Fonts.MEDIUM);
        float[] warningBounds = new float[0];
        if (warningDescription == null) {
            textHeight = descriptionBounds[3] - descriptionBounds[1];
            textWidth = descriptionBounds[2] - descriptionBounds[0];
        } else {
            warningBounds = nanoVGHelper.getWrappedStringBounds(vg, warningDescription, 400, 16, 1f, Fonts.MEDIUM);
            textHeight = (descriptionBounds[3] - descriptionBounds[1]) + (warningBounds[3] - warningBounds[1]);
            textWidth = Math.max(descriptionBounds[2] - descriptionBounds[0], warningBounds[2] - warningBounds[0]);
        }

        nanoVGHelper.setAlpha(vg, animation.get());

        switch (position) {
            case RIGHT:
                nanoVGHelper.translate(vg, -(textWidth + 68), 0);
                break;
            case MIDDLE:
                nanoVGHelper.translate(vg, -(textWidth / 2f + 34), 0);
                break;
        }
        int returnValue;
        if (warningDescription != null) {
            returnValue = (int) (88f + 3f + (textHeight - 32f));
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 42f - 47f - (textHeight - 32f), textWidth + 70f, returnValue, Colors.GRAY_700, 8f);
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 42f - 47f - (textHeight - 32f), textWidth + 70f, returnValue, ColorUtils.getColor(204, 204, 204, 25), 8f);
            nanoVGHelper.drawRoundedRect(vg, x, y - 42f - 46f - (textHeight - 32f), textWidth + 68f, returnValue - 2f, Colors.GRAY_700, 8f);

            nanoVGHelper.drawSvg(vg, SVGs.WARNING, x + 16, y - 30f - (warningBounds[3] - warningBounds[1] - 16f), 20f, 20f, ColorUtils.getColor(223, 51, 39));
            nanoVGHelper.drawWrappedString(vg, warningDescription, x + 52, y - 19f - (warningBounds[3] - warningBounds[1] - 16f), 400, ColorUtils.getColor(223, 51, 39), 16, 1f, Fonts.MEDIUM);

            nanoVGHelper.drawLine(vg, x + 16f, y - 42f - 47f - (textHeight - 32f) + ((88f + 3f + (textHeight - 32f)) / 2), x + textWidth + 68f - 16f, y - 42f - 47f - (textHeight - 32f) + ((88f + 3f + (textHeight - 32f)) / 2), 1, ColorUtils.getColor(204, 204, 204, 25));

            nanoVGHelper.drawSvg(vg, SVGs.INFO_ARROW, x + 16, y - 14f - 45f - (textHeight - 16f), 20f, 20f, Colors.WHITE_80);
            nanoVGHelper.drawWrappedString(vg, description, x + 52, y - 3f - 45f - (textHeight - 16f), 400, Colors.WHITE_80, 16, 1f, Fonts.MEDIUM);
        } else {
            returnValue = (int) (44f + 2f + (textHeight - 16f));
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 42f - 1f - (textHeight - 16f), textWidth + 70f, returnValue, Colors.GRAY_700, 8f);
            nanoVGHelper.drawRoundedRect(vg, x - 1f, y - 42f - 1f - (textHeight - 16f), textWidth + 70f, returnValue, ColorUtils.getColor(204, 204, 204, 25), 8f);
            nanoVGHelper.drawRoundedRect(vg, x, y - 42f - (textHeight - 16f), textWidth + 68f, returnValue - 2f, Colors.GRAY_700, 8f);
            nanoVGHelper.drawSvg(vg, (icon == null ? SVGs.INFO_ARROW : icon), x + 16, y - 30f - (textHeight - 16f), 20f, 20f, Colors.WHITE_80);
            nanoVGHelper.drawWrappedString(vg, description, x + 52, y - 19 - (textHeight - 16f), 400, Colors.WHITE_80, 16, 1f, Fonts.MEDIUM);
        }
        switch (position) {
            case RIGHT:
                nanoVGHelper.translate(vg, (textWidth + 68), 0);
                break;
            case MIDDLE:
                nanoVGHelper.translate(vg, (textWidth / 2f + 34), 0);
                break;
        }
        nanoVGHelper.setAlpha(vg, 1f);
        return returnValue;
    }

    public enum DescriptionPosition {
        LEFT, MIDDLE, RIGHT
    }
}
