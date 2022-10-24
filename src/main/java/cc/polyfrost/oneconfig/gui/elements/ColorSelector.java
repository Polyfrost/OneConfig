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

package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ColorSelector {
    private OneColor color;
    private static final BasicButton closeBtn = new BasicButton(32, 32, SVGs.ARROW_LEFT, BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY_DESTRUCTIVE);
    private final BasicButton faveBtn = new BasicButton(32, 32, SVGs.HEART_OUTLINE, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton recentBtn = new BasicButton(32, 32, SVGs.HISTORY, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY) {
        @Override
        public void onClick() {
            super.onClick();
            assignRecentColor(color.clone());
        }
    };
    private static final BasicButton pickerBtn = new BasicButton(32, 32, SVGs.COPY, BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY);
    private final Dropdown modeDropdown = new Dropdown(128, 36, new String[]{"Solid Color"}, 0, ColorPalette.TERTIARY) {
        @Override
        public void onChange(int changedTo) {
            switch (changedTo) {
                default:
                case 0:
                    picker = new SolidColorPicker();
            }
        }
    };
    private final Dropdown inputTypeDropdown = new Dropdown(90, 36, new String[]{"HEX", "HSBA"}, 0, ColorPalette.TERTIARY) {
        @Override
        public void onChange(int changedTo) {
            switch (changedTo) {
                default:
                case 0:
                    colorInput = new HexInput();
                    break;
                case 1:
                    colorInput = new HSBAInput();
                    break;
            }
        }
    };
    private final ArrayList<ColorBox> favoriteColors = new ArrayList<>(6);
    private final ArrayList<ColorBox> recentColors = new ArrayList<>(6);
    private final float x, y;
    private float cursorX, cursorY;
    @NotNull
    private ColorInput colorInput;
    @NotNull
    private ColorPicker picker;
    private static final float width = 296, height = 398;
    private boolean pickerIsActive, mouseWasDown;
    private final boolean hasAlpha;
    private Scissor inputScissor;
    private final InputHandler inputHandler;

    public ColorSelector(OneColor color, float mouseX, float mouseY, final boolean hasAlpha, final InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        for (OneColor oo : OneConfigConfig.recentColors) {
            // this was annoying to use.
            //if (oo.equals(color)) color = oo;
            recentColors.add(new ColorBox(oo));
        }
        for (OneColor o : OneConfigConfig.favoriteColors) {
            //if (o.equals(color)) color = o;
            favoriteColors.add(new ColorBox(o));
        }
        while (favoriteColors.size() < 6) favoriteColors.add(new ColorBox(new OneColor(0, 0, 0, 0)));
        while (recentColors.size() < 6) recentColors.add(new ColorBox(new OneColor(0, 0, 0, 0)));
        this.color = color;
        this.hasAlpha = hasAlpha;
        this.x = mouseX - width / 2;
        this.y = Math.max(0, mouseY - height);
        // it looks nice okay? streams are cool
        faveBtn.setToggleable(true);
        picker = new SolidColorPicker();
        colorInput = new HexInput();
    }

    public void draw(long vg) {
        if (inputScissor != null) inputHandler.stopBlock(inputScissor);
        if (pickerBtn.toggled) {
            inputHandler.blockAllInput();
            // net.minecraft.util.ScreenShotHelper
            final ByteBuffer buf = BufferUtils.createByteBuffer(4);
            // TODO this segfaults lmao
            // if(Platform.getGLPlatform().isFrameBufferEnabled()) GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            GL11.glReadPixels((int) Platform.getMousePlatform().getMouseY(), (int) Platform.getMousePlatform().getMouseY(), 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            final int color = ColorUtils.getColor(buf.get(), buf.get(), buf.get(), buf.get());
            RenderManager.drawRoundedRect(vg, inputHandler.mouseX() - 16, inputHandler.mouseY() - 32, 32, 32, -1, 16f);
            RenderManager.drawRoundedRect(vg, inputHandler.mouseX() - 15, inputHandler.mouseY() - 31, 30, 30, color, 15f);
            if (inputHandler.isClicked(true)) {
                __setColor(new OneColor(color));
                pickerBtn.toggled = false;
                inputHandler.stopBlockingInput();
            }
        }

        Scissor scissor = ScissorManager.scissor(vg, x - 3, y - 3, width + 6, height + 6);
        RenderManager.drawRoundedRect(vg, x, y, width, height, Colors.GRAY_800, 20f);
        closeBtn.draw(vg, x + 248, y + 8, inputHandler);

        picker.drawAndUpdate(vg, x + 16, y + 48, inputHandler);
        picker.drawCursor(vg, cursorX, cursorY, color.getRGB());

        colorInput.drawAndUpdate(vg, x + 100, y + 260, inputHandler);

        pickerBtn.draw(vg, x + 252, y + 260, inputHandler);

        faveBtn.draw(vg, x + 12, y + 304, inputHandler);
        recentBtn.draw(vg, x + 12, y + 348, inputHandler);
        for (int i = 0; i < 6; i++) {
            final ColorBox box = favoriteColors.get(i);
            box.draw(vg, x + 52 + i * 40, y + 304, inputHandler);
            if (box.isClicked()) {
                if (faveBtn.isToggled()) {
                    box.setColor(color);
                    // remove tie
                    __setColor(color.clone());
                    faveBtn.setToggled(false);
                } else {
                    __setColor(box.getColor());
                }
            }
            final ColorBox rBox = recentColors.get(i);
            rBox.draw(vg, x + 52 + i * 40, y + 348, inputHandler);
            if (rBox.isClicked()) {
                __setColor(rBox.getColor());
            }
        }

        modeDropdown.draw(vg, x + 16, y + 8, inputHandler);
        inputTypeDropdown.draw(vg, x + 2, y + 260, inputHandler);

        inputScissor = inputHandler.blockInputArea(x, y, width, height);
        ScissorManager.resetScissor(vg, scissor);
        if (pickerIsActive && inputHandler.isClicked(true)) {
            pickerIsActive = false;
        }
        mouseWasDown = Platform.getMousePlatform().isButtonDown(0);
        if (closeBtn.isClicked() && !mouseWasDown) {
            OneConfigGui.INSTANCE.closeColorSelector();
        }
    }

    public void update() {
        doDrag();
    }


    private void doDrag() {
        if (inputHandler.isAreaHovered(x, y, width, 48) && Platform.getMousePlatform().isButtonDown(0) && !pickerIsActive) {
            // TODO: drag
        }
    }


    public OneColor getColor() {
        return color;
    }

    public void keyTyped(char typedChar, int keyCode) {
        colorInput.keyTyped(typedChar, keyCode);
    }

    public void onClose() {
        if (inputScissor != null) inputHandler.stopBlock(inputScissor);
        int i = 0;
        for (ColorBox box : favoriteColors) {
            if (box.isEmpty()) continue;
            assignFavoriteColor(box.getColor().clone(), i);
            i++;
        }
        assignRecentColor(color.clone());
        OneConfigConfig.getInstance().save();
    }

    public static void assignRecentColor(OneColor color) {
        if (OneConfigConfig.recentColors.size() == 6) {
            OneConfigConfig.recentColors.set(0, color);
        } else OneConfigConfig.recentColors.add(color);
    }

    public static void assignFavoriteColor(OneColor color, int index) {
        if (index < 0) index = 0;
        if (index > 5) index = 5;
        if (index >= OneConfigConfig.favoriteColors.size()) OneConfigConfig.favoriteColors.add(color);
        else OneConfigConfig.favoriteColors.set(index, color);
    }

    /**
     * <b>Dangerous Method!</b>
     */
    private void __setColor(OneColor color) {
        if (color.getRGB() == 0) return;
        this.color = color;
        picker.onColorChanged();
        colorInput.onColorChanged();
    }

    public boolean isAlphaAllowed() {
        return hasAlpha;
    }


    // input types
    interface ColorInput {
        void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler);

        void onColorChanged();

        void keyTyped(char typedChar, int keyCode);
    }

    final class HexInput implements ColorInput {
        private final TextInputField alphaInput = new TextInputField(56, 32, true, 4f) {
            @Override
            public void onClose() {
                alphaInput.setInput((int) (color.getAlpha() / 2.55f) + "%");
            }
        };
        private final TextInputField hexInput = new TextInputField(80, 32, true, 4f) {
            @Override
            public void onClose() {
                hexInput.setInput("#" + color.getHex());
            }
        };

        public HexInput() {
            hexInput.setInput("#" + color.getHex());
            alphaInput.setInput((int) (color.getAlpha() / 2.55f) + "%");
            if (!hasAlpha) alphaInput.disable(true);
        }

        @Override
        public void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler) {
            hexInput.draw(vg, x, y, inputHandler);
            alphaInput.draw(vg, x + 88, y, inputHandler);
            hexInput.setErrored(false);
            alphaInput.setErrored(false);
            if (hexInput.isToggled()) {
                try {
                    color.setColorFromHex(hexInput.getInput());
                    picker.onColorChanged();
                } catch (Exception e) {
                    hexInput.setErrored(true);
                }
            }
            if (alphaInput.isToggled()) {
                final String s = alphaInput.getInput().endsWith("%") ? alphaInput.getInput().substring(0, alphaInput.getInput().length() - 1) : alphaInput.getInput();
                try {
                    color.setAlpha((int) (Float.parseFloat(s) * 2.55f));
                    picker.onColorChanged();
                } catch (Exception e) {
                    alphaInput.setErrored(true);
                }
            }
        }

        @Override
        public void onColorChanged() {
            hexInput.setInput("#" + color.getHex());
            alphaInput.setInput((int) (color.getAlpha() / 2.55f) + "%");
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            hexInput.keyTyped(typedChar, keyCode);
            alphaInput.keyTyped(typedChar, keyCode);
        }
    }

    final class HSBAInput implements ColorInput {
        final TextInputField[] inputs = new TextInputField[4];

        HSBAInput() {
            for (int i = 0; i < inputs.length; i++) {
                final int index = i;
                inputs[i] = new TextInputField(40, 32, true, 4f) {
                    @Override
                    public void onClose() {
                        inputs[index].setInput(color.getHSBA()[index] + "");
                    }
                };
                inputs[i].setInput(color.getHSBA()[i] + "");
            }
            if (!hasAlpha) inputs[3].disable(true);
        }

        @Override
        public void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler) {
            for (int i = 0; i < 4; i++) {
                final TextInputField in = inputs[i];
                in.setErrored(false);
                in.draw(vg, x - 12 + (42 * i), y, inputHandler);
                if (in.isToggled()) {
                    color.setHSBA(i, parseIntOrElse(in.getInput(), color.getHSBA()[i]));
                    picker.onColorChanged();
                }
            }
        }

        @Override
        public void onColorChanged() {
            inputs[0].setInput(color.getHue() + "");
            inputs[1].setInput(color.getSaturation() + "");
            inputs[2].setInput(color.getBrightness() + "");
            inputs[3].setInput(color.getAlpha() + "");
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            for (TextInputField i : inputs) i.keyTyped(typedChar, keyCode);
        }

        int parseIntOrElse(String s, int orElse) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                return orElse;
            }
        }
    }


    // color pickers
    interface ColorPicker {
        void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler);

        void onColorChanged();

        default void drawCursor(long vg, float cursorX, float cursorY, int color) {
            RenderManager.drawRoundedRect(vg, cursorX - 7, cursorY - 7, 14, 14, Colors.WHITE, 14f);
            RenderManager.drawRoundedRect(vg, cursorX - 5, cursorY - 5, 10, 10, color, 10f);
        }
    }

    private final class SolidColorPicker implements ColorPicker {
        private final ColorSlider alphaSlider, hueSlider;

        public SolidColorPicker() {
            cursorX = (color.getSaturation() / 100f * 200) + x + 16;
            cursorY = (1 - (color.getBrightness() / 100f)) * 200 + y + 48;
            alphaSlider = new ColorSlider(200, 0, 255, 255 - color.getAlpha(), Colors.TRANSPARENT, color.getRGBMax(true));
            hueSlider = new ColorSlider(200, 0, 360, 360 - color.getHue());
            if (!hasAlpha) alphaSlider.disable(true);
        }

        @Override
        public void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler) {
            RenderManager.drawHSBBox(vg, x, y, 200, 200, color.getRGBMax(true));
            hueSlider.draw(vg, x + 216, y, inputHandler);
            alphaSlider.draw(vg, x + 248, y, inputHandler);
            alphaSlider.setGradient(color.getRGBNoAlpha(), Colors.TRANSPARENT);

            if (hueSlider.isDragging() || alphaSlider.isDragging()) {
                color.setHSBA((int) hueSlider.getValueInverted(), color.getSaturation(), color.getBrightness(), (int) alphaSlider.getValueInverted());
                colorInput.onColorChanged();
            }

            final boolean hovered = Platform.getMousePlatform().isButtonDown(0) && inputHandler.isAreaHovered(x, y, 200, 200);
            if (hovered && Platform.getMousePlatform().isButtonDown(0) && !mouseWasDown) pickerIsActive = true;
            if (!pickerIsActive) return;
            cursorX = inputHandler.mouseX();
            cursorY = inputHandler.mouseY();
            if (cursorX < x) cursorX = x;
            if (cursorY < y) cursorY = y;
            if (cursorX > x + 200) cursorX = x + 200;
            if (cursorY > y + 200) cursorY = y + 200;
            final float progressX = (cursorX - x) / 200f;
            final float progressY = Math.abs((cursorY - y) / 200f - 1f);
            color.setHSBA((int) hueSlider.getValueInverted(), Math.round(progressX * 100), Math.round(progressY * 100), (int) alphaSlider.getValueInverted());
        }

        @Override
        public void onColorChanged() {
            cursorX = (color.getSaturation() / 100f * 200) + x + 16;
            cursorY = (1 - (color.getBrightness() / 100f)) * 200 + y + 48;
            alphaSlider.setValueInverted(color.getAlpha());
            hueSlider.setValueInverted(color.getHue());
            alphaSlider.setGradient(color.getRGBNoAlpha(), Colors.TRANSPARENT);
        }
    }


    // other util classes
    static final class ColorSlider extends Slider {
        private int gradColorStart, gradColorEnd;
        private final boolean img;

        public ColorSlider(int length, float min, float max, float startValue, int gradColorStart, int gradColorEnd) {
            super(length, min, max, startValue, VERTICAL);
            super.width = 16;
            super.dragPointerSize = 15f;
            if (gradColorEnd == 0 && gradColorStart == 0) {
                this.gradColorStart = 0;
                this.gradColorEnd = 0;
                img = true;
            } else {
                this.gradColorStart = gradColorStart;
                this.gradColorEnd = gradColorEnd;
                img = false;
            }
        }

        public ColorSlider(int length, float min, float max, float startValue) {
            this(length, min, max, startValue, 0, 0);
        }

        @Override
        public void draw(long vg, float x, float y, InputHandler inputHandler) {
            if (!disabled) super.update(x, y, inputHandler);
            else RenderManager.setAlpha(vg, 0.5f);
            if (img) {
                RenderManager.drawRoundImage(vg, Images.HUE_GRADIENT.filePath, x + 1, y + 1, width - 2, height - 2, 8f);
            } else {
                RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x + 1, y + 1, width - 2, height - 2, 8f);
                RenderManager.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f, RenderManager.GradientDirection.DOWN);
            }
            RenderManager.drawHollowRoundRect(vg, x, currentDragPoint, 14, 14, Colors.WHITE, 7f, 1f);
            RenderManager.setAlpha(vg, 1f);
        }

        public void setGradient(int gradColorStart, int gradColorEnd) {
            if (img) return;
            this.gradColorEnd = gradColorEnd;
            this.gradColorStart = gradColorStart;
        }
    }

    static final class ColorBox extends BasicElement {
        private OneColor color;

        public ColorBox(OneColor color) {
            // this attempts to make the color hover-able
            super(32, 32, new ColorPalette(color.getRGB(), color.toJavaColor().brighter().getRGB(), ColorUtils.setAlpha(color.getRGB(), (int) (color.getAlpha() * 0.8f))), true);
            this.color = color;
        }

        @Override
        public void draw(long vg, float x, float y, InputHandler inputHandler) {
            RenderManager.drawRoundedRect(vg, x, y, 32, 32, Colors.GRAY_900, 8f);
            RenderManager.drawRoundedRect(vg, x, y, 32, 32, color.getRGB(), 8f);
            super.update(x, y, inputHandler);
        }

        public OneColor getColor() {
            return color;
        }

        public void setColor(OneColor color) {
            this.color = color;
        }

        public boolean isEmpty() {
            return color.getRGB() == 0;
        }
    }
}