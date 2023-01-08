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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutCubic;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuart;
import cc.polyfrost.oneconfig.gui.animations.EaseOutExpo;
import cc.polyfrost.oneconfig.gui.elements.IFocusable;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.MathUtils;

import java.lang.reflect.Field;

public class ConfigSlider extends BasicOption implements IFocusable {
    private static final int STEP_POPUP_DURATION = 400;
    private static final int INDICATOR_POPUP_DURATION = 200;
    private static final int INDICATOR_SLIDING_DURATION = 60;

    private static final float STEP_HEIGHT_TOTAL = 16;
    private static final float STEP_HEIGHT_HOVER = 10;
    private static final float STEP_HEIGHT_DRAG = 16;
    private static final float TOUCH_TARGET_TOTAL = 16;
    private static final float TOUCH_TARGET_HOVER = 16;
    private static final float TOUCH_TARGET_DRAG = 10;

    private final NumberInputField inputField;
    private final float min, max;
    private final int step;
    private boolean isFloat = true;
    private boolean dragging = false;
    private boolean mouseWasDown = false;
    private Animation stepsAnimation;
    private Animation targetAnimation;
    private Animation stepSlideAnimation;
    private boolean animReset;
    private float lastX = -1;

    public ConfigSlider(Field field, Object parent, String name, String description, String category, String subcategory, float min, float max, int step) {
        super(field, parent, name, description, category, subcategory, 2);
        this.min = min;
        this.max = max;
        this.step = step;
        this.inputField = new NumberInputField(84, 32, 0, min, max, step == 0 ? 1 : step);
        this.stepsAnimation = new DummyAnimation(0);
        this.targetAnimation = new DummyAnimation(0);
        this.stepSlideAnimation = new DummyAnimation(-1);
    }

    public static ConfigSlider create(Field field, Object parent) {
        Slider slider = field.getAnnotation(Slider.class);
        return new ConfigSlider(field, parent, slider.name(), slider.description(), slider.category(), slider.subcategory(), slider.min(), slider.max(), slider.step());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        int xCoordinate = 0;
        float value = 0;
        boolean hovered = inputHandler.isAreaHovered(x + 352, y, 512, 32) && isEnabled();

        inputField.disable(!isEnabled());
        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);

        boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        boolean startedDragging = !mouseWasDown && isMouseDown;
        mouseWasDown = isMouseDown;
        if (dragging) {
            xCoordinate = (int) MathUtils.clamp(inputHandler.mouseX(), x + 352, x + 864);
            if (step > 0) xCoordinate = getStepCoordinate(xCoordinate, x);
            value = MathUtils.map(xCoordinate, x + 352, x + 864, min, max);
        } else if (inputField.isToggled() || inputField.arrowsClicked()) {
            value = inputField.getCurrentValue();
            xCoordinate = (int) MathUtils.clamp(MathUtils.map(value, min, max, x + 352, x + 864), x + 352, x + 864);
        }
        if (dragging && inputHandler.isClicked() || inputField.isToggled() || inputField.arrowsClicked()) {
            dragging = false;
            if (step > 0) {
                xCoordinate = getStepCoordinate(xCoordinate, x);
                value = MathUtils.map(xCoordinate, x + 352, x + 864, min, max);
            }
            setValue(value);
        }

        float stepPercent = stepsAnimation.get();
        float targetPercent = targetAnimation.get();
        if (isEnabled()) {
            if (dragging && startedDragging) {
                stepsAnimation = new EaseOutExpo(STEP_POPUP_DURATION, stepPercent, 1, false);
                targetAnimation = new EaseOutExpo(INDICATOR_POPUP_DURATION, targetPercent, TOUCH_TARGET_DRAG / TOUCH_TARGET_TOTAL, false);
                animReset = true;
            } else if (!dragging && hovered) {
                if (targetAnimation.getEnd() != 1) {
                    stepsAnimation = new EaseOutExpo(STEP_POPUP_DURATION, stepPercent, STEP_HEIGHT_HOVER / STEP_HEIGHT_TOTAL, false);
                    targetAnimation = new EaseInOutQuart(INDICATOR_POPUP_DURATION, targetPercent, 1, false);
                    animReset = true;
                }
            } else if (!dragging && animReset) {
                stepsAnimation = new EaseOutExpo(STEP_POPUP_DURATION, stepPercent, 0, false);
                targetAnimation = new EaseOutExpo(INDICATOR_POPUP_DURATION, targetPercent, 0, false);
                animReset = false;
            }
        }

        if (!dragging && !inputField.isToggled()) {
            try {
                Object object = get();
                if (object instanceof Integer)
                    isFloat = false;
                if (isFloat) value = (float) object;
                else value = (int) object;
                xCoordinate = (int) MathUtils.clamp(MathUtils.map(value, min, max, x + 352, x + 864), x + 352, x + 864);
            } catch (IllegalAccessException ignored) {
            }
        }
        if (!inputField.isToggled()) {
            inputField.setCurrentValue(value);
        }

        // Animate sliding
        if (stepSlideAnimation.get() == -1 || lastX != x) {
            stepSlideAnimation = new DummyAnimation(xCoordinate);
        } else {
            stepSlideAnimation = new EaseInOutCubic(INDICATOR_SLIDING_DURATION, stepSlideAnimation.get(), xCoordinate, false);
        }
        xCoordinate = (int) stepSlideAnimation.get();

        lastX = x;

        // Ease-out the radius when the steps are in view
        float radius = 4;
        if (step > 0) {
            radius *= 1 - (Math.min(stepPercent, STEP_HEIGHT_HOVER / STEP_HEIGHT_TOTAL) * STEP_HEIGHT_TOTAL / STEP_HEIGHT_HOVER);
        }

        nanoVGHelper.drawText(vg, name, x, y + 17, nameColor, 14f, Fonts.MEDIUM);
        nanoVGHelper.drawRoundedRect(vg, x + 352, y + 13, 512, 4, Colors.GRAY_300, radius);
        nanoVGHelper.drawRoundedRect(vg, x + 352, y + 13 - 1, xCoordinate - x - 352, 6, Colors.PRIMARY_500, 4f);

        if (step > 0 && stepPercent > 0.05f) {
            float stepOffset = stepPercent * 16;
            for (float i = x + 354; i <= x + 864; i += 512 / ((max - min) / step)) {
                int color = xCoordinate > i - 2 ? Colors.PRIMARY_500 : Colors.GRAY_300;
                nanoVGHelper.drawRoundedRect(vg, i - 2, y + 16 - 1 - (stepOffset / 2f), 4, stepOffset, color, 2f);
            }
        }

        nanoVGHelper.drawRoundedRect(vg, xCoordinate - 12, y + 4, 24, 24, Colors.WHITE, 12f);
        if (targetPercent > 0.02f) {
            nanoVGHelper.drawRoundedRect(vg, xCoordinate - (TOUCH_TARGET_HOVER / 2 * targetPercent), y + 16 - (TOUCH_TARGET_HOVER / 2 * targetPercent), TOUCH_TARGET_HOVER * targetPercent, TOUCH_TARGET_HOVER * targetPercent, Colors.PRIMARY_500, 12f);
        }

        inputField.draw(vg, x + 892, y, inputHandler);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    private int getStepCoordinate(int xCoordinate, int x) {
        Integer nearest = null;
        for (float i = x + 352; i <= x + 864; i += 512 / ((max - min) / step)) {
            if (nearest == null || Math.abs(xCoordinate - i) < Math.abs(xCoordinate - nearest))
                nearest = (int) i;
        }
        return nearest == null ? 0 : nearest;
    }

    private void setValue(float value) {
        try {
            if (isFloat) set(value);
            else set(Math.round(value));
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        inputField.keyTyped(key, keyCode);
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public boolean hasFocus() {
        return inputField.isToggled();
    }
}
