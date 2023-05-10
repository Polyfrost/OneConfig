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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutQuad;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import kotlin.collections.ArraysKt;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigDropdown extends BasicOption {
    private final String[] options;
    private final ColorAnimation backgroundColor = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation atomColor = new ColorAnimation(new ColorPalette(Colors.PRIMARY_600, Colors.PRIMARY_500, Colors.PRIMARY_500));
    private final ColorAnimation colorAnimation = new ColorAnimation(new ColorPalette(Colors.GRAY_400_80, Colors.GRAY_400, Colors.GRAY_400), 200);
    private final TextInputField textInputField;
    private boolean opened = false;
    private Scissor inputScissor = null;
    private Animation scrollAnimation = null;
    private float scrollTarget = 0;
    private long scrollTime = 0;
    private boolean mouseWasDown = false;
    private boolean dragging = false;
    private float yStart = 0;
    private float scroll = 0;
    private float maxHeight = 0;
    private InputHandler inputHandler;

    public ConfigDropdown(Field field, Object parent, String name, String description, String category, String subcategory, int size, String[] options) {
        super(field, parent, name, description, category, subcategory, size);
        this.options = options;
        this.textInputField = new TextInputField((size == 1) ? 256 : 640, 32, "Search...", false, false, SVGs.SEARCH_SM);
    }

    public static ConfigDropdown create(Field field, Object parent) {
        Dropdown dropdown = field.getAnnotation(Dropdown.class);
        return new ConfigDropdown(field, parent, dropdown.name(), dropdown.description(), dropdown.category(), dropdown.subcategory(), dropdown.size(), dropdown.options());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        nanoVGHelper.drawText(vg, name, x, y + 16, nameColor, 14f, Fonts.MEDIUM);

        boolean hovered;
        if (size == 1) hovered = inputHandler.isAreaHovered(x + 224, y, 256, 32) && isEnabled();
        else hovered = inputHandler.isAreaHovered(x + 352, y, 640, 32) && isEnabled();

        if (hovered && inputHandler.isClicked() || opened && inputHandler.isClicked(!dragging) &&
                (size == 1 && !inputHandler.isAreaHovered(x + 224, y + 40, 256, options.length * 32, true) ||
                        size == 2 && !inputHandler.isAreaHovered(x + 352, y + 40, 640, options.length * 32, true))) {
            opened = !opened;
            if (!opened) {
                inputHandler.unblockDWheel();
                inputHandler.stopBlockingInput();
                if (inputScissor != null) inputHandler.stopBlock(inputScissor);
            } else {
                inputScissor = inputHandler.blockAllInput();
                textInputField.onClick();
            }
            backgroundColor.setPalette(opened ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
        }
        if (opened) return;

        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Platform.getMousePlatform().isButtonDown(0)) nanoVGHelper.setAlpha(vg, 0.8f);
        if (size == 1) {
            nanoVGHelper.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12);
            nanoVGHelper.drawText(vg, options[selected], x + 236, y + 16, Colors.WHITE_80, 14f, Fonts.MEDIUM);
            nanoVGHelper.drawRoundedRect(vg, x + 452, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
            nanoVGHelper.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 452, y + 4, 24, 24);
        } else {
            nanoVGHelper.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12);
            nanoVGHelper.drawText(vg, options[selected], x + 364, y + 16, Colors.WHITE_80, 14f, Fonts.MEDIUM);
            nanoVGHelper.drawRoundedRect(vg, x + 964, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
            nanoVGHelper.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 964, y + 4, 24, 24);
        }
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public void drawLast(long vg, int x, int y, InputHandler inputHandler) {
        super.drawLast(vg, x, y, inputHandler);
        if (!opened) {
            finishUpAndClose(false);
            return;
        }
        if (dragging) {
            inputHandler.blockAllInput();
        }
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        boolean hovered;
        if (size == 1) hovered = inputHandler.isAreaHovered(x + 224, y, 256, 32, true);
        else hovered = inputHandler.isAreaHovered(x + 352, y, 640, 32, true);

        //TODO: cache this on typing
        List<String> options = new ArrayList<>();
        for (String option : this.options) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(textInputField.getInput().toLowerCase(Locale.ENGLISH))) {
                options.add(option);
            }
        }

        textInputField.setErrored(options.isEmpty());
        if (hovered && Platform.getMousePlatform().isButtonDown(0)) nanoVGHelper.setAlpha(vg, 0.8f);

        if (size == 1) {
            textInputField.draw(vg, x + 224, y, inputHandler);
        } else {
            textInputField.draw(vg, x + 352, y, inputHandler);
        }
        if (options.isEmpty()) {
            return;
        }

        if (size == 1) {
            nanoVGHelper.setAlpha(vg, 1f);
            nanoVGHelper.drawRoundedRect(vg, x + 224, y + 40, 256, Math.min(options.size(), 10) * 32 + 8, Colors.GRAY_700, 12);
            nanoVGHelper.drawHollowRoundRect(vg, x + 223, y + 39, 258, Math.min(options.size(), 10) * 32 + 10, new Color(204, 204, 204, 77).getRGB(), 12, 1);
            ScissorHelper scissorHelper = ScissorHelper.INSTANCE;
            Scissor scissor = null;
            if (options.size() > 10) {
                scissor = scissorHelper.scissor(vg, x + 224, y + 40, 256, 328);
            }
            inputHandler.unblockDWheel();
            scroll = scrollAnimation == null ? scrollTarget : scrollAnimation.get();
            maxHeight = options.size() * 32 + 8;
            float scrollWidth = Math.min(options.size(), 10) * 32 + 8;
            final float scrollBarLength = (scrollWidth / maxHeight) * scrollWidth;
            float dWheel = (float) inputHandler.getDWheel(true);
            if (dWheel != 0) {
                scrollTarget += dWheel * 0.25;

                if (scrollTarget > 0f) scrollTarget = 0f;
                else if (scrollTarget < -maxHeight + scrollWidth) scrollTarget = -maxHeight + scrollWidth;

                scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
                scrollTime = System.currentTimeMillis();
            } else if (scrollAnimation != null && scrollAnimation.isFinished()) scrollAnimation = null;

            float optionY = y + 44 + scroll;
            for (String option : options) {
                int color = Colors.WHITE_80;
                boolean optionHovered = inputHandler.isAreaHovered(x + 224, optionY, 252, 32, true);
                if (optionHovered && Platform.getMousePlatform().isButtonDown(0)) {
                    nanoVGHelper.drawRoundedRect(vg, x + 228, optionY + 2f, 248, 28, Colors.PRIMARY_700_80, 8);
                } else if (optionHovered) {
                    nanoVGHelper.drawRoundedRect(vg, x + 228, optionY + 2f, 248, 28, Colors.PRIMARY_700, 8);
                    color = Colors.WHITE;
                }
                if (optionHovered && inputHandler.isClicked(!dragging)) {
                    try {
                        set(ArraysKt.indexOf(this.options, option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                    backgroundColor.setPalette(ColorPalette.SECONDARY);
                    inputHandler.unblockDWheel();
                    if (inputScissor != null && !dragging) inputHandler.stopBlock(inputScissor);
                }

                nanoVGHelper.drawText(vg, option, x + 240, optionY + 18f, color, 14, Fonts.MEDIUM);
                optionY += 32;
            }

            if (scissor != null) scissorHelper.resetScissor(vg, scissor);

            if (dragging && inputHandler.isClicked(true)) {
                dragging = false;
                inputHandler.stopBlockingInput();
            }
            if (!(scrollBarLength >= scrollWidth)) {
                final float scrollBarY = (scroll / maxHeight) * (scrollWidth - 8) - 45;
                final boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
                final boolean scrollHover = inputHandler.isAreaHovered(x + 476, y - scrollBarY, 12, (int) scrollBarLength - 5, true);
                final boolean scrollTimePeriod = (System.currentTimeMillis() - scrollTime < 1000);
                if (scrollHover && isMouseDown && !mouseWasDown) {
                    yStart = inputHandler.mouseY();
                    dragging = true;
                }
                mouseWasDown = isMouseDown;
                if (dragging) {
                    scrollTarget = -(inputHandler.mouseY() - yStart) * maxHeight / scrollWidth;
                    if (scrollTarget > 0f) scrollTarget = 0f;
                    else if (scrollTarget < -maxHeight + scrollWidth) scrollTarget = -maxHeight + scrollWidth;
                    scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
                }
                NanoVGHelper.INSTANCE.drawRoundedRect(vg, x + 476, y - scrollBarY, 4, scrollBarLength - 5, colorAnimation.getColor(scrollHover || scrollTimePeriod, dragging), 4f);
            }
            if (inputHandler.isAreaHovered(x + 224, y + 40, 256, Math.min(options.size(), 10) * 32 + 8, true) && opened) {
                inputHandler.blockDWheel();
            }
        } else {

            nanoVGHelper.setAlpha(vg, 1f);
            nanoVGHelper.drawRoundedRect(vg, x + 352, y + 40, 640, Math.min(options.size(), 10) * 32 + 8, Colors.GRAY_700, 12);
            nanoVGHelper.drawHollowRoundRect(vg, x + 351, y + 39, 642, Math.min(options.size(), 10) * 32 + 10, new Color(204, 204, 204, 77).getRGB(), 12, 1);
            ScissorHelper scissorHelper = ScissorHelper.INSTANCE;
            Scissor scissor = null;
            if (options.size() > 10) {
                scissor = scissorHelper.scissor(vg, x + 352, y + 40, 640, 328);
            }
            inputHandler.unblockDWheel();
            scroll = scrollAnimation == null ? scrollTarget : scrollAnimation.get();
            maxHeight = options.size() * 32 + 8;
            float scrollWidth = Math.min(options.size(), 10) * 32 + 8;
            final float scrollBarLength = (scrollWidth / maxHeight) * scrollWidth;
            float dWheel = (float) inputHandler.getDWheel(true);
            if (dWheel != 0) {
                scrollTarget += dWheel * 0.25;

                if (scrollTarget > 0f) scrollTarget = 0f;
                else if (scrollTarget < -maxHeight + scrollWidth) scrollTarget = -maxHeight + scrollWidth;

                scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
                scrollTime = System.currentTimeMillis();
            } else if (scrollAnimation != null && scrollAnimation.isFinished()) scrollAnimation = null;

            float optionY = y + 44 + scroll;
            for (String option : options) {
                int color = Colors.WHITE_80;
                boolean optionHovered = inputHandler.isAreaHovered(x + 352, optionY, 640, 36, true);
                if (optionHovered && Platform.getMousePlatform().isButtonDown(0)) {
                    nanoVGHelper.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, Colors.PRIMARY_700_80, 8);
                } else if (optionHovered) {
                    nanoVGHelper.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, Colors.PRIMARY_700, 8);
                    color = Colors.WHITE;
                }

                nanoVGHelper.drawText(vg, option, x + 368, optionY + 18, color, 14, Fonts.MEDIUM);

                if (optionHovered && inputHandler.isClicked(!dragging)) {
                    try {
                        set(ArraysKt.indexOf(this.options, option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                    backgroundColor.setPalette(ColorPalette.SECONDARY);
                    inputHandler.unblockDWheel();
                    if (inputScissor != null && !dragging) inputHandler.stopBlock(inputScissor);
                }
                optionY += 32;
            }


            if (scissor != null) scissorHelper.resetScissor(vg, scissor);

            if (dragging && inputHandler.isClicked(true)) {
                dragging = false;
                inputHandler.stopBlockingInput();
            }
            if (!(scrollBarLength >= scrollWidth)) {
                final float scrollBarY = (scroll / maxHeight) * (scrollWidth - 8) - 45;
                final boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
                final boolean scrollHover = inputHandler.isAreaHovered(x + 988, y - scrollBarY, 12, (int) scrollBarLength - 5, true);
                final boolean scrollTimePeriod = (System.currentTimeMillis() - scrollTime < 1000);
                if (scrollHover && isMouseDown && !mouseWasDown) {
                    yStart = inputHandler.mouseY();
                    dragging = true;
                }
                mouseWasDown = isMouseDown;
                if (dragging) {
                    scrollTarget = -(inputHandler.mouseY() - yStart) * maxHeight / scrollWidth;
                    if (scrollTarget > 0f) scrollTarget = 0f;
                    else if (scrollTarget < -maxHeight + scrollWidth) scrollTarget = -maxHeight + scrollWidth;
                    scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
                }
                NanoVGHelper.INSTANCE.drawRoundedRect(vg, x + 988, y - scrollBarY, 4, scrollBarLength - 5, colorAnimation.getColor(scrollHover || scrollTimePeriod, dragging), 4f);
            }
            if (inputHandler.isAreaHovered(x + 352, y + 40, 640, Math.min(options.size(), 10) * 32 + 8, true) && opened) {
                inputHandler.blockDWheel();
            }
        }
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        super.keyTyped(key, keyCode);
        textInputField.keyTyped(key, keyCode);
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    protected boolean shouldDrawDescription() {
        return super.shouldDrawDescription() && !opened;
    }

    @Override
    public void finishUpAndClose() {
        super.finishUpAndClose();
        finishUpAndClose(true);
    }

    private void finishUpAndClose(boolean unblock) {
        super.finishUpAndClose();
        textInputField.setInput("");
        scroll = 0;
        scrollTarget = 0;
        scrollTime = 0;
        scrollAnimation = null;
        if (unblock) {
            inputHandler.unblockDWheel();
            inputHandler.stopBlockingInput();
        }
    }
}
