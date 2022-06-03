package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.*;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.lwjgl.scissor.Scissor;
import cc.polyfrost.oneconfig.lwjgl.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

public class ColorSelector {
    private int x;
    private int y;
    private final OneColor color;
    private Animation barMoveAnimation = new DummyAnimation(18);
    private Animation barSizeAnimation = new DummyAnimation(124);
    private Animation moveAnimation = new DummyAnimation(1);
    private int mouseX, mouseY;
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
    private int mode = 0, prevMode = 0;
    private boolean dragging, mouseWasDown;


    public ColorSelector(OneColor color, int mouseX, int mouseY) {
        this.color = color;
        buttons.add(new BasicElement(124, 28, ColorPalette.SECONDARY, true, 10f));
        buttons.add(new BasicElement(124, 28, ColorPalette.SECONDARY, true, 10f));
        buttons.add(new BasicElement(124, 28, ColorPalette.SECONDARY, true, 10f));
        hueInput.setCurrentValue(color.getHue());
        saturationInput.setCurrentValue(color.getSaturation());
        brightnessInput.setCurrentValue(color.getBrightness());
        alphaInput.setCurrentValue(color.getAlpha() / 255f * 100f);
        speedSlider.setValue(color.getDataBit());
        topSlider.setValue(color.getHue());
        topSlider.setColor(color.getRGBMax(true));
        bottomSlider.setValue(color.getAlpha());
        hexInput.setInput(color.getHex());
        this.x = mouseX - 208;
        this.y = Math.max(0, mouseY - 776);
        if (color.getDataBit() != -1) mode = 2;
        if (mode == 0 || mode == 2) {
            this.mouseX = (int) (color.getSaturation() / 100f * 384 + x + 16);
            this.mouseY = (int) (Math.abs(color.getBrightness() / 100f - 1f) * 288 + y + 120);
        } else {
            topSlider.setValue(color.getBrightness() / 100f * 360f);
            this.mouseX = (int) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
            this.mouseY = (int) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
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

        topSlider.setImage(Images.HUE_GRADIENT);
    }

    public void draw(long vg) {
        InputUtils.blockClicks(false);
        doDrag();
        int width = 416;
        int height = 768;
        Scissor scissor = ScissorManager.scissor(vg, x - 3, y - 3, width + 6, height + 6);
        RenderManager.drawHollowRoundRect(vg, x - 3, y - 3, width + 4, height + 4, new Color(204, 204, 204, 77).getRGB(), 20f, 2f);
        RenderManager.drawRoundedRect(vg, x, y, width, height, OneConfigConfig.GRAY_800, 20f);
        RenderManager.drawText(vg, "Color Selector", x + 16, y + 32, OneConfigConfig.WHITE_90, 18f, Fonts.SEMIBOLD);
        if (!closeBtn.isHovered()) RenderManager.setAlpha(vg, 0.8f);
        closeBtn.draw(vg, x + 368, y + 16);
        RenderManager.drawSvg(vg, SVGs.X_CIRCLE_BOLD, x + 368, y + 16, 32, 32, closeBtn.isHovered() ? OneConfigConfig.ERROR_600 : -1);
        RenderManager.setAlpha(vg, 1f);

        // hex parser
        if(hexInput.isToggled()) {
            parseHex();
        }

        // TODO favorite stuff
        faveBtn.draw(vg, x + 16, y + 672);
        recentBtn.draw(vg, x + 16, y + 720);
        for (int i = 0; i < 7; i++) {
            favoriteColors.get(i).draw(vg, x + 104 + i * 44, y + 672);
        }
        for (int i = 0; i < 7; i++) {
            recentColors.get(i).draw(vg, x + 104 + i * 44, y + 720);
        }

        int i = 18;
        for (BasicElement button : buttons) {
            button.draw(vg, x + i, y + 66);
            if (button.isClicked()) {
                prevMode = mode;
                mode = buttons.indexOf(button);
                setXYFromColor();
                barMoveAnimation = new EaseInOutQuart(200, 18 + prevMode * 128, 18 + mode * 128, false);
                barSizeAnimation = new EaseInQuartReversed(200, 124, 186, false);
                moveAnimation = new EaseInOutQuad(200, 0, 1, false);
            }
            i += 128;
        }
        float percentMoveMain = moveAnimation.get();

        RenderManager.drawRoundedRect(vg, x + 16, y + 64, 384, 32, OneConfigConfig.GRAY_500, 12f);
        RenderManager.drawRoundedRect(vg, x + barMoveAnimation.get(), y + 66, barSizeAnimation.get(), 28, OneConfigConfig.PRIMARY_600, 10f);

        RenderManager.drawText(vg, "HSB Box", x + 55, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        RenderManager.drawText(vg, "Color Wheel", x + 172.5f, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        RenderManager.drawText(vg, "Chroma", x + 313, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);

        RenderManager.drawText(vg, "Saturation", x + 224, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        saturationInput.draw(vg, x + 312, y + 544);
        RenderManager.drawText(vg, "Brightness", x + 16, y + 599, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        brightnessInput.draw(vg, x + 104, y + 584);
        RenderManager.drawText(vg, "Alpha (%)", x + 224, y + 599, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        alphaInput.draw(vg, x + 312, y + 584);
        RenderManager.drawText(vg, color.getDataBit() == -1 ? "Hex (RGB):" : "Color Code:", x + 16, y + 641, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        hexInput.draw(vg, x + 104, y + 624);

        copyBtn.draw(vg, x + 204, y + 624);
        pasteBtn.draw(vg, x + 244, y + 624);
        if(mode != 2) {
            RenderManager.drawText(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
            hueInput.draw(vg, x + 104, y + 544);
        } else {
            RenderManager.drawText(vg, "Speed (s)", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
            speedInput.draw(vg, x + 104, y + 544);
        }

        guideBtn.draw(vg, x + 288, y + 624);


        setColorFromXY();
        if (mode != 2) color.setChromaSpeed(-1);
        drawColorSelector(vg, mode, (int) (x * percentMoveMain), y);
        if (dragging && InputUtils.isClicked(true)) {
            dragging = false;
        }
        bottomSlider.setGradient(OneConfigConfig.TRANSPARENT, color.getRGBNoAlpha());
        RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x + 16, y + 456, 384, 16, 8f);
        bottomSlider.draw(vg, x + 16, y + 456);

        if(percentMoveMain > 0.96f) {
            RenderManager.drawRoundedRect(vg, mouseX - 7, mouseY - 7, 14, 14, OneConfigConfig.WHITE, 14f);
            RenderManager.drawRoundedRect(vg, mouseX - 6, mouseY - 6, 12, 12, OneConfigConfig.BLACK, 12f);
            RenderManager.drawRoundedRect(vg, mouseX - 5, mouseY - 5, 10, 10, color.getRGBMax(true), 10f);
        }

        // deal with the input fields
        parseInputFields();
        if (guideBtn.isClicked()) NetworkUtils.browseLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ");


        // draw the color preview
        RenderManager.drawHollowRoundRect(vg, x + 15, y + 487, 384, 40, OneConfigConfig.GRAY_300, 12f, 2f);
        RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x + 20, y + 492, 376, 32, 8f);
        RenderManager.drawRoundedRect(vg, x + 20, y + 492, 376, 32, color.getRGB(), 8f);
        InputUtils.blockClicks(true);
        if (closeBtn.isClicked()) {
            OneConfigGui.INSTANCE.closeColorSelector();
        }
        ScissorManager.resetScissor(vg, scissor);
    }

    private void drawColorSelector(long vg, int mode, int x, int y) {
        switch (mode) {
            default:
            case 0:
            case 2:
                //buttons.get(mode).currentColor = OneConfigConfig.TRANSPARENT;
                topSlider.setImage(Images.HUE_GRADIENT);
                RenderManager.drawHSBBox(vg, x + 16, y + 120, 384, 288, color.getRGBMax(true));

                if (mode == 0) {
                    topSlider.setColor(color.getRGBMax(true));
                    topSlider.draw(vg, x + 16, y + 424);
                }
                if (mode == 2) {
                    speedSlider.draw(vg, x + 60, y + 424);
                    RenderManager.drawText(vg, "SLOW", x + 16, y + 429, OneConfigConfig.WHITE_80, 12f, Fonts.REGULAR);
                    RenderManager.drawText(vg, "FAST", x + 370, y + 429, OneConfigConfig.WHITE_80, 12f, Fonts.REGULAR);
                }
                break;
            case 1:
                //buttons.get(1).currentColor = OneConfigConfig.TRANSPARENT;
                topSlider.setImage(null);
                RenderManager.drawRoundImage(vg, Images.COLOR_WHEEL, x + 64, y + 120, 288, 288, 144f);

                topSlider.setGradient(OneConfigConfig.BLACK, color.getRGBMax(true));
                topSlider.setImage(null);
                topSlider.draw(vg, x + 16, y + 424);
                break;
        }
    }

    private void doDrag() {
        if (InputUtils.isAreaHovered(x, y, 368, 64) && Mouse.isButtonDown(0) && !dragging) {
            int dx = Mouse.getDX();
            int dy = Mouse.getDY();
            x += dx;
            mouseX += dx;
            y -= dy;
            mouseY -= dy;
        }
    }

    private void setColorFromXY() {
        boolean isMouseDown = Mouse.isButtonDown(0);
        boolean hovered = Mouse.isButtonDown(0) && InputUtils.isAreaHovered(x + 16, y + 120, 384, 288);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        mouseWasDown = isMouseDown;
        switch (mode) {
            case 0:
            case 2:
                if (dragging) {
                    mouseX = InputUtils.mouseX();
                    mouseY = InputUtils.mouseY();
                }
                if (mouseX < x + 16) mouseX = x + 16;
                if (mouseY < y + 120) mouseY = y + 120;
                if (mouseX > x + 400) mouseX = x + 400;
                if (mouseY > y + 408) mouseY = y + 408;
                float progressX = (mouseX - x - 16f) / 384f;
                float progressY = Math.abs((mouseY - y - 120f) / 288f - 1f);
                color.setHSBA((int) topSlider.getValue(), Math.round(progressX * 100), Math.round(progressY * 100), (int) bottomSlider.getValue());
                if(mode == 2) {
                    if (!speedSlider.isDragging()) {
                        if(!speedInput.isToggled()) {
                            color.setChromaSpeed((int) Math.abs(speedSlider.getValue() - 31));
                            speedInput.setCurrentValue(color.getDataBit());
                        }
                    }
                }
                break;
            case 1:
                int circleCenterX = x + 208;
                int circleCenterY = y + 264;
                double squareDist = Math.pow((circleCenterX - InputUtils.mouseX()), 2) + Math.pow((circleCenterY - InputUtils.mouseY()), 2);
                hovered = squareDist < 144 * 144 && Mouse.isButtonDown(0);
                isMouseDown = Mouse.isButtonDown(0);
                if (hovered && isMouseDown && !mouseWasDown) dragging = true;
                mouseWasDown = isMouseDown;

                int angle = 0;
                int saturation = color.getSaturation();
                if (dragging) {
                    angle = (int) Math.toDegrees(Math.atan2(InputUtils.mouseY() - circleCenterY, InputUtils.mouseX() - circleCenterX));
                    if (angle < 0) angle += 360;
                    if ((squareDist / (144 * 144) > 1f)) {
                        saturation = 100;
                        mouseX = (int) (Math.sin(Math.toRadians(-angle) + 1.5708) * 144 + x + 208);
                        mouseY = (int) (Math.cos(Math.toRadians(-angle) + 1.5708) * 144 + y + 264);
                    } else {
                        saturation = (int) (squareDist / (144 * 144) * 100);
                        mouseX = InputUtils.mouseX();
                        mouseY = InputUtils.mouseY();
                    }
                }
                color.setHSBA(dragging ? angle : color.getHue(), saturation, (int) (topSlider.getValue() / 360 * 100), (int) bottomSlider.getValue());
                break;
        }
    }

    private void setXYFromColor() {
        bottomSlider.setValue(color.getAlpha());
        if (mode == 1) {
            mouseX = (int) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
            mouseY = (int) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
            topSlider.setValue(color.getBrightness() / 100f * 360f);
        }
        if (mode == 0 || mode == 2) {
            topSlider.setValue(color.getHue());
            mouseX = (int) (saturationInput.getCurrentValue() / 100f * 384 + x + 16);
            mouseY = (int) (Math.abs(brightnessInput.getCurrentValue() / 100f - 1f) * 288 + y + 120);
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
        } else if (OneConfigGui.INSTANCE.mouseDown) {
            saturationInput.setInput(String.format("%.01f", (float) color.getSaturation()));
            brightnessInput.setInput(String.format("%.01f", (float) color.getBrightness()));
            if(!alphaInput.arrowsClicked()) {
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
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(color.getHex()), null);
        }
        if (pasteBtn.isClicked() && mode != 2) {
            try {
                color.setColorFromHex(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor).toString());
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
        InputUtils.blockClicks(false);
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


    private static class ColorSlider extends Slider {
        protected int gradColorStart, gradColorEnd;
        protected Images image;
        protected int color;

        public ColorSlider(int length, float min, float max, float startValue) {
            super(length, min, max, startValue);
            super.height = 16;
            super.dragPointerSize = 0f;
        }

        @Override
        public void draw(long vg, int x, int y) {
            update(x, y);
            super.dragPointerSize = 15f;
            if (image != null) {
                RenderManager.drawRoundImage(vg, image, x + 1, y + 1, width - 2, height - 2, 8f);
            } else {
                RenderManager.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f);
            }

            RenderManager.drawHollowRoundRect(vg, x - 0.5f, y - 0.5f, width, height, new Color(204, 204, 204, 80).getRGB(), 8f, 1f);
            RenderManager.drawHollowRoundRect(vg, currentDragPoint - 1, y - 1, 18, 18, OneConfigConfig.WHITE, 9f, 1f);
            RenderManager.drawHollowRoundRect(vg, currentDragPoint, y, 16, 16, OneConfigConfig.BLACK, 8f, 1f);
                RenderManager.drawRoundedRect(vg, currentDragPoint + 1.5f, y + 1.5f, 14, 14, color, 7f);
        }

        public void setGradient(int start, int end) {
            gradColorStart = start;
            gradColorEnd = end;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public void setImage(Images image) {
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
        public void draw(long vg, int x, int y) {
            RenderManager.drawRoundedRect(vg, x, y, 32, 32, toggled ? OneConfigConfig.PRIMARY_600 : OneConfigConfig.GRAY_300, 12f);
            RenderManager.drawRoundedRect(vg, x + 2, y + 2, 28, 28, OneConfigConfig.GRAY_800, 10f);
            RenderManager.drawRoundedRect(vg, x + 4, y + 4, 24, 24, color.getRGB(), 8f);
            update(x, y);
        }

        public void setColor(OneColor color) {
            this.color = color;
        }

        public OneColor getColor() {
            return color;
        }
    }
}

