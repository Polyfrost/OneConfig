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
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutCubic;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import cc.polyfrost.oneconfig.utils.IOUtils;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

import java.awt.*;
import java.util.ArrayList;

public class ColorSelector {
    private final OneColor color;
    private final ArrayList<BasicElement> buttons = new ArrayList<>();
    private final BasicElement closeBtn = new BasicElement(32, 32, false);
    private final BasicButton copyBtn = new BasicButton(32, 32, SVGs.COPY, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton pasteBtn = new BasicButton(32, 32, SVGs.PASTE, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton guideBtn = new BasicButton(112, 32, "Guide", SVGs.HELP_CIRCLE, SVGs.POP_OUT, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton faveBtn = new BasicButton(32, 32, SVGs.HEART_OUTLINE, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final BasicButton recentBtn = new BasicButton(32, 32, SVGs.HISTORY, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
    private final NumberInputField hueInput = new NumberInputField(90, 32, 0, 0, 360, 1);
    private final NumberInputField saturationInput = new NumberInputField(90, 32, 100, 0, 100, 1);
    private final NumberInputField brightnessInput = new NumberInputField(90, 32, 100, 0, 100, 1);
    private final NumberInputField alphaInput = new NumberInputField(90, 32, 0, 0, 100, 1);
    private final NumberInputField speedInput = new NumberInputField(90, 32, 2, 1, 30, 1);
    private final TextInputField hexInput = new TextInputField(88, 32, true, "");
    private final ArrayList<ColorBox> favoriteColors = new ArrayList<>();
    private final ArrayList<ColorBox> recentColors = new ArrayList<>();
    private final ColorSlider topSlider = new ColorSlider(384, 0, 360, 127);
    private final ColorSlider bottomSlider = new ColorSlider(384, 0, 255, 100);
    private final Slider speedSlider = new Slider(296, 1, 32, 0);
    private float x;
    private float y;
    private Animation barMoveAnimation = new DummyAnimation(18);
    private Animation moveAnimation = new DummyAnimation(1);
    private float mouseX, mouseY;
    private int mode = 0;
    private boolean dragging, mouseWasDown;
    private final boolean hasAlpha;
    private Scissor inputScissor = null;
    private final InputHandler inputHandler;

    public ColorSelector(OneColor color, float mouseX, float mouseY, InputHandler inputHandler) {
        this(color, mouseX, mouseY, true, inputHandler);
    }

    public ColorSelector(OneColor color, float mouseX, float mouseY, boolean hasAlpha, InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        this.color = color;
        this.hasAlpha = hasAlpha;
        buttons.add(new BasicButton(124, 28, "HSB Box", BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY));
        buttons.add(new BasicButton(124, 28, "Color Wheel", BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY));
        buttons.add(new BasicButton(124, 28, "Chroma", BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY));
        hueInput.setCurrentValue(color.getHue());
        saturationInput.setCurrentValue(color.getSaturation());
        brightnessInput.setCurrentValue(color.getBrightness());
        alphaInput.setCurrentValue(color.getAlpha() / 255f * 100f);
        if (!hasAlpha) {
            bottomSlider.disabled = true;
            alphaInput.disabled = true;
        }
        speedSlider.setValue(color.getDataBit());
        topSlider.setValue(color.getHue());
        topSlider.setColor(color.getRGBMax(true));
        bottomSlider.setValue(color.getAlpha());
        hexInput.setInput(color.getHex());
        this.x = mouseX - 208;
        this.y = Math.max(0, mouseY - 776);
        if (color.getDataBit() != -1) mode = 2;
        if (mode == 0 || mode == 2) {
            this.mouseX = (color.getSaturation() / 100f * 384 + x + 16);
            this.mouseY = (Math.abs(color.getBrightness() / 100f - 1f) * 288 + y + 120);
        } else {
            topSlider.setValue(color.getBrightness() / 100f * 360f);
            this.mouseX = (float) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
            this.mouseY = (float) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
        }
        //for(OneColor color1 : OneConfigConfig.recentColors) {
        //    recentColors.add(new ColorBox(color1));
        //}
        //for(OneColor color1 : OneConfigConfig.favoriteColors) {
        //    favoriteColors.add(new ColorBox(color1));
        //}
        while (favoriteColors.size() < 7) {
            favoriteColors.add(new ColorBox(new OneColor(0, 0, 0, 0)));
        }
        while (recentColors.size() < 7) {
            recentColors.add(new ColorBox(new OneColor(0, 0, 0, 0)));
        }

        topSlider.setImage(Images.HUE_GRADIENT.filePath);
    }

    public void draw(long vg) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        ScissorHelper scissorHelper = ScissorHelper.INSTANCE;

        if (inputScissor != null)
            inputHandler.stopBlock(inputScissor);
        doDrag();
        int width = 416;
        int height = 768;
        Scissor scissor = scissorHelper.scissor(vg, x - 3, y - 3, width + 6, height + 6);
        nanoVGHelper.drawHollowRoundRect(vg, x - 3, y - 3, width + 4, height + 4, new Color(204, 204, 204, 77).getRGB(), 20f, 2f);
        nanoVGHelper.drawRoundedRect(vg, x, y, width, height, Colors.GRAY_800, 20f);
        nanoVGHelper.drawText(vg, "Color Selector", x + 16, y + 32, Colors.WHITE_90, 18f, Fonts.SEMIBOLD);
        if (!closeBtn.isHovered()) nanoVGHelper.setAlpha(vg, 0.8f);
        closeBtn.draw(vg, x + 368, y + 16, inputHandler);
        nanoVGHelper.drawSvg(vg, SVGs.X_CIRCLE_BOLD, x + 368, y + 16, 32, 32, closeBtn.isHovered() ? Colors.ERROR_600 : -1);
        nanoVGHelper.setAlpha(vg, 1f);

        // hex parser
        if (hexInput.isToggled()) {
            parseHex();
        }

        // TODO favorite stuff
        faveBtn.draw(vg, x + 16, y + 672, inputHandler);
        recentBtn.draw(vg, x + 16, y + 720, inputHandler);
        for (int i = 0; i < 7; i++) {
            favoriteColors.get(i).draw(vg, x + 104 + i * 44, y + 672, inputHandler);
        }
        for (int i = 0; i < 7; i++) {
            recentColors.get(i).draw(vg, x + 104 + i * 44, y + 720, inputHandler);
        }

        nanoVGHelper.drawRoundedRect(vg, x + 16, y + 64, 384, 32, Colors.GRAY_500, 12f);
        if (!barMoveAnimation.isFinished())
            nanoVGHelper.drawRoundedRect(vg, x + barMoveAnimation.get(), y + 66, 124, 28, Colors.PRIMARY_600, 10f);
        else buttons.get(mode).setColorPalette(ColorPalette.PRIMARY);

        int i = 18;
        for (BasicElement button : buttons) {
            button.draw(vg, x + i, y + 66, inputHandler);
            if (button.isClicked()) {
                int prevMode = mode;
                mode = buttons.indexOf(button);
                setXYFromColor();
                barMoveAnimation = new EaseInOutCubic(175, 18 + prevMode * 128, 18 + mode * 128, false);
                moveAnimation = new EaseInOutQuad(300, 0, 1, false);
                for (BasicElement button1 : buttons) button1.setColorPalette(ColorPalette.TERTIARY);
            }
            i += 128;
        }
        float percentMoveMain = moveAnimation.get();

        nanoVGHelper.drawText(vg, "Saturation", x + 224, y + 560, Colors.WHITE_80, 12f, Fonts.MEDIUM);
        saturationInput.draw(vg, x + 312, y + 544, inputHandler);
        nanoVGHelper.drawText(vg, "Brightness", x + 16, y + 599, Colors.WHITE_80, 12f, Fonts.MEDIUM);
        brightnessInput.draw(vg, x + 104, y + 584, inputHandler);
        nanoVGHelper.drawText(vg, "Alpha (%)", x + 224, y + 599, Colors.WHITE_80, 12f, Fonts.MEDIUM);
        alphaInput.draw(vg, x + 312, y + 584, inputHandler);
        nanoVGHelper.drawText(vg, color.getDataBit() == -1 ? "Hex (RGB):" : "Color Code:", x + 16, y + 641, Colors.WHITE_80, 12f, Fonts.MEDIUM);
        hexInput.draw(vg, x + 104, y + 624, inputHandler);

        copyBtn.draw(vg, x + 204, y + 624, inputHandler);
        pasteBtn.draw(vg, x + 244, y + 624, inputHandler);
        if (mode != 2) {
            nanoVGHelper.drawText(vg, "Hue", x + 16, y + 560, Colors.WHITE_80, 12f, Fonts.MEDIUM);
            hueInput.draw(vg, x + 104, y + 544, inputHandler);
        } else {
            nanoVGHelper.drawText(vg, "Speed (s)", x + 16, y + 560, Colors.WHITE_80, 12f, Fonts.MEDIUM);
            speedInput.draw(vg, x + 104, y + 544, inputHandler);
        }

        guideBtn.draw(vg, x + 288, y + 624, inputHandler);


        setColorFromXY();
        if (mode != 2) color.setChromaSpeed(-1);
        drawColorSelector(vg, mode, (x * percentMoveMain), y);
        if (dragging && inputHandler.isClicked(true)) {
            dragging = false;
        }
        bottomSlider.setGradient(Colors.TRANSPARENT, color.getRGBNoAlpha());
        nanoVGHelper.drawRoundImage(vg, Images.ALPHA_GRID.filePath, x + 16, y + 456, 384, 16, 8f);
        bottomSlider.draw(vg, x + 16, y + 456, inputHandler);

        if (percentMoveMain > 0.96f) {
            nanoVGHelper.drawRoundedRect(vg, mouseX - 7, mouseY - 7, 14, 14, Colors.WHITE, 14f);
            nanoVGHelper.drawRoundedRect(vg, mouseX - 6, mouseY - 6, 12, 12, Colors.BLACK, 12f);
            nanoVGHelper.drawRoundedRect(vg, mouseX - 5, mouseY - 5, 10, 10, color.getRGBMax(true), 10f);
        }

        // deal with the input fields
        parseInputFields();
        if (guideBtn.isClicked()) NetworkUtils.browseLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ");

        // draw the color preview
        nanoVGHelper.drawHollowRoundRect(vg, x + 15, y + 487, 384, 40, Colors.GRAY_300, 12f, 2f);
        nanoVGHelper.drawRoundImage(vg, Images.ALPHA_GRID.filePath, x + 20, y + 492, 376, 32, 8f);
        nanoVGHelper.drawRoundedRect(vg, x + 20, y + 492, 376, 32, color.getRGB(), 8f);

        inputScissor = inputHandler.blockInputArea(x - 3, y - 3, width + 6, height + 6);
        scissorHelper.resetScissor(vg, scissor);
        mouseWasDown = Platform.getMousePlatform().isButtonDown(0);
        if (closeBtn.isClicked()) {
            OneConfigGui.INSTANCE.closeColorSelector();
        }
    }

    private void drawColorSelector(long vg, int mode, float x, float y) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        switch (mode) {
            default:
            case 0:
            case 2:
                //buttons.get(mode).colorAnimation.setPalette(ColorPalette.TERTIARY);
                topSlider.setImage(Images.HUE_GRADIENT.filePath);
                nanoVGHelper.drawHSBBox(vg, x + 16, y + 120, 384, 288, color.getRGBMax(true));

                if (mode == 0) {
                    topSlider.setColor(color.getRGBMax(true));
                    topSlider.draw(vg, x + 16, y + 424, inputHandler);
                }
                if (mode == 2) {
                    speedSlider.draw(vg, x + 60, y + 424, inputHandler);
                    nanoVGHelper.drawText(vg, "SLOW", x + 16, y + 429, Colors.WHITE_80, 12f, Fonts.REGULAR);
                    nanoVGHelper.drawText(vg, "FAST", x + 370, y + 429, Colors.WHITE_80, 12f, Fonts.REGULAR);
                }
                break;
            case 1:
                //buttons.get(1).colorAnimation.setPalette(ColorPalette.TERTIARY);
                topSlider.setImage(null);
                nanoVGHelper.drawRoundImage(vg, Images.COLOR_WHEEL.filePath, x + 64, y + 120, 288, 288, 144f);

                topSlider.setGradient(Colors.BLACK, color.getRGBMax(true));
                topSlider.setImage(null);
                topSlider.draw(vg, x + 16, y + 424, inputHandler);
                break;
        }
    }

    private void doDrag() {
        if (inputHandler.isAreaHovered(x, y, 368, 64) && Platform.getMousePlatform().isButtonDown(0) && !dragging) {
            float dx = (float) (Platform.getMousePlatform().getMouseDX() / (OneConfigGui.INSTANCE == null ? 1 : OneConfigGui.getScaleFactor()));
            float dy = (float) (Platform.getMousePlatform().getMouseDY() / (OneConfigGui.INSTANCE == null ? 1 : OneConfigGui.getScaleFactor()));
            x += dx;
            mouseX += dx;
            y -= dy;
            mouseY -= dy;
        }
    }

    private void setColorFromXY() {
        boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
        boolean hovered = Platform.getMousePlatform().isButtonDown(0) && inputHandler.isAreaHovered(x + 16, y + 120, 384, 288);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        switch (mode) {
            case 0:
            case 2:
                if (dragging) {
                    mouseX = inputHandler.mouseX();
                    mouseY = inputHandler.mouseY();
                }
                if (mouseX < x + 16) mouseX = x + 16;
                if (mouseY < y + 120) mouseY = y + 120;
                if (mouseX > x + 400) mouseX = x + 400;
                if (mouseY > y + 408) mouseY = y + 408;
                float progressX = (mouseX - x - 16f) / 384f;
                float progressY = Math.abs((mouseY - y - 120f) / 288f - 1f);
                color.setHSBA((int) topSlider.getValue(), Math.round(progressX * 100), Math.round(progressY * 100), (int) bottomSlider.getValue());
                if (mode == 2) {
                    if (!speedSlider.isDragging()) {
                        if (!speedInput.isToggled()) {
                            color.setChromaSpeed((int) Math.abs(speedSlider.getValue() - 31));
                            speedInput.setCurrentValue(color.getDataBit());
                        }
                    }
                }
                break;
            case 1:
                float circleCenterX = x + 208;
                float circleCenterY = y + 264;
                double squareDist = Math.pow((circleCenterX - inputHandler.mouseX()), 2) + Math.pow((circleCenterY - inputHandler.mouseY()), 2);
                hovered = squareDist < 144 * 144 && Platform.getMousePlatform().isButtonDown(0);
                isMouseDown = Platform.getMousePlatform().isButtonDown(0);
                if (hovered && isMouseDown && !mouseWasDown) dragging = true;

                int angle = 0;
                int saturation = color.getSaturation();
                if (dragging) {
                    angle = (int) Math.toDegrees(Math.atan2(inputHandler.mouseY() - circleCenterY, inputHandler.mouseX() - circleCenterX));
                    if (angle < 0) angle += 360;
                    if ((squareDist / (144 * 144) > 1f)) {
                        saturation = 100;
                        mouseX = (float) (Math.sin(Math.toRadians(-angle) + 1.5708) * 144 + x + 208);
                        mouseY = (float) (Math.cos(Math.toRadians(-angle) + 1.5708) * 144 + y + 264);
                    } else {
                        saturation = (int) (squareDist / (144 * 144) * 100);
                        mouseX = inputHandler.mouseX();
                        mouseY = inputHandler.mouseY();
                    }
                }
                color.setHSBA(dragging ? angle : color.getHue(), saturation, (int) (topSlider.getValue() / 360 * 100), (int) bottomSlider.getValue());
                break;
        }
    }

    private void setXYFromColor() {
        bottomSlider.setValue(color.getAlpha());
        if (mode == 1) {
            mouseX = (float) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
            mouseY = (float) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
            topSlider.setValue(color.getBrightness() / 100f * 360f);
        }
        if (mode == 0 || mode == 2) {
            topSlider.setValue(color.getHue());
            mouseX = (saturationInput.getCurrentValue() / 100f * 384 + x + 16);
            mouseY = (Math.abs(brightnessInput.getCurrentValue() / 100f - 1f) * 288 + y + 120);
        }
    }

    private void parseInputFields() {
        if (hueInput.isToggled() || saturationInput.isToggled() || brightnessInput.isToggled() || alphaInput.isToggled() || hueInput.arrowsClicked() || saturationInput.arrowsClicked() || brightnessInput.arrowsClicked() || alphaInput.arrowsClicked() || hexInput.isToggled() || pasteBtn.isClicked() || speedInput.isToggled()) {
            if (mode != 2 && !hexInput.isToggled()) {
                color.setHSBA((int) hueInput.getCurrentValue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
            }
            if (mode == 2) {
                color.setHSBA(color.getHue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
                color.setChromaSpeed((int) speedInput.getCurrentValue());
            }
            setXYFromColor();
        } else if (GuiUtils.wasMouseDown()) {
            saturationInput.setInput(String.format("%.01f", (float) color.getSaturation()));
            brightnessInput.setInput(String.format("%.01f", (float) color.getBrightness()));
            if (!alphaInput.arrowsClicked()) {
                alphaInput.setInput(String.format("%.01f", color.getAlpha() / 255f * 100f));
            }
            if (hexInput.isToggled()) return;
            if (mode != 2) {
                hueInput.setInput(String.format("%.01f", (float) color.getHue()));
                hexInput.setInput("#" + color.getHex());
            } else {
                speedInput.setInput(String.format("%.01f", (float) color.getDataBit()));
                hexInput.setInput("Z" + color.getDataBit());
            }

        }
        if (mode != 2 && !hexInput.isToggled()) {
            hexInput.setInput("#" + color.getHex());
        }
    }

    private void parseHex() {
        if (copyBtn.isClicked()) {
            IOUtils.copyStringToClipboard(color.getHex());
        }
        if (pasteBtn.isClicked() && mode != 2) {
            try {
                color.setColorFromHex(IOUtils.getStringFromClipboard());
                hexInput.setInput("#" + color.getHex());
            } catch (Exception ignored) {
            }
        }
        hexInput.setErrored(false);
        if ((hexInput.isToggled() || pasteBtn.isClicked()) && mode != 2) {
            try {
                color.setColorFromHex(hexInput.getInput());
            } catch (Exception e) {
                hexInput.setErrored(true);
                e.printStackTrace();
            }
            saturationInput.setInput(String.format("%.01f", (float) color.getSaturation()));
            brightnessInput.setInput(String.format("%.01f", (float) color.getBrightness()));
            alphaInput.setInput(String.format("%.01f", color.getAlpha() / 255f * 100f));
            hueInput.setInput(String.format("%.01f", (float) color.getHue()));
            if (mode == 0) topSlider.setValue(color.getHue());
            if (mode == 1) topSlider.setValue(color.getBrightness() / 100f * 360f);
        }
    }

    public OneColor getColor() {
        return color;
    }

    public void keyTyped(char typedChar, int keyCode) {
        hexInput.keyTyped(typedChar, keyCode);
        saturationInput.keyTyped(typedChar, keyCode);
        brightnessInput.keyTyped(typedChar, keyCode);
        alphaInput.keyTyped(typedChar, keyCode);
        hueInput.keyTyped(typedChar, keyCode);
        speedInput.keyTyped(typedChar, keyCode);
    }

    public void onClose() {
        if (inputScissor != null) inputHandler.stopBlock(inputScissor);
        /*for (int i = 0; i < OneConfigConfig.recentColors.size(); i++) {
            OneColor color1 = OneConfigConfig.recentColors.get(i);
            if (color1.getRGB() == color.getRGB()) {
                OneConfigConfig.recentColors.get(i).setFromOneColor(color1);
                return;
            }
        }
        OneConfigConfig.recentColors.add(color);*/
    }

    public void setFavorite(int index) {
        if (index < 0 || index >= OneConfigConfig.favoriteColors.size()) {
            return;
        }
        OneConfigConfig.favoriteColors.add(index, color);
        this.favoriteColors.add(index, new ColorBox(color));
        this.favoriteColors.get(index).setToggled(true);
    }

    public boolean isAlphaAllowed() {
        return hasAlpha;
    }

    private static class ColorSlider extends Slider {
        protected int gradColorStart, gradColorEnd;
        protected String image;
        protected int color;

        public ColorSlider(int length, float min, float max, float startValue) {
            super(length, min, max, startValue);
            super.height = 16;
            super.dragPointerSize = 0f;
        }

        @Override
        public void draw(long vg, float x, float y, InputHandler inputHandler) {
            NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

            if (!disabled) update(x, y, inputHandler);
            else nanoVGHelper.setAlpha(vg, 0.5f);
            super.dragPointerSize = 15f;
            if (image != null) {
                nanoVGHelper.drawRoundImage(vg, image, x + 1, y + 1, width - 2, height - 2, 8f);
            } else {
                nanoVGHelper.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f);
            }

            nanoVGHelper.drawHollowRoundRect(vg, x - 0.5f, y - 0.5f, width, height, new Color(204, 204, 204, 80).getRGB(), 8f, 1f);
            nanoVGHelper.drawHollowRoundRect(vg, currentDragPoint - 1, y - 1, 18, 18, Colors.WHITE, 9f, 1f);
            nanoVGHelper.drawHollowRoundRect(vg, currentDragPoint, y, 16, 16, Colors.BLACK, 8f, 1f);
            nanoVGHelper.drawRoundedRect(vg, currentDragPoint + 1.5f, y + 1.5f, 14, 14, color, 7f);
            nanoVGHelper.setAlpha(vg, 1f);
        }

        public void setGradient(int start, int end) {
            gradColorStart = start;
            gradColorEnd = end;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }

    private static class ColorBox extends BasicElement {
        protected OneColor color;

        public ColorBox(OneColor color) {
            super(32, 32, false);
            this.color = color;
        }

        @Override
        public void draw(long vg, float x, float y, InputHandler inputHandler) {
            NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
            nanoVGHelper.drawRoundedRect(vg, x, y, 32, 32, toggled ? Colors.PRIMARY_600 : Colors.GRAY_300, 12f);
            nanoVGHelper.drawRoundedRect(vg, x + 2, y + 2, 28, 28, Colors.GRAY_800, 10f);
            nanoVGHelper.drawRoundedRect(vg, x + 4, y + 4, 24, 24, color.getRGB(), 8f);
            update(x, y, inputHandler);
        }

        public OneColor getColor() {
            return color;
        }

        public void setColor(OneColor color) {
            this.color = color;
        }
    }
}
