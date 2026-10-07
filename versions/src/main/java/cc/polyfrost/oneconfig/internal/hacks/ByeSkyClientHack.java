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

package cc.polyfrost.oneconfig.internal.hacks;

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ByeSkyClientHack {

    private static final File crashPatchSkyClientFile = new File("./OneConfig/CrashPatch/SKYCLIENT");
    private static final File oldCrashPatchSkyClientFile = new File("./W-OVERFLOW/CrashPatch/SKYCLIENT");
    public static final ByeSkyClientHack INSTANCE = new ByeSkyClientHack();

    private static final String[] CAROUSEL_IMAGES = {
            "/oneclient1.jpg",
            "/oneclient2.jpg",
            "/oneclient3.jpg"
    };

    private static final float[] CAROUSEL_RATIOS = {
            644f / 1050f,
            803f / 1281f,
            803f / 1281f
    };

    private static final String DOWNLOAD_URL = "https://polyfrost.org/projects/oneclient";
    private static final String DISCORD_URL = "https://discord.gg/polyfrost";
    private static final String FAQ_URL = "https://polyfrost.org/blog/oneclient-announcement";

    private boolean dismissed = false;
    private boolean wasMouseDown = false;

    private int currentImage = 0;
    private long lastImageSwitch = 0;
    private static final long AUTO_SWITCH_MS = 5000;

    private float scrollY = 0;
    private float targetScrollY = 0;
    private long firstShownTime = 0;

    public void initialize() {
    }

    public void overwriteGui(int mouseX, int mouseY, float partialTicks) {
        NanoVGHelper nvg = NanoVGHelper.INSTANCE;

        float scaleFactor = (float) UResolution.getScaleFactor();
        float mx = mouseX * scaleFactor;
        float my = mouseY * scaleFactor;

        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            targetScrollY += dWheel > 0 ? 40 : -40;
            if (targetScrollY > 0) targetScrollY = 0;
        }
        scrollY += (targetScrollY - scrollY) * 0.3f;
        if (scrollY > 0) scrollY = 0;

        boolean isMouseDown = Mouse.isButtonDown(0);
        boolean clicked = !isMouseDown && wasMouseDown;
        wasMouseDown = isMouseDown;

        nvg.setupAndDraw(vg -> {
            float windowW = UResolution.getWindowWidth();
            float windowH = UResolution.getWindowHeight();

            if (firstShownTime == 0) firstShownTime = System.currentTimeMillis();

            nvg.drawRect(vg, 0, 0, windowW, windowH, Colors.GRAY_900);

            float contentW = Math.min(700, windowW - 60);
            float contentX = (windowW - contentW) / 2f;

            LocalDate endOfSupportDate = LocalDate.of(2026, 2, 24);
            LocalDate today = LocalDate.now();
            long daysDiff = ChronoUnit.DAYS.between(endOfSupportDate, today);
            String daysText;
            if (daysDiff > 0) {
                daysText = daysDiff + " days ago";
            } else if (daysDiff == 0) {
                daysText = "today";
            } else {
                daysText = "in " + Math.abs(daysDiff) + " days";
            }

            float yBase = 40;
            float y = yBase + scrollY;

            float titleSize = 32;
            nvg.drawCenteredText(vg, "SkyClient is moving to OneClient",
                    windowW / 2f, y + titleSize / 2f, Colors.WHITE, titleSize, Fonts.BOLD);
            y += titleSize + 20;

            nvg.drawRect(vg, contentX, y, contentW, 1, Colors.GRAY_600);
            y += 16;

            float descSize = 14;
            String desc1 = "Starting from February 24th, 2026 (" + daysText
                    + "), Hypixel SkyBlock will be removing 1.8 support. "
                    + "As a result, we are ending support for SkyClient.";
            float[] desc1Bounds = nvg.getWrappedStringBounds(vg, desc1, contentW, descSize, 1.4f, Fonts.REGULAR);
            float desc1Height = desc1Bounds[3] - desc1Bounds[1];
            nvg.drawWrappedString(vg, desc1, contentX, y + descSize / 2f,
                    contentW, Colors.WHITE_80, descSize, 1.4f, Fonts.REGULAR);
            y += desc1Height + 20;

            float desc2Size = 16;
            String desc2 = "We have been developing OneClient, the 1.21 version of SkyClient "
                    + "with its own launcher and updated to the latest mods. We recommend trying it "
                    + "as it is the closest thing now to SkyClient and made by the same team.";
            float[] desc2Bounds = nvg.getWrappedStringBounds(vg, desc2, contentW, desc2Size, 1.4f, Fonts.MEDIUM);
            float desc2Height = desc2Bounds[3] - desc2Bounds[1];
            nvg.drawWrappedString(vg, desc2, contentX, y + desc2Size / 2f,
                    contentW, Colors.WHITE, desc2Size, 1.4f, Fonts.MEDIUM);
            y += desc2Height + 28;

            float imgW = contentW;
            float imgH = imgW * CAROUSEL_RATIOS[currentImage];

            long now = System.currentTimeMillis();
            if (lastImageSwitch == 0) lastImageSwitch = now;
            if (now - lastImageSwitch > AUTO_SWITCH_MS) {
                currentImage = (currentImage + 1) % CAROUSEL_IMAGES.length;
                lastImageSwitch = now;
            }

            nvg.drawDropShadow(vg, contentX, y, imgW, imgH, 12, 4, 12);
            nvg.drawRoundImage(vg, CAROUSEL_IMAGES[currentImage],
                    contentX, y, imgW, imgH, 12, ByeSkyClientHack.class);

            float arrowAreaW = 50;
            float arrowCenterY = y + imgH / 2f;
            boolean leftHover = mx >= contentX && mx <= contentX + arrowAreaW && my >= y && my <= y + imgH;
            int leftArrowColor = leftHover
                    ? new Color(255, 255, 255, 180).getRGB()
                    : new Color(255, 255, 255, 80).getRGB();
            nvg.drawCenteredText(vg, "<", contentX + arrowAreaW / 2f,
                    arrowCenterY, leftArrowColor, 28, Fonts.BOLD);
            if (clicked && leftHover) {
                currentImage = (currentImage - 1 + CAROUSEL_IMAGES.length) % CAROUSEL_IMAGES.length;
                lastImageSwitch = System.currentTimeMillis();
            }

            float rightArrowX = contentX + imgW - arrowAreaW;
            boolean rightHover = mx >= rightArrowX && mx <= contentX + imgW && my >= y && my <= y + imgH;
            int rightArrowColor = rightHover
                    ? new Color(255, 255, 255, 180).getRGB()
                    : new Color(255, 255, 255, 80).getRGB();
            nvg.drawCenteredText(vg, ">", rightArrowX + arrowAreaW / 2f,
                    arrowCenterY, rightArrowColor, 28, Fonts.BOLD);
            if (clicked && rightHover) {
                currentImage = (currentImage + 1) % CAROUSEL_IMAGES.length;
                lastImageSwitch = System.currentTimeMillis();
            }

            float dotAreaY = y + imgH + 12;
            float dotRadius = 4;
            float dotSpacing = 16;
            float dotsWidth = CAROUSEL_IMAGES.length * dotRadius * 2
                    + (CAROUSEL_IMAGES.length - 1) * (dotSpacing - dotRadius * 2);
            float dotStartX = (windowW - dotsWidth) / 2f;
            for (int i = 0; i < CAROUSEL_IMAGES.length; i++) {
                float dotCenterX = dotStartX + i * dotSpacing + dotRadius;
                float dotCenterY = dotAreaY + dotRadius;
                int dotColor = (i == currentImage) ? Colors.PRIMARY_500 : Colors.GRAY_500;
                nvg.drawCircle(vg, dotCenterX, dotCenterY, dotRadius, dotColor);

                if (clicked && mx >= dotCenterX - dotRadius - 4 && mx <= dotCenterX + dotRadius + 4
                        && my >= dotCenterY - dotRadius - 4 && my <= dotCenterY + dotRadius + 4) {
                    currentImage = i;
                    lastImageSwitch = System.currentTimeMillis();
                }
            }

            y = dotAreaY + dotRadius * 2 + 24;

            float btnH = 40;
            float btnSpacing = 12;
            float btnRadius = 8;
            float btnFontSize = 14;

            boolean downloadHovered = mx >= contentX && mx <= contentX + contentW
                    && my >= y && my <= y + btnH;
            nvg.drawRoundedRect(vg, contentX, y, contentW, btnH,
                    downloadHovered ? Colors.PRIMARY_500 : Colors.PRIMARY_600, btnRadius);
            nvg.drawCenteredText(vg, "Download OneClient",
                    contentX + contentW / 2f, y + btnH / 2f, Colors.WHITE, btnFontSize, Fonts.SEMIBOLD);
            if (clicked && downloadHovered) {
                NetworkUtils.browseLink(DOWNLOAD_URL);
            }
            y += btnH + btnSpacing;

            float halfW = (contentW - btnSpacing) / 2f;
            float discordBtnX = contentX;
            float faqBtnX = contentX + halfW + btnSpacing;

            boolean discordHovered = mx >= discordBtnX && mx <= discordBtnX + halfW
                    && my >= y && my <= y + btnH;
            nvg.drawRoundedRect(vg, discordBtnX, y, halfW, btnH,
                    discordHovered ? Colors.GRAY_300 : Colors.GRAY_500, btnRadius);
            nvg.drawCenteredText(vg, "Discord",
                    discordBtnX + halfW / 2f, y + btnH / 2f, Colors.WHITE, btnFontSize, Fonts.SEMIBOLD);
            if (clicked && discordHovered) {
                NetworkUtils.browseLink(DISCORD_URL);
            }

            boolean faqHovered = mx >= faqBtnX && mx <= faqBtnX + halfW
                    && my >= y && my <= y + btnH;
            nvg.drawRoundedRect(vg, faqBtnX, y, halfW, btnH,
                    faqHovered ? Colors.GRAY_300 : Colors.GRAY_500, btnRadius);
            nvg.drawCenteredText(vg, "FAQ",
                    faqBtnX + halfW / 2f, y + btnH / 2f, Colors.WHITE, btnFontSize, Fonts.SEMIBOLD);
            if (clicked && faqHovered) {
                NetworkUtils.browseLink(FAQ_URL);
            }
            y += btnH + btnSpacing;

            boolean continueHovered = mx >= contentX && mx <= contentX + contentW
                    && my >= y && my <= y + btnH;
            nvg.drawRoundedRect(vg, contentX, y, contentW, btnH,
                    continueHovered ? Colors.GRAY_300 : Colors.GRAY_600, btnRadius);
            nvg.drawCenteredText(vg, "Continue using SkyClient",
                    contentX + contentW / 2f, y + btnH / 2f, Colors.WHITE_60, btnFontSize, Fonts.MEDIUM);
            if (clicked && continueHovered) {
                dismissed = true;
            }

            float totalContentHeight = (y - scrollY) + btnH + 40;
            float maxScroll = totalContentHeight - windowH;
            if (maxScroll > 0) {
                if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;

                float trackW = 4;
                float trackX = windowW - 12;
                nvg.drawRoundedRect(vg, trackX, 0, trackW, windowH, Colors.GRAY_700, 2);

                float viewRatio = windowH / totalContentHeight;
                float thumbH = Math.max(30, windowH * viewRatio);
                float scrollFraction = -scrollY / maxScroll;
                float thumbY = scrollFraction * (windowH - thumbH);
                nvg.drawRoundedRect(vg, trackX, thumbY, trackW, thumbH, Colors.GRAY_400, 2);

                if (-scrollY < maxScroll) {
                    nvg.drawGradientRect(vg, 0, windowH - 80, windowW, 80,
                            Colors.GRAY_900 & 0x00FFFFFF, Colors.GRAY_900,
                            NanoVGHelper.GradientDirection.DOWN);
                }

                long elapsed = System.currentTimeMillis() - firstShownTime;
                long fadeDuration = 500;
                long holdDuration = 3000;
                if (elapsed < holdDuration + fadeDuration) {
                    float hintAlpha;
                    if (elapsed < holdDuration) {
                        hintAlpha = 1f;
                    } else {
                        hintAlpha = 1f - (float)(elapsed - holdDuration) / fadeDuration;
                    }
                    int hintColor = new Color(1f, 1f, 1f, hintAlpha * 0.7f).getRGB();
                    float hintSize = 13;
                    String hintText = "Scroll down \u2193";
                    float hintX = windowW / 2f;
                    float hintY = windowH - 18;
                    nvg.drawCenteredText(vg, hintText, hintX, hintY, hintColor, hintSize, Fonts.MEDIUM);
                }
            } else {
                targetScrollY = 0;
            }
        });
    }

    public boolean isSkyClient() {
        if (dismissed) return false;
        if (crashPatchSkyClientFile.exists()) return true;
        if (oldCrashPatchSkyClientFile.exists()) return true;
        return Platform.getLoaderPlatform().getLoadedMods().stream().anyMatch(
                mod -> mod != null && (StringUtils.contains(mod.id, "skyclient")
                        || StringUtils.contains(mod.id, "skyblockclient")
                        || StringUtils.equals(mod.id, "scc")));
    }
}
