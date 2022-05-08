package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.OneColor;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

public class ColorSelector {
    private final int x;
    private final int y;
    private OneColor color;
    private float percentMove = 0f;
    private int mouseX, mouseY;
    private final BasicElement hsbBtn = new BasicElement(124, 28, 2, true);
    private final BasicElement rgbBtn = new BasicElement(124, 28, 2, true);
    private final BasicElement chromaBtn = new BasicElement(124, 28, 2, true);
    private final BasicElement closeBtn = new BasicElement(32, 32, true);

    private final BasicElement copyBtn = new BasicElement(32, 32, 2, true);
    private final BasicElement pasteBtn = new BasicElement(32, 32, 2, true);
    private final BasicButton guideBtn = new BasicButton(112, 32, "Guide", null, null, 0, BasicButton.ALIGNMENT_CENTER);

    private final NumberInputField hueInput = new NumberInputField(90, 32, 0, 0, 360, 1);
    private final NumberInputField saturationInput = new NumberInputField(90, 32, 100, 0, 100, 1);
    private final NumberInputField brightnessInput = new NumberInputField(90, 32, 100, 0, 100, 1);
    private final NumberInputField alphaInput = new NumberInputField(90, 32, 0, 100, 100, 1);
    private final TextInputField hexInput = new TextInputField(88, 32, true, "");

    private final ColorSlider topSlider = new ColorSlider(384, 0, 360, 127);
    private final ColorSlider bottomSlider = new ColorSlider(384, 0, 100, 100);
    private final Slider speedSlider = new Slider(384, 1, 60, 20);


    public ColorSelector(OneColor color, int mouseX, int mouseY) {
        this.color = color;
        this.x = mouseX - 208;
        this.y = mouseY - 776;
    }

    public void draw(long vg) {
        int width = 416;
        int height = 768;
        int mode = 0;

        RenderManager.drawHollowRoundRect(vg, x - 3, y - 3, width + 4, height + 4, new Color(204, 204, 204, 77).getRGB(), 20f, 2f);
        RenderManager.drawRoundedRect(vg, x, y, width, height, OneConfigConfig.GRAY_800, 20f);
        RenderManager.drawString(vg, "Color Selector", x + 16, y + 32, OneConfigConfig.WHITE_90, 18f, Fonts.SEMIBOLD);
        closeBtn.draw(vg, x + 368, y + 16);
        RenderManager.drawImage(vg, Images.CLOSE_COLOR, x + 369, y + 17, 32, 32);
        if (closeBtn.isClicked()) {
            OneConfigGui.INSTANCE.closeColorSelector();
        }

        RenderManager.drawRoundedRect(vg, x + 16, y + 64, 384, 32, OneConfigConfig.GRAY_500, 12f);
        RenderManager.drawRoundedRect(vg, x + 18 + (percentMove * 128), y + 66, 124, 28, OneConfigConfig.BLUE_600, 10f);
        percentMove = MathUtils.easeOut(percentMove, mode, 20f);
        hsbBtn.draw(vg, x + 18, y + 66);
        rgbBtn.draw(vg, x + 146, y + 66);
        chromaBtn.draw(vg, x + 274, y + 66);
        RenderManager.drawString(vg, "HSB Box", x + 55, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        RenderManager.drawString(vg, "Color Wheel", x + 165, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        RenderManager.drawString(vg, "Chroma", x + 307, y + 81, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);

        RenderManager.drawString(vg, "Saturation", x + 224, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        saturationInput.draw(vg, x + 312, y + 544);
        RenderManager.drawString(vg, "Brightness", x + 16, y + 599, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        brightnessInput.draw(vg, x + 104, y + 584);
        RenderManager.drawString(vg, "Alpha (%)", x + 224, y + 599, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        alphaInput.draw(vg, x + 312, y + 584);
        RenderManager.drawString(vg, "Hex (ARGB)", x + 16, y + 641, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        hexInput.draw(vg, x + 104, y + 624);

        copyBtn.draw(vg, x + 204, y + 624);
        pasteBtn.draw(vg, x + 244, y + 624);
        RenderManager.drawImage(vg, Images.COPY, x + 211, y + 631, 18, 18);
        RenderManager.drawImage(vg, Images.PASTE, x + 251, y + 631, 18, 18);

        guideBtn.draw(vg, x + 288, y + 624);
        RenderManager.drawImage(vg, Images.HELP, x + 301, y + 631, 18, 18);
        RenderManager.drawImage(vg, Images.LAUNCH, x + 369, y + 631, 18, 18);



        switch (mode) {
            default:
            case 0:
                if(mouseX < x + 16 || mouseY < y + 120){
                    mouseX = x + 16;
                    mouseY = y + 120;
                }
                boolean drag = Mouse.isButtonDown(0) && InputUtils.isAreaHovered(x + 16, y + 120, 384, 288);
                if(drag) {
                    mouseX = InputUtils.mouseX();
                    mouseY = InputUtils.mouseY();
                }
                float progressX = (mouseX - x - 16f) / 384f;
                float progressY = Math.abs((mouseY - y - 120f) / 288f - 1f);
                RenderManager.drawHSBBox(vg, x + 16, y + 120, 384, 288, color.getRGBMax());
                RenderManager.drawRoundedRect(vg, mouseX - 6, mouseY - 6, 12, 12, OneConfigConfig.BLUE_600, 12f);

                topSlider.setImage(Images.HUE_GRADIENT);
                color.setHSBA((int) topSlider.getValue(), Math.round(progressX * 100), Math.round(progressY * 100), (int) ((bottomSlider.getValue() / 100f) * 255));

                topSlider.draw(vg, x + 16, y + 424);

                RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                hueInput.draw(vg, x + 104, y + 544);
                bottomSlider.setGradient(OneConfigConfig.TRANSPARENT_25, color.getRGBNoAlpha());
                RenderManager.drawImage(vg, Images.COLOR_BASE_LONG, x + 16, y + 456, 384, 16);
                bottomSlider.draw(vg, x + 16, y + 456);
                break;
            case 1:
                RenderManager.drawRoundedRect(vg, x + 64, y + 120, 288, 288, OneConfigConfig.WHITE, 144f);

                RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                hueInput.draw(vg, x + 104, y + 544);



                //RenderManager.drawRoundedRect(vg, bottomSlider.currentDragPoint - 8, y + 456, 16, 16, color.getRGB(), 16f);

                topSlider.draw(vg, x + 16, y + 424);
                RenderManager.drawImage(vg, Images.COLOR_BASE_LONG, x + 16, y + 456, 384, 16);
                bottomSlider.draw(vg, x + 16, y + 456);
                break;
            case 2:
                break;
        }

        if(hueInput.isToggled() || saturationInput.isToggled() || brightnessInput.isToggled() || alphaInput.isToggled() || hueInput.arrowsClicked() || saturationInput.arrowsClicked() || brightnessInput.arrowsClicked() || alphaInput.arrowsClicked()) {
            color.setHSBA((int) hueInput.getCurrentValue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
            topSlider.setValue(color.getHue());
            bottomSlider.setValue(color.getAlpha() / 255f * 100f);
        }
        else if(OneConfigGui.INSTANCE.mouseDown) {
            hueInput.setInput(String.valueOf(color.getHue()));
            saturationInput.setInput(String.valueOf(color.getSaturation()));
            brightnessInput.setInput(String.valueOf(color.getBrightness()));
            alphaInput.setInput(String.format("%.01f", color.getAlpha() / 255f * 100f));
            hexInput.setInput(color.getHex());
        }


        RenderManager.drawHollowRoundRect(vg, x + 15, y + 487, 384, 40, OneConfigConfig.GRAY_300, 12f, 2f);
        RenderManager.drawImage(vg, Images.COLOR_BASE_LARGE, x + 20, y + 492, 376, 32);
        RenderManager.drawRoundedRect(vg, x + 20, y + 492, 376, 32, color.getRGB(), 8f);

        hexInput.setErrored(false);
        if(hexInput.isToggled()) {
            try {
                color.setColorFromHex(hexInput.getInput());
            } catch (Exception e) {
                hexInput.setErrored(true);
            }
        }

        if(copyBtn.isClicked()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(color.getHex()), null);
        }
        if(pasteBtn.isClicked()) {
            try {
                color.setColorFromHex(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor).toString());
            } catch (Exception ignored) {
            }
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
    }


    private static class ColorSlider extends Slider {
        protected int gradColorStart, gradColorEnd;
        protected Images image;

        public ColorSlider(int length, float min, float max, float startValue) {
            super(length, min, max, startValue);
            super.height = 16;
        }

        @Override
        public void draw(long vg, int x, int y) {
            update(x, y);

            if(image != null) {
                RenderManager.drawRoundImage(vg, image, x, y, width, height, 8f);
            } else {
                RenderManager.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f);
            }

            RenderManager.drawHollowRoundRect(vg, x - 1.5f, y - 1.5f, width + 2, height + 2, new Color(204, 204, 204, 77).getRGB(), 8f, 1f);
            RenderManager.drawHollowRoundRect(vg, currentDragPoint - 9, y - 2, 18, 18, OneConfigConfig.WHITE, 7f, 1f);
        }

        public void setGradient(int start, int end) {
            gradColorStart = start;
            gradColorEnd = end;
        }

        public void setImage(Images image) {
            this.image = image;
        }
    }
}

