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

package cc.polyfrost.oneconfig.internal.gui;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutExpo;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.awt.Color;
import java.util.Locale;

public class OneClientPromo {
    public static final OneClientPromo INSTANCE = new OneClientPromo();

    private static final String DOWNLOAD_URL = "https://polyfrost.org/projects/oneclient";
    private static final String DISCORD_URL = "https://polyfrost.org/discord";
    private static final float ONECLIENT_AVERAGE = 1530.3f;
    private static final float ONECLIENT_LOW = 632.2f;
    private static final float FORGE_AVERAGE = 635.5f;
    private static final float FORGE_LOW = 282.7f;

    private static final int DIM = new Color(0, 0, 0, 170).getRGB();
    private static final float SCALE = 1.5f;
    private static final float WIDTH = 640;
    private static final float HEIGHT = 492;
    private static final float PADDING = 32;
    private static final float LABEL_WIDTH = 72;
    private static final float VALUE_WIDTH = 96;
    private static final float BAR_HEIGHT = 24;
    private static final float BAR_GAP = 6;
    private static final int BUTTON_WIDTH = 184;
    private static final float BUTTON_GAP = 12;
    private static final float GROUP_HEIGHT = 20 + BAR_HEIGHT * 2 + BAR_GAP;

    private final InputHandler inputHandler = new InputHandler();
    private final BasicButton downloadButton = new BasicButton(BUTTON_WIDTH, BasicButton.SIZE_40, "Download OneClient", BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
    private final BasicButton discordButton = new BasicButton(BUTTON_WIDTH, BasicButton.SIZE_40, "Support / Discord", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton laterButton = new BasicButton(BUTTON_WIDTH, BasicButton.SIZE_40, "Maybe later", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton ignoreButton = new BasicButton(BUTTON_WIDTH, BasicButton.SIZE_40, "Don't show again", BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY_DESTRUCTIVE);
    private Animation barAnimation = new EaseOutExpo(1500, 0, 1, false);
    private boolean dismissed = false;
    private boolean reopened = false;

    private OneClientPromo() {
        downloadButton.setClickAction(() -> NetworkUtils.browseLink(DOWNLOAD_URL));
        discordButton.setClickAction(() -> NetworkUtils.browseLink(DISCORD_URL));
        laterButton.setClickAction(this::dismiss);
        ignoreButton.setClickAction(this::ignore);
    }

    public boolean isActive() {
        return reopened || (OneConfigConfig.oneClientPromo && !dismissed);
    }

    public void dismiss() {
        dismissed = true;
        reopened = false;
    }

    public void ignore() {
        OneConfigConfig.oneClientPromo = false;
        OneConfigConfig.getInstance().save();
        dismiss();
    }

    public void show() {
        reopened = true;
        barAnimation = new EaseOutExpo(1500, 0, 1, false);
    }

    public void draw() {
        NanoVGHelper nvg = NanoVGHelper.INSTANCE;
        nvg.setupAndDraw(vg -> {
            nvg.drawRect(vg, 0, 0, UResolution.getWindowWidth(), UResolution.getWindowHeight(), DIM);

            float scale = OneConfigGui.getScaleFactor() * SCALE;
            float x = (UResolution.getWindowWidth() / scale - WIDTH) / 2f;
            float y = (UResolution.getWindowHeight() / scale - HEIGHT) / 2f;
            nvg.scale(vg, scale, scale);
            inputHandler.scale(scale, scale);

            nvg.drawDropShadow(vg, x, y, WIDTH, HEIGHT, 64, 0, 20);
            nvg.drawRoundedRect(vg, x, y, WIDTH, HEIGHT, Colors.GRAY_800, 20);

            float contentX = x + PADDING;
            nvg.drawText(vg, "Try OneClient", contentX, y + 52, Colors.WHITE, 28, Fonts.BOLD);
            nvg.drawText(vg, "OneClient 1.8.9 runs on Fabric with modern rendering optimizations from Sodium.", contentX, y + 84, Colors.WHITE_80, 14, Fonts.REGULAR);
            nvg.drawText(vg, "Its lightweight Rust launcher has no useless browser running in the background.", contentX, y + 104, Colors.WHITE_80, 14, Fonts.REGULAR);
            nvg.drawText(vg, "Our Forge mods haven't been updated in over a year and won't get any more updates.", contentX, y + 124, Colors.WHITE_80, 14, Fonts.REGULAR);
            nvg.drawText(vg, "All of your mods are already on OneClient!", contentX, y + 144, Colors.WHITE_80, 14, Fonts.BOLD);

            float progress = barAnimation.get();
            float groupY = y + 184;
            drawGroup(vg, contentX, groupY, SVGs.ONECLIENT, "OneClient 1.8.9 + Sodium backport", ONECLIENT_AVERAGE, ONECLIENT_LOW, Colors.PRIMARY_500, Colors.PRIMARY_700, progress, scale);
            drawGroup(vg, contentX, groupY + GROUP_HEIGHT + 18, SVGs.FORGE, "Forge 1.8.9 (with OptiFine + PolyPatcher)", FORGE_AVERAGE, FORGE_LOW, Colors.GRAY_300, Colors.GRAY_400, progress, scale);
            nvg.drawText(vg, "Tested on a Ryzen 5 5600X and Radeon RX 5700 XT.", contentX, groupY + GROUP_HEIGHT * 2 + 42, Colors.WHITE_50, 12, Fonts.REGULAR);

            float buttonY = y + HEIGHT - PADDING - 40;
            downloadButton.draw(vg, contentX, buttonY, inputHandler);
            discordButton.draw(vg, contentX + BUTTON_WIDTH + BUTTON_GAP, buttonY, inputHandler);
            (reopened ? ignoreButton : laterButton).draw(vg, contentX + (BUTTON_WIDTH + BUTTON_GAP) * 2, buttonY, inputHandler);
        });
    }

    private void drawGroup(long vg, float x, float y, SVG logo, String name, float average, float low, int averageColor, int lowColor, float progress, float scale) {
        NanoVGHelper nvg = NanoVGHelper.INSTANCE;
        nvg.drawSvg(vg, logo, x, y - 1, 16, 16, Colors.WHITE, scale);
        nvg.drawText(vg, name, x + 22, y + 8, Colors.WHITE, 14, Fonts.SEMIBOLD);
        drawBar(vg, x, y + 20, "Average", average, averageColor, progress);
        drawBar(vg, x, y + 20 + BAR_HEIGHT + BAR_GAP, "1% low", low, lowColor, progress);
    }

    private void drawBar(long vg, float x, float y, String label, float fps, int color, float progress) {
        NanoVGHelper nvg = NanoVGHelper.INSTANCE;
        float middle = y + BAR_HEIGHT / 2f + 1;
        float maxWidth = WIDTH - PADDING * 2 - LABEL_WIDTH - VALUE_WIDTH;
        float barWidth = Math.max(6, maxWidth * fps / ONECLIENT_AVERAGE * progress);
        nvg.drawText(vg, label, x, middle, Colors.WHITE_80, 12, Fonts.MEDIUM);
        nvg.drawRoundedRect(vg, x + LABEL_WIDTH, y, barWidth, BAR_HEIGHT, color, 6);
        nvg.drawText(vg, String.format(Locale.ROOT, "%.1f FPS", fps * progress), x + LABEL_WIDTH + barWidth + 12, middle, Colors.WHITE, 13, Fonts.SEMIBOLD);
    }
}
