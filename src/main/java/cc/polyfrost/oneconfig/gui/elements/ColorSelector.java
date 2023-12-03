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

package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.libs.universal.UMouse;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ColorSelector {
    private OneColor color;
    private static final BasicButton closeBtn = new BasicButton(32, 32, SVGs.X_CLOSE, BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY_DESTRUCTIVE);
    private final BasicButton faveBtn = new BasicButton(32, 32, SVGs.HEART_OUTLINE, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton recentBtn = new BasicButton(32, 32, SVGs.HISTORY, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY) {
        @Override
        public void onClick() {
            super.onClick();
            if (recentColors.get(0).getColor().equals(color)) return;
            assignRecentColor(color.clone());
            recentColors.remove(5);
            recentColors.add(0, new ColorBox(color.clone()));
        }
    };
    private static final BasicButton pickerBtn = new BasicButton(32, 32, SVGs.PICKER, BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY);
    private final Dropdown modeDropdown = new Dropdown(128, 36, 18, new String[]{"Solid Color", "Chroma"}, 0, ColorPalette.TERTIARY) {
        @Override
        public void onChange(int changedTo) {
            switch (changedTo) {
                default:
                case 0:
                    picker = new SolidColorPicker();
                    break;
                case 1:
                    picker = new ChromaColorPicker();
            }
        }
    };
    private final Dropdown inputTypeDropdown = new Dropdown(90, 36, 20, new String[]{"HEX", "HSBA"}, 0, ColorPalette.TERTIARY) {
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
    private float x, y;
    private float dragX, dragY;
    @NotNull
    private ColorInput colorInput;
    @SuppressWarnings("NotNullFieldNotInitialized")
    @NotNull
    private ColorPicker picker;
    private static float width = 296, height = 398;
    private boolean pickerIsActive, mouseWasDown, dragging;
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
        if (color.getDataBit() != -1) {
            modeDropdown.select(1);
        } else {
            picker = new SolidColorPicker();
        }
        this.x = mouseX - width / 2;
        this.y = Math.max(0, mouseY - height);
        colorInput = new HexInput();
    }

    public void draw(long vg) {
        if (inputScissor != null) inputHandler.stopBlock(inputScissor);
        if (pickerBtn.toggled) {
            inputHandler.blockAllInput();
            final int color = NanoVGHelper.INSTANCE.readPixels((int) (UMouse.Raw.getX()), UResolution.getViewportHeight() - (int) UMouse.Raw.getY(), 1, 1)[0];
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, inputHandler.mouseX() - 16, inputHandler.mouseY() - 33, 32, 32, -1, 16f);
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, inputHandler.mouseX() - 15, inputHandler.mouseY() - 32, 30, 30, color, 15f);
            if (inputHandler.isClicked(true)) {
                __setColor(new OneColor(color));
                pickerBtn.toggled = false;
                inputHandler.stopBlockingInput();
            }
            return;
        }

        Scissor scissor = ScissorHelper.INSTANCE.scissor(vg, x - 3, y - 3, width + 6, height + 6);
        NanoVGHelper.INSTANCE.drawHollowRoundRect(vg, x - 3, y - 3, width + 3, height + 3, Colors.GRAY_700, 20f, 3);
        NanoVGHelper.INSTANCE.drawRoundedRect(vg, x, y, width, height, Colors.GRAY_800, 20f);
        closeBtn.draw(vg, x + 248 + 8, y + 12, inputHandler);

        picker.drawAndUpdate(vg, x + 16, y + 48, inputHandler);

        if (modeDropdown.getSelected() == 1) {
            y += 20;
            inputHandler.stopBlockingInput();
        }

        colorInput.drawAndUpdate(vg, x + 100, y + 260, inputHandler);

        pickerBtn.draw(vg, x + 252, y + 260, inputHandler);

        faveBtn.draw(vg, x + 12, y + 304, inputHandler);

        if (faveBtn.isClicked()) {
            favoriteColors.remove(5);
            favoriteColors.add(0, new ColorBox(color.clone()));
        }

        recentBtn.draw(vg, x + 12, y + 348, inputHandler);

        for (int i = 0; i < 6; i++) {
            final ColorBox box = favoriteColors.get(i);
            box.draw(vg, x + 52 + i * 40, y + 304, inputHandler);
            if (box.isClicked()) {
                __setColor(box.getColor().clone());
            }

            final ColorBox rBox = recentColors.get(i);
            rBox.draw(vg, x + 52 + i * 40, y + 348, inputHandler);
            if (rBox.isClicked()) {
                __setColor(rBox.getColor().clone());
            }
        }

        inputTypeDropdown.draw(vg, x - 2, y + 260, inputHandler);

        if (modeDropdown.getSelected() == 1) {
            y -= 20;
        }
        modeDropdown.draw(vg, x - 2, y + 8, inputHandler);

        final boolean hovered = Platform.getMousePlatform().isButtonDown(0) && inputHandler.isAreaHovered(x, y, width, 48);
        if (hovered && Platform.getMousePlatform().isButtonDown(0) && !mouseWasDown) {
            dragging = true;
            dragX = inputHandler.mouseX() - x;
            dragY = inputHandler.mouseY() - y;
        }
        if (dragging) {
            x = inputHandler.mouseX() - dragX;
            y = inputHandler.mouseY() - dragY;
        }
        inputScissor = inputHandler.blockInputArea(x, y, width, height);
        if (closeBtn.isClicked() && !pickerIsActive) {
            OneConfigGui.INSTANCE.closeColorSelector();
        }
        ScissorHelper.INSTANCE.resetScissor(vg, scissor);
        if (dragging && inputHandler.isClicked(true)) {
            dragging = false;
        }
        if (pickerIsActive && inputHandler.isClicked(true)) {
            pickerIsActive = false;
        }
        mouseWasDown = Platform.getMousePlatform().isButtonDown(0);
    }


    public OneColor getColor() {
        return color;
    }

    public void keyTyped(char typedChar, int keyCode) {
        colorInput.keyTyped(typedChar, keyCode);
    }

    public void onClose() {
        inputHandler.stopBlockingInput();
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
            OneConfigConfig.recentColors.remove(5);
            OneConfigConfig.recentColors.add(0, color);
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
                alphaInput.setInput(Math.round(color.getAlpha() / 2.55f) + "%");
            }
        };
        private final TextInputField hexInput = new TextInputField(80, 32, true, 4f) {
            @Override
            public void onClose() {
                hexInput.setInput("#" + color.getHex());
            }
        };

        public HexInput() {
            hexInput.setBoarderThickness(1f);
            alphaInput.setBoarderThickness(1f);
            hexInput.setInput("#" + color.getHex());
            alphaInput.setInput(Math.round(color.getAlpha() / 2.55f) + "%");
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
                    color.setAlpha(Math.round(Math.max(0, Math.min(Float.parseFloat(s), 100)) * 2.55f));
                    picker.onColorChanged();
                } catch (Exception e) {
                    alphaInput.setErrored(true);
                }
            }
        }

        @Override
        public void onColorChanged() {
            hexInput.setInput("#" + color.getHex());
            alphaInput.setInput(Math.round(color.getAlpha() / 2.55f) + "%");
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
                inputs[i].setBoarderThickness(1f);
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
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, cursorX - 7, cursorY - 7, 14, 14, Colors.WHITE, 14f);
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, cursorX - 5, cursorY - 5, 10, 10, color, 10f);
        }
    }

    private final class SolidColorPicker implements ColorPicker {
        private float cursorX, cursorY;
        private final ColorSlider alphaSlider, hueSlider;

        public SolidColorPicker() {
            width = 296;
            height = 398;
            cursorX = (color.getSaturation() / 100f * 200);
            cursorY = (1 - (color.getBrightness() / 100f)) * 200;
            alphaSlider = new ColorSlider(200, 0, 255, 255 - color.getAlpha(), Colors.TRANSPARENT, color.getRGBMax(true));
            hueSlider = new ColorSlider(200, 0, 360, 360 - color.getHue());
            color.setChromaSpeed(-1);
            if (!hasAlpha) alphaSlider.disable(true);
        }

        @Override
        public void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler) {
            NanoVGHelper.INSTANCE.drawHSBBox(vg, x, y, 200, 200, color.getRGBMax(true));
            drawCursor(vg, x + cursorX, y + cursorY, color.getRGB());
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
            cursorX = inputHandler.mouseX() - x;
            cursorY = inputHandler.mouseY() - y;
            if (cursorX < 0) cursorX = 0;
            if (cursorY < 0) cursorY = 0;
            if (cursorX > 200) cursorX = 200;
            if (cursorY > 200) cursorY = 200;
            final float progressX = cursorX / 200f;
            final float progressY = Math.abs(cursorY / 200f - 1f);
            color.setHSBA((int) hueSlider.getValueInverted(), Math.round(progressX * 100), Math.round(progressY * 100), (int) alphaSlider.getValueInverted());
        }

        @Override
        public void onColorChanged() {
            cursorX = (color.getSaturation() / 100f * 200);
            cursorY = (1 - (color.getBrightness() / 100f)) * 200;
            alphaSlider.setValueInverted(color.getAlpha());
            hueSlider.setValueInverted(color.getHue());
            alphaSlider.setGradient(color.getRGBNoAlpha(), Colors.TRANSPARENT);
        }
    }

    private final class ChromaColorPicker implements ColorPicker {
        private float cursorX, cursorY;
        private final ColorSlider alphaSlider;
        private final Slider speedSlider = new Slider(200, 1, 30, 1, Slider.HORIZONTAL);

        public ChromaColorPicker() {
            width = 296;
            height = 408;
            cursorX = (color.getSaturation() / 100f * 200);
            cursorY = (1 - (color.getBrightness() / 100f)) * 200;
            alphaSlider = new ColorSlider(200, 0, 255, 255 - color.getAlpha(), Colors.TRANSPARENT, color.getRGBMax(true));
            if (color.getDataBit() == -1) color.setChromaSpeed(30);
            speedSlider.setValueInverted(color.getDataBit());
            if (!hasAlpha) alphaSlider.disable(true);
        }

        @Override
        public void drawAndUpdate(long vg, float x, float y, InputHandler inputHandler) {
            speedSlider.draw(vg, x, y, inputHandler);
            y += 8 + 12;
            NanoVGHelper.INSTANCE.drawHSBBox(vg, x, y, 200, 200, color.getRGBMax(true));
            drawCursor(vg, x + cursorX, y + cursorY, color.getRGB());
            alphaSlider.draw(vg, x + 216, y, inputHandler);
            alphaSlider.setGradient(color.getRGBNoAlpha(), Colors.TRANSPARENT);

            if (alphaSlider.isDragging() || speedSlider.isDragging()) {
                color.setHSBA(color.getHue(), color.getSaturation(), color.getBrightness(), (int) alphaSlider.getValueInverted());
                color.setChromaSpeed(Math.round(speedSlider.getValueInverted()));
//                speedSlider.setValueInverted(color.getDataBit());
            }
            colorInput.onColorChanged();

            final boolean hovered = Platform.getMousePlatform().isButtonDown(0) && inputHandler.isAreaHovered(x, y, 200, 200);
            if (hovered && Platform.getMousePlatform().isButtonDown(0) && !mouseWasDown) pickerIsActive = true;
            if (!pickerIsActive) return;
            cursorX = inputHandler.mouseX() - x;
            cursorY = inputHandler.mouseY() - y;
            if (cursorX < 0) cursorX = 0;
            if (cursorY < 0) cursorY = 0;
            if (cursorX > 200) cursorX = 200;
            if (cursorY > 200) cursorY = 200;
            final float progressX = cursorX / 200f;
            final float progressY = Math.abs(cursorY / 200f - 1f);
            color.setHSBA(color.getHue(), Math.round(progressX * 100), Math.round(progressY * 100), (int) alphaSlider.getValueInverted());
        }

        @Override
        public void onColorChanged() {
            cursorX = (color.getSaturation() / 100f * 200);
            cursorY = (1 - (color.getBrightness() / 100f)) * 200;
            alphaSlider.setValueInverted(color.getAlpha());
            alphaSlider.setGradient(color.getRGBNoAlpha(), Colors.TRANSPARENT);
            speedSlider.setValueInverted(color.getDataBit());
        }
    }


    // other util classes
    static final class ColorSlider extends Slider {
        private int gradColorStart, gradColorEnd;
        private final boolean img;

        public ColorSlider(int length, float min, float max, float startValue, int gradColorStart, int gradColorEnd) {
            super(length, min, max, startValue, VERTICAL);
            super.width = 16;
            super.dragPointerSize = 12f;
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
            else NanoVGHelper.INSTANCE.setAlpha(vg, 0.5f);
            if (img) {
                NanoVGHelper.INSTANCE.drawRoundImage(vg, Images.HUE_GRADIENT.filePath, x + 1, y + 1, width - 2, height - 2, 8f);
            } else {
                NanoVGHelper.INSTANCE.drawRoundImage(vg, Images.VERTICAL_ALPHA_GRID, x + 1, y + 1, width - 2, height - 2, 8f);
                NanoVGHelper.INSTANCE.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f, NanoVGHelper.GradientDirection.DOWN);
            }
            // I actually hate this
            NanoVGHelper.INSTANCE.drawHollowRoundRect(vg, x + 1.5f, currentDragPoint, 12, 12, Colors.WHITE, 6f, 1f);
            NanoVGHelper.INSTANCE.setAlpha(vg, 1f);
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
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, x, y, 32, 32, Colors.GRAY_900, 8f);
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, x, y, 32, 32, color.getRGB(), 8f);
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