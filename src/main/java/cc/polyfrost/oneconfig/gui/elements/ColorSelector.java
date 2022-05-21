package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.OneColor;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

public class ColorSelector {
    private final int x;
    private final int y;
    private OneColor color;
    private float percentMove = 0f;
    private int mouseX, mouseY;
    private final ArrayList<BasicElement> buttons = new ArrayList<>();
    private final BasicElement closeBtn = new BasicElement(32, 32, true);

    private final BasicElement copyBtn = new BasicElement(32, 32, 2, true);
    private final BasicElement pasteBtn = new BasicElement(32, 32, 2, true);
    private final BasicButton guideBtn = new BasicButton(112, 32, "Guide", null, null, 0, BasicButton.ALIGNMENT_CENTER);

    private final NumberInputField hueInput = new NumberInputField(90, 32, 0, 0, 360, 1);
    private final NumberInputField saturationInput = new NumberInputField(90, 32, 100, 0, 100, 1);
    private final NumberInputField brightnessInput = new NumberInputField(90, 32, 100, 0, 100, 1);
    private final NumberInputField alphaInput = new NumberInputField(90, 32, 0, 0, 100, 1);
    private final TextInputField hexInput = new TextInputField(88, 32, true, "");
    private final ArrayList<ColorBox> favoriteColors = new ArrayList<>();
    private final ArrayList<ColorBox> recentColors = new ArrayList<>();

    private final ColorSlider topSlider = new ColorSlider(384, 0, 360, 127);
    private final ColorSlider bottomSlider = new ColorSlider(384, 0, 100, 100);
    private final Slider speedSlider = new Slider(296, 1, 30, 20);
    private int mode = 0;
    private boolean dragging, mouseWasDown;


    public ColorSelector(OneColor color, int mouseX, int mouseY) {
        this.color = color;
        buttons.add(new BasicElement(124, 28, 2, true, 10f));
        buttons.add(new BasicElement(124, 28, 2, true, 10f));
        buttons.add(new BasicElement(124, 28, 2, true, 10f));
        hueInput.setCurrentValue(color.getHue());
        saturationInput.setCurrentValue(color.getSaturation());
        brightnessInput.setCurrentValue(color.getBrightness());
        alphaInput.setCurrentValue(color.getAlpha() / 255f * 100f);
        speedSlider.setValue(color.getChroma());
        topSlider.setValue(color.getHue());
        topSlider.setColor(color);
        bottomSlider.setValue(color.getAlpha() / 255f * 100f);
        this.x = mouseX - 208;
        this.y = Math.max(0, mouseY - 776);
        for(OneColor color1 : OneConfigConfig.recentColors) {
            recentColors.add(new ColorBox(color1));
        }
        for(OneColor color1 : OneConfigConfig.favoriteColors) {
            favoriteColors.add(new ColorBox(color1));
        }

        topSlider.setImage(Images.HUE_GRADIENT);
    }

    public void draw(long vg) {
        int width = 416;
        int height = 768;

        RenderManager.drawHollowRoundRect(vg, x - 3, y - 3, width + 4, height + 4, new Color(204, 204, 204, 77).getRGB(), 20f, 2f);
        RenderManager.drawRoundedRect(vg, x, y, width, height, OneConfigConfig.GRAY_800, 20f);
        RenderManager.drawString(vg, "Color Selector", x + 16, y + 32, OneConfigConfig.WHITE_90, 18f, Fonts.SEMIBOLD);
        closeBtn.draw(vg, x + 368, y + 16);
        RenderManager.drawSvg(vg, SVGs.X_CIRCLE, x + 369, y + 17, 32, 32);
        if (closeBtn.isClicked()) {
            OneConfigGui.INSTANCE.closeColorSelector();
        }

        // hex parser
        if(copyBtn.isClicked()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(color.getHex()), null);
        }
        if(pasteBtn.isClicked()) {
            try {
                color.setColorFromHex(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor).toString());
            } catch (Exception ignored) {
            }
        }
        hexInput.setErrored(false);
        if(hexInput.isToggled()) {          // TODO fix this
            try {
                color.setColorFromHex(hexInput.getInput());
            } catch (Exception e) {
                hexInput.setErrored(true);
                e.printStackTrace();
            }
        }

        // TODO favorite stuff

        RenderManager.drawRoundedRect(vg, x + 16, y + 64, 384, 32, OneConfigConfig.GRAY_500, 12f);
        RenderManager.drawRoundedRect(vg, x + 18 + (percentMove * 128), y + 66, 124, 28, OneConfigConfig.PRIMARY_600, 10f);
        int i = 18;
        for(BasicElement button : buttons) {
            button.draw(vg, x + i, y + 66);
            if(button.isClicked()) {
                mode = buttons.indexOf(button);
                if(mode == 1) {
                    mouseX = (int) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
                    mouseY = (int) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
                    topSlider.setValue(color.getBrightness() / 100f * 360f);
                }
                if(mode == 0 || mode == 2) {
                    topSlider.setValue(color.getHue());
                    mouseX = (int) (saturationInput.getCurrentValue() / 100f * 384 + x + 16);
                    mouseY = (int) (Math.abs(brightnessInput.getCurrentValue() / 100f - 1f) * 288 + y + 120);
                }
            }
            if(percentMove != mode) {
                button.currentColor = OneConfigConfig.TRANSPARENT;
            }
            i += 128;
        }
        percentMove = MathUtils.easeOut(percentMove, mode, 100f);

        RenderManager.drawString(vg, "HSB Box", x + 55, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        RenderManager.drawString(vg, "Color Wheel", x + 172.5f, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        RenderManager.drawString(vg, "Chroma", x + 313, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);

        RenderManager.drawString(vg, "Saturation", x + 224, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        saturationInput.draw(vg, x + 312, y + 544);
        RenderManager.drawString(vg, "Brightness", x + 16, y + 599, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        brightnessInput.draw(vg, x + 104, y + 584);
        RenderManager.drawString(vg, "Alpha (%)", x + 224, y + 599, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        alphaInput.draw(vg, x + 312, y + 584);
        RenderManager.drawString(vg, color.getChroma() == -1 ? "Hex (RGB)" : "Chroma Speed", x + 16, y + 641, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        hexInput.draw(vg, x + 104, y + 624);

        copyBtn.draw(vg, x + 204, y + 624);
        pasteBtn.draw(vg, x + 244, y + 624);
        RenderManager.drawSvg(vg, SVGs.COPY, x + 211, y + 631, 18, 18);
        RenderManager.drawSvg(vg, SVGs.PASTE, x + 251, y + 631, 18, 18);

        guideBtn.draw(vg, x + 288, y + 624);
        RenderManager.drawSvg(vg, SVGs.HELP_CIRCLE, x + 301, y + 631, 18, 18);
        RenderManager.drawSvg(vg, SVGs.POP_OUT, x + 369, y + 631, 18, 18);


        boolean isMouseDown = Mouse.isButtonDown(0);
        boolean hovered = Mouse.isButtonDown(0) && InputUtils.isAreaHovered(x + 16, y + 120, 384, 288);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        mouseWasDown = isMouseDown;
        if(mode != 2) color.setChromaSpeed(-1);
        switch (mode) {
            default:
            case 0:
            case 2:
                buttons.get(mode).currentColor = OneConfigConfig.TRANSPARENT;
                topSlider.setImage(Images.HUE_GRADIENT);
                RenderManager.drawHSBBox(vg, x + 16, y + 120, 384, 288, color.getRGBMax(false));
                if(dragging) {
                    mouseX = InputUtils.mouseX();
                    mouseY = InputUtils.mouseY();
                }
                if(mouseX < x + 16) mouseX = x + 16;
                if(mouseY < y + 120) mouseY = y + 120;
                if(mouseX > x + 400) mouseX = x + 400;
                if(mouseY > y + 408) mouseY = y + 408;
                float progressX = (mouseX - x - 16f) / 384f;
                float progressY = Math.abs((mouseY - y - 120f) / 288f - 1f);
                color.setHSBA((int) topSlider.getValue(), Math.round(progressX * 100), Math.round(progressY * 100), (int) ((bottomSlider.getValue() / 100f) * 255));
                if(mode == 0) {
                    topSlider.setColor(color);
                    topSlider.draw(vg, x + 16, y + 424);
                    RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                    hueInput.draw(vg, x + 104, y + 544);
                }
                if(mode == 2) {
                    speedSlider.draw(vg, x + 60, y + 424);
                    RenderManager.drawString(vg, "SLOW", x + 16, y + 429, OneConfigConfig.WHITE_80, 12f, Fonts.REGULAR);
                    RenderManager.drawString(vg, "FAST", x + 370, y + 429, OneConfigConfig.WHITE_80, 12f, Fonts.REGULAR);
                    RenderManager.drawString(vg, "Speed (s)", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                    hueInput.draw(vg, x + 104, y + 544);
                    color.setChromaSpeed((int) speedSlider.getValue());
                }
                break;
            case 1:
                buttons.get(1).currentColor = OneConfigConfig.TRANSPARENT;
                topSlider.setImage(null);
                RenderManager.drawRoundImage(vg, Images.COLOR_WHEEL, x + 64, y + 120, 288, 288, 144f);
                int circleCenterX = x + 208;
                int circleCenterY = y + 264;
                double squareDist = Math.pow((circleCenterX - InputUtils.mouseX()), 2) + Math.pow((circleCenterY - InputUtils.mouseY()), 2);
                hovered = squareDist < 144 * 144 && Mouse.isButtonDown(0);
                isMouseDown = Mouse.isButtonDown(0);
                if (hovered && isMouseDown && !mouseWasDown) dragging = true;
                mouseWasDown = isMouseDown;

                int angle = 0;
                if(dragging) {
                    //if(!(squareDist / (144 * 144) > 1f)
                    mouseX = InputUtils.mouseX();
                    mouseY = InputUtils.mouseY();
                    angle = (int) Math.toDegrees(Math.atan2(mouseY - circleCenterY, mouseX - circleCenterX));
                    if(angle < 0) angle += 360;
                }
                color.setHSBA(dragging ? angle : color.getHue(), dragging ? (int) (squareDist / (144 * 144) * 100) : color.getSaturation(), (int) (topSlider.getValue() / 360 * 100), (int) ((bottomSlider.getValue() / 100f) * 255));
                topSlider.setGradient(OneConfigConfig.BLACK, color.getRGBMax(true));
                topSlider.setImage(null);
                RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                hueInput.draw(vg, x + 104, y + 544);
                topSlider.draw(vg, x + 16, y + 424);
                break;
        }
        if(dragging && InputUtils.isClicked()) {
            dragging = false;
        }
        bottomSlider.setGradient(OneConfigConfig.TRANSPARENT_25, color.getRGBNoAlpha());
        RenderManager.drawImage(vg, Images.ALPHA_GRID, x + 16, y + 456, 384, 16);
        bottomSlider.draw(vg, x + 16, y + 456);

        RenderManager.drawRoundedRect(vg, mouseX - 6, mouseY - 6, 12, 12, OneConfigConfig.WHITE, 12f);
        RenderManager.drawRoundedRect(vg, mouseX - 5, mouseY - 5, 10, 10, color.getRGBNoAlpha(), 10f);

        // deal with the input fields
        if(hueInput.isToggled() || saturationInput.isToggled() || brightnessInput.isToggled() || alphaInput.isToggled() || hueInput.arrowsClicked() || saturationInput.arrowsClicked() || brightnessInput.arrowsClicked() || alphaInput.arrowsClicked()) {
            if(mode != 2) {
                color.setHSBA((int) hueInput.getCurrentValue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
            }
            if(mode == 2) {
                color.setHSBA(color.getHue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
                color.setChromaSpeed((int) (hueInput.getCurrentValue() / 360f * 30f));
                speedSlider.setValue(hueInput.getCurrentValue() / 360f * 30f);
            }
            bottomSlider.setValue(color.getAlpha() / 255f * 100f);
            if(mode == 0 || mode == 2) {
                mouseX = (int) (saturationInput.getCurrentValue() / 100f * 384 + x + 16);
                mouseY = (int) (Math.abs(brightnessInput.getCurrentValue() / 100f - 1f) * 288 + y + 120);
            } else {
                topSlider.setValue(color.getBrightness() / 100f * 360f);
                mouseX = (int) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
                mouseY = (int) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
            }
            if(mode == 0) {
                topSlider.setValue(color.getHue());
            }
        }
        else if(OneConfigGui.INSTANCE.mouseDown) {
            if(mode != 2) {
                hueInput.setInput(String.format("%.01f", (float) color.getHue()));
                hexInput.setInput("#" + color.getHex());
            } else {
                hueInput.setInput(String.format("%.01f", (float) color.getChroma()));
                hexInput.setInput("Z" + color.getChroma());
            }
            saturationInput.setInput(String.format("%.01f", (float) color.getSaturation()));
            brightnessInput.setInput(String.format("%.01f", (float) color.getBrightness()));
            alphaInput.setInput(String.format("%.01f", color.getAlpha() / 255f * 100f));
        }
        if(mode != 2 && !hexInput.isToggled()) {
            hueInput.setInput(String.format("%.01f", (float) color.getHue()));
            hexInput.setInput("#" + color.getHex());
        }


        // draw the color preview
        RenderManager.drawHollowRoundRect(vg, x + 15, y + 487, 384, 40, OneConfigConfig.GRAY_300, 12f, 2f);
        RenderManager.drawImage(vg, Images.ALPHA_GRID, x + 20, y + 492, 376, 32);
        RenderManager.drawRoundedRect(vg, x + 20, y + 492, 376, 32, color.getRGB(), 8f);
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
    }

    public void onClose() {
        for(int i = 0; i < OneConfigConfig.recentColors.size(); i++) {
            OneColor color1 = OneConfigConfig.recentColors.get(i);
            if(color1.getRGB() == color.getRGB()) {
                OneConfigConfig.recentColors.get(i).setFromOneColor(color1);
                return;
            }
        }
        OneConfigConfig.recentColors.add(color);
    }

    public void setFavorite(int index) {
        if(index < 0 || index >= OneConfigConfig.favoriteColors.size()) {
            return;
        }
        OneConfigConfig.favoriteColors.add(index, color);
        this.favoriteColors.add(index, new ColorBox(color));
        this.favoriteColors.get(index).setToggled(true);
    }


    private static class ColorSlider extends Slider {
        protected int gradColorStart, gradColorEnd;
        protected Images image;
        protected OneColor color;

        public ColorSlider(int length, float min, float max, float startValue) {
            super(length, min, max, startValue);
            super.height = 16;
            super.dragPointerSize = 0f;
        }

        @Override
        public void draw(long vg, int x, int y) {
            update(x, y);
            super.dragPointerSize = 15f;
            if(image != null) {
                RenderManager.drawRoundImage(vg, image, x, y, width, height, 8f);
            } else {
                RenderManager.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f);
            }

            RenderManager.drawHollowRoundRect(vg, x - 1.5f, y - 1.5f, width + 2, height + 2, new Color(204, 204, 204, 77).getRGB(), 8f, 1f);
            RenderManager.drawHollowRoundRect(vg, currentDragPoint - 2, y - 2, 18, 18, OneConfigConfig.WHITE, 7f, 1f);
            if(color != null) {
                RenderManager.drawRoundedRect(vg, currentDragPoint, y, 15, 15, color.getRGBNoAlpha(), 7.5f);
            }
        }

        public void setGradient(int start, int end) {
            gradColorStart = start;
            gradColorEnd = end;
        }

        public void setColor(OneColor color) {
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
            RenderManager.drawRoundedRect(vg, x + 2, y + 2, 28, 28, OneConfigConfig.GRAY_800, 8f);
            RenderManager.drawRoundedRect(vg, x + 4, y + 4, 24, 24, color.getRGB(), 8f);
        }

        public void setColor(OneColor color) {
            this.color = color;
        }

        public OneColor getColor() {
            return color;
        }
    }
}

