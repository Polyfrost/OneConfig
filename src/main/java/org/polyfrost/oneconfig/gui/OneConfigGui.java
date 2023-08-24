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

package org.polyfrost.oneconfig.gui;

import org.polyfrost.oneconfig.config.core.OneColor;
import org.polyfrost.oneconfig.config.elements.BasicOption;
import org.polyfrost.oneconfig.config.elements.OptionPage;
import org.polyfrost.oneconfig.config.elements.OptionSubcategory;
import org.polyfrost.oneconfig.events.EventManager;
import org.polyfrost.oneconfig.events.event.RenderEvent;
import org.polyfrost.oneconfig.events.event.Stage;
import org.polyfrost.oneconfig.gui.animations.Animation;
import org.polyfrost.oneconfig.gui.animations.DummyAnimation;
import org.polyfrost.oneconfig.gui.animations.EaseInBack;
import org.polyfrost.oneconfig.gui.animations.EaseOutExpo;
import org.polyfrost.oneconfig.gui.elements.BasicElement;
import org.polyfrost.oneconfig.gui.elements.ColorSelector;
import org.polyfrost.oneconfig.gui.elements.IFocusable;
import org.polyfrost.oneconfig.gui.elements.text.TextInputField;
import org.polyfrost.oneconfig.gui.pages.ModConfigPage;
import org.polyfrost.oneconfig.gui.pages.ModsPage;
import org.polyfrost.oneconfig.gui.pages.Page;
import org.polyfrost.oneconfig.internal.assets.Colors;
import org.polyfrost.oneconfig.internal.assets.SVGs;
import org.polyfrost.oneconfig.internal.config.OneConfigConfig;
import org.polyfrost.oneconfig.internal.config.Preferences;
import org.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl;
import org.polyfrost.oneconfig.libs.eventbus.Subscribe;
import org.polyfrost.oneconfig.libs.universal.UKeyboard;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.platform.Platform;
import org.polyfrost.oneconfig.renderer.NanoVGHelper;
import org.polyfrost.oneconfig.renderer.font.Fonts;
import org.polyfrost.oneconfig.renderer.scissor.Scissor;
import org.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import org.polyfrost.oneconfig.utils.InputHandler;
import org.polyfrost.oneconfig.utils.MathUtils;
import org.polyfrost.oneconfig.utils.color.ColorPalette;
import org.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.polyfrost.oneconfig.utils.gui.OneUIScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OneConfigGui extends OneUIScreen {
    private static final InputHandler DUMMY_HANDLER = new InputHandler();
    public static OneConfigGui INSTANCE;

    private final SideBar sideBar = new SideBar();
    private final TextInputField textInputField = new TextInputField(248, 40, "Search...", false, false, SVGs.SEARCH_SM);
    private final List<Page> previousPages = new ArrayList<>();
    private final List<Page> nextPages = new ArrayList<>();
    private final BasicElement backArrow = new BasicElement(40, 40, ColorPalette.TERTIARY, true);
    private final BasicElement forwardArrow = new BasicElement(40, 40, ColorPalette.TERTIARY, true);
    public ColorSelector currentColorSelector;
    public boolean allowClose = true;
    protected Page currentPage;
    protected Page prevPage;
    private Animation pageAnimation;

    private long lastClosedTime = -1;

    private Animation containerAnimation = new DummyAnimation(0);
    public boolean isClosed = true;
    private boolean shouldDisplayHud = false;
    public float transparencyFactor = 0f;
    public float animationScaleFactor = 0f;
    /**
     * Used for global transparency animation in {@link NanoVGHelperImpl#setAlpha(long, float)}
     */
    private boolean isDrawing;

    public OneConfigGui() {
        if (INSTANCE != null)
            EventManager.INSTANCE.unregister(INSTANCE);
        INSTANCE = this;

        EventManager.INSTANCE.register(INSTANCE);
    }

    public OneConfigGui(Page page) {
        this();
        this.currentPage = page;
    }

    @Override
    public void initScreen(int width, int height) {
        super.initScreen(width, height);
        if (currentPage == null) {
            currentPage = new ModsPage();
            currentPage.parents.add(currentPage);
        }

        handleOpeningPage();

        if (Preferences.guiOpenAnimation) {
            shouldDisplayHud = false;
        }
    }

    private void handleOpeningPage() {
        boolean instant = !Preferences.showPageAnimationOnOpen;
        switch (Preferences.openingBehavior) {
            case 0:
                if (currentPage instanceof ModsPage) {
                    break;
                }
                previousPages.clear();
                openPage(new ModsPage(), false, instant);
                break;
            case 1:
                OptionPage preferencesPage = Preferences.getInstance().mod.defaultPage;
                if (currentPage instanceof ModConfigPage) {
                    ModConfigPage modConfigPage = (ModConfigPage) currentPage;
                    if (modConfigPage.getPage() == preferencesPage) {
                        break;
                    }
                }
                previousPages.clear();
                openPage(
                        new ModConfigPage(
                                Preferences.getInstance().mod.defaultPage,
                                true
                        ),
                        false,
                        instant
                );
                break;
            case 2:
                break;
            case 3:
                if (currentPage instanceof ModsPage) {
                    break;
                }

                long current = System.currentTimeMillis();
                long diff = current - lastClosedTime;
                if (lastClosedTime == -1)
                    break;
                if (diff <= Preferences.timeUntilReset * 1000L) {
                    break;
                }
                previousPages.clear();
                openPage(new ModsPage(), false, instant);
                break;
        }
    }

    @Override
    public void draw(long vg, float partialTicks, InputHandler inputHandler) {
        this.isDrawing = true;

        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        final ScissorHelper scissorHelper = ScissorHelper.INSTANCE;

        boolean renderedInHud = (inputHandler == null);
        if (Preferences.guiOpenAnimation) {
            int animationTime = (int) (Preferences.animationTime * 1000);
            if (renderedInHud && Preferences.guiClosingAnimation && shouldDisplayHud) {
                if (containerAnimation.getEnd() != 0) {
                    switch (Preferences.animationType) {
                        case 0:
                            containerAnimation = new EaseOutExpo(animationTime, MathUtils.clamp(animationScaleFactor - 0.9f, 0f, 0.1f), 0, false);
                            break;
                        case 1:
                            containerAnimation = new EaseInBack(animationTime, MathUtils.clamp(animationScaleFactor, 0f, 1f), 0, false);
                            break;
                    }
                }
            } else if (!renderedInHud && isClosed) {
                // If we are switching animations, aka if the previous one is already finished
                boolean forceFinished = containerAnimation.isFinished() && containerAnimation.getEnd() != 0;
                switch (Preferences.animationType) {
                    case 0:
                        containerAnimation = new EaseOutExpo(animationTime, MathUtils.clamp(forceFinished ? 0.1f : (animationScaleFactor - 0.9f), 0, 0.1f), 0.1f, false);
                        break;
                    case 1:
                        containerAnimation = new EaseOutExpo(animationTime, MathUtils.clamp(forceFinished ? 1 : animationScaleFactor, 0, 1), 1, false);
                        break;
                }
                isClosed = false;
            }
        }

        float animationValue = Math.max(0, Preferences.guiOpenAnimation ? containerAnimation.get() : 1);
        switch (Preferences.animationType) {
            case 0:
                animationScaleFactor = MathUtils.clamp(.9f + animationValue, .9f, 1f);
                transparencyFactor = MathUtils.clamp(animationValue * 10f, 0, 1);
                break;
            case 1:
                animationScaleFactor = transparencyFactor = animationValue;
                break;
        }

        nanoVGHelper.setAlpha(vg, transparencyFactor);

        if (OneConfigConfig.australia) {
            nanoVGHelper.translate(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight());
            nanoVGHelper.rotate(vg, (float) Math.toRadians(180));
        }

        if (inputHandler == null) {
            inputHandler = DUMMY_HANDLER;
        }

        float scale = getScaleFactor() * animationScaleFactor;
        int x = (int) ((UResolution.getWindowWidth() - 1280 * scale) / 2f / scale);
        int y = (int) ((UResolution.getWindowHeight() - 800 * scale) / 2f / scale);
        nanoVGHelper.scale(vg, scale, scale);
        inputHandler.scale(scale, scale);

        nanoVGHelper.drawDropShadow(vg, x, y, 1280, 800, 64, 0, 20);

        Scissor mainPanel = scissorHelper.scissor(vg, x, y, 224, 800);
        nanoVGHelper.drawRoundedRect(vg, x, y, 244, 800, Colors.GRAY_800_95, 20f);
        scissorHelper.resetScissor(vg, mainPanel);

        Scissor contentPanel = scissorHelper.scissor(vg, x + 224, y, 1056, 800);
        nanoVGHelper.drawRoundedRect(vg, x + 224 - 20, y, 1056 + 20, 800, Colors.GRAY_800, 20f);
        scissorHelper.resetScissor(vg, contentPanel);

        nanoVGHelper.drawLine(vg, x + 224, y + 72, x + 1280, y + 72, 1, Colors.GRAY_700);
        nanoVGHelper.drawLine(vg, x + 224, y, x + 222, y + 800, 1, Colors.GRAY_700);

        nanoVGHelper.drawSvg(vg, SVGs.ONECONFIG_FULL_DARK, x + 33f, y + 22f, 158f, 34f);

        textInputField.draw(vg, x + 1020, y + 16, inputHandler);
        sideBar.draw(vg, x, y, inputHandler);
        backArrow.update(x + 240, y + 16, inputHandler);
        forwardArrow.update(x + 280, y + 16, inputHandler);

        if (previousPages.size() == 0) {
            backArrow.disable(true);
            nanoVGHelper.setAlpha(vg, 0.5f);
        } else {
            backArrow.disable(false);
            if (!backArrow.isHovered() || Platform.getMousePlatform().isButtonDown(0))
                nanoVGHelper.setAlpha(vg, 0.8f);
        }
        nanoVGHelper.drawSvg(vg, SVGs.ARROW_LEFT, x + 250, y + 26, 20, 20, backArrow.currentColor);
        nanoVGHelper.setAlpha(vg, 1f);
        if (nextPages.size() == 0) {
            forwardArrow.disable(true);
            nanoVGHelper.setAlpha(vg, 0.5f);
        } else {
            forwardArrow.disable(false);
            if (!forwardArrow.isHovered() || Platform.getMousePlatform().isButtonDown(0))
                nanoVGHelper.setAlpha(vg, 0.8f);
        }
        nanoVGHelper.drawSvg(vg, SVGs.ARROW_RIGHT, x + 290, y + 26, 20, 20, forwardArrow.currentColor);
        nanoVGHelper.setAlpha(vg, 1f);

        handleHistoryMovement(backArrow.isClicked(), forwardArrow.isClicked());

        scissorHelper.scissor(vg, x + 224, y + 72, 1056, 728);
        Scissor blockedClicks = inputHandler.blockInputArea(x + 224, y, 1056, 72);
        if (prevPage != null && pageAnimation != null) {
            float pageProgress = pageAnimation.get(GuiUtils.getDeltaTime());
            if (!pageAnimation.isReversed()) {
                prevPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72, inputHandler);
                currentPage.scrollWithDraw(vg, (int) (x - 1904 + pageProgress), y + 72, inputHandler);
            } else {
                prevPage.scrollWithDraw(vg, (int) (x - 1904 + pageProgress), y + 72, inputHandler);
                currentPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72, inputHandler);
            }
            if (pageAnimation.isFinished()) {
                prevPage = null;
            }
        } else {
            currentPage.scrollWithDraw(vg, x + 224, y + 72, inputHandler);
        }
        scissorHelper.clearScissors(vg);
        inputHandler.stopBlock(blockedClicks);

        float breadcrumbX = x + 336;
        for (int i = 0; i < currentPage.parents.size(); i++) {
            String title = currentPage.parents.get(i).getTitle();
            float width = nanoVGHelper.getTextWidth(vg, title, 24f, Fonts.SEMIBOLD);
            boolean hovered = inputHandler.isAreaHovered((int) breadcrumbX, y + 24, (int) width, 36);
            int color = Colors.WHITE_60;
            if (i == currentPage.parents.size() - 1) color = Colors.WHITE;
            else if (hovered && !Platform.getMousePlatform().isButtonDown(0)) color = Colors.WHITE_80;
            nanoVGHelper.drawText(vg, title, breadcrumbX, y + 38, color, 24f, Fonts.SEMIBOLD);
            if (i != 0)
                nanoVGHelper.drawSvg(vg, SVGs.CARET_RIGHT, breadcrumbX - 28, y + 25, 24, 24, color);
            if (hovered && inputHandler.isClicked()) openPage(currentPage.parents.get(i));
            breadcrumbX += width + 32;
        }

        if (currentColorSelector != null) {
            currentColorSelector.draw(vg);
        }
        GuiNotifications.INSTANCE.draw(vg, x + 224 + ((1280 - 224) / 2), y + 720 + 72, inputHandler);
        nanoVGHelper.resetTransform(vg);
        isDrawing = false;
    }

    @Override
    public void onKeyPressed(int keyCode, char typedChar, @Nullable UKeyboard.Modifiers modifiers) {
        UKeyboard.allowRepeatEvents(true);
        try {
            if (allowClose) super.onKeyPressed(keyCode, typedChar, modifiers);
            textInputField.keyTyped(typedChar, keyCode);
            if (currentColorSelector != null) currentColorSelector.keyTyped(typedChar, keyCode);
            currentPage.keyTyped(typedChar, keyCode);

            // Don't handle inputs any further if a config element is focused
            if (textInputField.isToggled()) return;
            if (currentPage instanceof ModConfigPage) {
                ModConfigPage modConfigPage = ((ModConfigPage) currentPage);
                for (OptionSubcategory subCategory : modConfigPage.getPage().categories.get(modConfigPage.getSelectedCategory()).subcategories) {
                    for (BasicOption option : subCategory.options) {
                        if (option.isEnabled()) {
                            if (option instanceof IFocusable) {
                                if (((IFocusable) option).hasFocus()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            handleHistoryMovement(
                    keyCode == UKeyboard.KEY_LEFT,
                    keyCode == UKeyboard.KEY_RIGHT
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleHistoryMovement(boolean back, boolean forward) {
        if (back && forward) return;

        try {
            if (back && previousPages.size() > 0) {
                nextPages.add(0, currentPage);
                openPage(previousPages.get(0), false);
                previousPages.remove(0);
            }
            if (forward && nextPages.size() > 0) {
                previousPages.add(0, currentPage);
                openPage(nextPages.get(0), new EaseOutExpo((int) Preferences.pageAnimationDuration * 1000, 224, 2128, true), false);
                nextPages.remove(0);
            }
        } catch (Throwable ignored) {
        }
    }

    public void openPage(@NotNull Page page) {
        openPage(page, true);
    }

    public void openPage(@NotNull Page page, boolean addToPrevious) {
        openPage(page, new EaseOutExpo((int) (Preferences.pageAnimationDuration * 1000), 224, 2128, false), addToPrevious);
    }

    private void openPage(@NotNull Page page, boolean addToPrevious, boolean instant) {
        if (instant) {
            openPageInstant(page, addToPrevious);
        } else {
            openPage(page, addToPrevious);
        }
    }

    private void openPageInstant(@NotNull Page page, boolean addToPrevious) {
        openPage(page, new DummyAnimation(2128), addToPrevious);
    }

    public void openPage(@NotNull Page page, Animation animation, boolean addToPrevious) {
        if (!Preferences.showPageAnimations) {
            animation = new DummyAnimation(animation.getEnd());
        }

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
        sideBar.pageOpened(page.parents.get(0).getTitle(), animation instanceof DummyAnimation);
        if (addToPrevious) {
            previousPages.add(0, currentPage);
            nextPages.clear();
        }
        if (prevPage == null) {
            prevPage = currentPage;
        }
        currentPage = page;
        this.pageAnimation = animation;
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
    @SuppressWarnings("UnusedReturnValue")
    public OneColor closeColorSelector() {
        if (currentColorSelector == null) return null;
        currentColorSelector.onClose();
        OneColor color = currentColorSelector.getColor();
        currentColorSelector = null;
        return color;
    }

    public OneColor getColor() {
        if (currentColorSelector == null) return null;
        return currentColorSelector.getColor();
    }

    public static float getScaleFactor() {
        float scale = Preferences.enableCustomScale ? Preferences.customScale : Math.min(UResolution.getWindowWidth() / 1920f, UResolution.getWindowHeight() / 1080f);
        if (scale < 1 && !Preferences.enableCustomScale)
            scale = Math.min(Math.min(1f, UResolution.getWindowWidth() / 1280f), Math.min(1f, UResolution.getWindowHeight() / 800f));
        return (float) (Math.floor(scale / 0.05f) * 0.05f);
    }

    public String getSearchValue() {
        return textInputField.getInput();
    }

    @Override
    public void onScreenClose() {
        currentPage.finishUpAndClose();

        lastClosedTime = System.currentTimeMillis();

        isClosed = true;
        if (Preferences.guiOpenAnimation) {
            if (Preferences.guiClosingAnimation) {
                shouldDisplayHud = true;
            } else {
                animationScaleFactor = transparencyFactor = 0;
            }
        }

        super.onScreenClose();
    }

    @Subscribe
    private void onRenderHUD(RenderEvent event) {
        if (!shouldDisplayHud || event.stage == Stage.START) return;
        if (Platform.getGuiPlatform().getCurrentScreen() == this) return;

        NanoVGHelper.INSTANCE.setupAndDraw(vg -> draw(vg, event.deltaTicks, null));

        if (transparencyFactor <= 0.01) {
            shouldDisplayHud = false;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public boolean hasBackgroundBlur() {
        return Preferences.enableBlur;
    }

    public boolean isDrawing() {
        return isDrawing;
    }

    public static OneConfigGui create() {
        return INSTANCE == null ? new OneConfigGui() : INSTANCE;
    }

    public static boolean isOpen() {
        return Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui;
    }

    static {
        DUMMY_HANDLER.blockAllInput();
    }
}
