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

package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.gui.pages.ModsPage;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.LwjglManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import cc.polyfrost.oneconfig.utils.gui.OneUIScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class OneConfigGui extends OneUIScreen {
    public static OneConfigGui INSTANCE;
    private final SideBar sideBar = new SideBar();
    private final TextInputField textInputField = new TextInputField(248, 40, "Search...", false, false, SVGs.MAGNIFYING_GLASS_BOLD);
    private final ArrayList<Page> previousPages = new ArrayList<>();
    private final ArrayList<Page> nextPages = new ArrayList<>();
    private final BasicElement backArrow = new BasicElement(40, 40, new ColorPalette(Colors.GRAY_700, Colors.GRAY_500, Colors.GRAY_500_80), true);
    private final BasicElement forwardArrow = new BasicElement(40, 40, new ColorPalette(Colors.GRAY_700, Colors.GRAY_500, Colors.GRAY_500_80), true);
    public ColorSelector currentColorSelector;
    public boolean allowClose = true;
    protected Page currentPage;
    protected Page prevPage;
    private float scale = 1f;
    private Animation animation;

    public OneConfigGui() {
        INSTANCE = this;
    }

    public OneConfigGui(Page page) {
        INSTANCE = this;
        currentPage = page;
    }

    public static OneConfigGui create() {
        return INSTANCE == null ? new OneConfigGui() : INSTANCE;
    }

    @Override
    public void draw(long vg, float partialTicks, InputHandler inputHandler) {
        long start = System.nanoTime();
        if (currentPage == null) {
            currentPage = new ModsPage();
            currentPage.parents.add(currentPage);
        }
        if (OneConfigConfig.australia) {
            LwjglManager.INSTANCE.getNanoVGHelper().translate(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight());
            LwjglManager.INSTANCE.getNanoVGHelper().rotate(vg, (float) Math.toRadians(180));
        }
        scale = Preferences.enableCustomScale ? Preferences.customScale : Math.min(UResolution.getWindowWidth() / 1920f, UResolution.getWindowHeight() / 1080f);
        if (scale < 1 && !Preferences.enableCustomScale)
            scale = Math.min(Math.min(1f, UResolution.getWindowWidth() / 1280f), Math.min(1f, UResolution.getWindowHeight() / 800f));
        scale = (float) (Math.floor(scale / 0.05f) * 0.05f);
        int x = (int) ((UResolution.getWindowWidth() - 1280 * scale) / 2f / scale);
        int y = (int) ((UResolution.getWindowHeight() - 800 * scale) / 2f / scale);
        LwjglManager.INSTANCE.getNanoVGHelper().scale(vg, scale, scale);
        inputHandler.scale(scale, scale);

        LwjglManager.INSTANCE.getNanoVGHelper().drawDropShadow(vg, x, y, 1280, 800, 32, 0, 20);
        LwjglManager.INSTANCE.getNanoVGHelper().drawRoundedRect(vg, x + 224, y, 1056, 800, Colors.GRAY_800, 20f);
        LwjglManager.INSTANCE.getNanoVGHelper().drawRoundedRect(vg, x, y, 244, 800, Colors.GRAY_800_95, 20f);
        LwjglManager.INSTANCE.getNanoVGHelper().drawRect(vg, x + 224, y, 20, 800, Colors.GRAY_800);
        LwjglManager.INSTANCE.getNanoVGHelper().drawHollowRoundRect(vg, x - 1, y - 1, 1282, 802, 0x4DCCCCCC, 20, scale < 1 ? 1 / scale : 1);

        LwjglManager.INSTANCE.getNanoVGHelper().drawLine(vg, x + 224, y + 72, x + 1280, y + 72, 1, Colors.GRAY_700);
        LwjglManager.INSTANCE.getNanoVGHelper().drawLine(vg, x + 224, y, x + 222, y + 800, 1, Colors.GRAY_700);

        LwjglManager.INSTANCE.getNanoVGHelper().drawSvg(vg, SVGs.ONECONFIG, x + 19, y + 19, 42, 42);
        LwjglManager.INSTANCE.getNanoVGHelper().drawText(vg, "OneConfig", x + 69, y + 32, -1, 18f, Fonts.BOLD);        // added half line height to center text
        LwjglManager.INSTANCE.getNanoVGHelper().drawText(vg, "By Polyfrost", x + 69, y + 51, -1, 12f, Fonts.REGULAR);

        textInputField.draw(vg, x + 1020, y + 16, inputHandler);
        sideBar.draw(vg, x, y, inputHandler);
        backArrow.draw(vg, x + 240, y + 16, inputHandler);
        forwardArrow.draw(vg, x + 288, y + 16, inputHandler);

        if (previousPages.size() == 0) {
            backArrow.disable(true);
            LwjglManager.INSTANCE.getNanoVGHelper().setAlpha(vg, 0.5f);
        } else {
            backArrow.disable(false);
            if (!backArrow.isHovered() || Platform.getMousePlatform().isButtonDown(0))
                LwjglManager.INSTANCE.getNanoVGHelper().setAlpha(vg, 0.8f);
        }
        LwjglManager.INSTANCE.getNanoVGHelper().drawSvg(vg, SVGs.CARET_LEFT, x + 246, y + 22, 28, 28);
        LwjglManager.INSTANCE.getNanoVGHelper().setAlpha(vg, 1f);
        if (nextPages.size() == 0) {
            forwardArrow.disable(true);
            LwjglManager.INSTANCE.getNanoVGHelper().setAlpha(vg, 0.5f);
        } else {
            forwardArrow.disable(false);
            if (!forwardArrow.isHovered() || Platform.getMousePlatform().isButtonDown(0))
                LwjglManager.INSTANCE.getNanoVGHelper().setAlpha(vg, 0.8f);
        }
        LwjglManager.INSTANCE.getNanoVGHelper().drawSvg(vg, SVGs.CARET_RIGHT, x + 294, y + 22, 28, 28);
        LwjglManager.INSTANCE.getNanoVGHelper().setAlpha(vg, 1f);

        if (backArrow.isClicked() && previousPages.size() > 0) {
            try {
                nextPages.add(0, currentPage);
                openPage(previousPages.get(0), false);
                previousPages.remove(0);
            } catch (Exception ignored) {
            }
        } else if (forwardArrow.isClicked() && nextPages.size() > 0) {
            try {
                previousPages.add(0, currentPage);
                openPage(nextPages.get(0), new EaseInOutQuad(300, 224, 2128, true), false);
                nextPages.remove(0);
            } catch (Exception ignored) {
            }
        }

        LwjglManager.INSTANCE.getScissorHelper().scissor(vg, x + 224, y + 72, 1056, 728);
        Scissor blockedClicks = inputHandler.blockInputArea(x + 224, y, 1056, 72);
        if (prevPage != null && animation != null) {
            float pageProgress = animation.get(GuiUtils.getDeltaTime());
            if (!animation.isReversed()) {
                prevPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72, inputHandler);
                currentPage.scrollWithDraw(vg, (int) (x - 1904 + pageProgress), y + 72, inputHandler);
            } else {
                prevPage.scrollWithDraw(vg, (int) (x - 1904 + pageProgress), y + 72, inputHandler);
                currentPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72, inputHandler);
            }
            if (animation.isFinished()) {
                prevPage = null;
            }
        } else {
            currentPage.scrollWithDraw(vg, x + 224, y + 72, inputHandler);
        }
        LwjglManager.INSTANCE.getScissorHelper().clearScissors(vg);
        inputHandler.stopBlock(blockedClicks);

        float breadcrumbX = x + 352;
        for (int i = 0; i < currentPage.parents.size(); i++) {
            String title = currentPage.parents.get(i).getTitle();
            float width = LwjglManager.INSTANCE.getNanoVGHelper().getTextWidth(vg, title, 24f, Fonts.SEMIBOLD);
            boolean hovered = inputHandler.isAreaHovered((int) breadcrumbX, y + 24, (int) width, 36);
            int color = Colors.WHITE_60;
            if (i == currentPage.parents.size() - 1) color = Colors.WHITE;
            else if (hovered && !Platform.getMousePlatform().isButtonDown(0)) color = Colors.WHITE_80;
            LwjglManager.INSTANCE.getNanoVGHelper().drawText(vg, title, breadcrumbX, y + 38, color, 24f, Fonts.SEMIBOLD);
            if (i != 0)
                LwjglManager.INSTANCE.getNanoVGHelper().drawSvg(vg, SVGs.CARET_RIGHT, breadcrumbX - 28, y + 25, 24, 24, color);
            if (hovered && inputHandler.isClicked()) openPage(currentPage.parents.get(i));
            breadcrumbX += width + 32;
        }

        long end = System.nanoTime() - start;
        String s = (" draw: " + end / 1000000f + "ms");
        LwjglManager.INSTANCE.getNanoVGHelper().drawText(vg, s, x + 1170, y + 792, Colors.GRAY_300, 10f, Fonts.MEDIUM);
        if (currentColorSelector != null) {
            currentColorSelector.draw(vg);
        }
    }

    @Override
    public void onKeyPressed(int keyCode, char typedChar, @Nullable UKeyboard.Modifiers modifiers) {
        UKeyboard.allowRepeatEvents(true);
        try {
            if (allowClose) super.onKeyPressed(keyCode, typedChar, modifiers);
            textInputField.keyTyped(typedChar, keyCode);
            if (currentColorSelector != null) currentColorSelector.keyTyped(typedChar, keyCode);
            currentPage.keyTyped(typedChar, keyCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openPage(@NotNull Page page) {
        openPage(page, true);
    }

    public void openPage(@NotNull Page page, boolean addToPrevious) {
        openPage(page, new EaseInOutQuad(300, 224, 2128, false), addToPrevious);
    }

    public void openPage(@NotNull Page page, Animation animation, boolean addToPrevious) {
        if (page == currentPage) return;
        currentPage.finishUpAndClose();
        textInputField.setInput("");
        if (page.parents.size() == 0) {
            page.parents.addAll(currentPage.parents);
            if (!page.isBase()) {
                boolean alreadyInParents = false;
                for (int i = 0; i < page.parents.size(); i++) {
                    Page parent = page.parents.get(i);
                    if (parent == page) {
                        alreadyInParents = true;
                        page.parents.subList(i + 1, page.parents.size()).clear();
                        break;
                    }
                }
                if (!alreadyInParents) page.parents.add(page);
            } else {
                page.parents.clear();
                page.parents.add(page);
            }
        }
        sideBar.pageOpened(page.parents.get(0).getTitle());
        if (addToPrevious) {
            previousPages.add(0, currentPage);
            nextPages.clear();
        }
        if (prevPage == null) {
            prevPage = currentPage;
        }
        currentPage = page;
        this.animation = animation;
    }

    /**
     * initialize a new ColorSelector and add it to the draw script. This method is used to make sure it is always rendered on top.
     * <p>
     * Correct usage: <code>OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, inputUtils.mouseX(), inputUtils.mouseY()));</code>
     */
    public void initColorSelector(ColorSelector colorSelector) {
        if (currentColorSelector != null) closeColorSelector();
        currentColorSelector = colorSelector;
    }

    /**
     * Close the current color selector and return the color it had when it closed.
     */
    public OneColor closeColorSelector() {
        currentColorSelector.onClose();
        OneColor color = currentColorSelector.getColor();
        currentColorSelector = null;
        return color;
    }

    public OneColor getColor() {
        if (currentColorSelector == null) return null;
        return currentColorSelector.getColor();
    }

    public float getScaleFactor() {
        return scale;
    }

    public String getSearchValue() {
        return textInputField.getInput();
    }

    @Override
    public void onScreenClose() {
        currentPage.finishUpAndClose();
        super.onScreenClose();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public boolean hasBackgroundBlur() {
        return Preferences.enableBlur;
    }
}
