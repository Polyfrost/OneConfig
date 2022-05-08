package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.MathUtils;

import java.awt.*;

public class ColorSelector {
    private final int x;
    private final int y;
    private Color color;
    private float percentMove = 0f;
    private final BasicElement hsbBtn = new BasicElement(124, 28, 2, true);
    private final BasicElement rgbBtn = new BasicElement(124, 28, 2, true);
    private final BasicElement chromaBtn = new BasicElement(124, 28, 2, true);
    private final BasicElement closeBtn = new BasicElement(32, 32, true);

    private final BasicElement copyBtn = new BasicElement(32, 32, 2, true);
    private final BasicElement pasteBtn = new BasicElement(32, 32, 2, true);
    private final BasicButton guideBtn = new BasicButton(112, 32, "Guide", null, null, 0, BasicButton.ALIGNMENT_CENTER);

    private final NumberInputField hueInput = new NumberInputField(90, 32, 0, 0, 255, 1);
    private final NumberInputField saturationInput = new NumberInputField(90, 32, 0, 0, 255, 1);
    private final NumberInputField brightnessInput = new NumberInputField(90, 32, 0, 0, 100, 1);
    private final NumberInputField alphaInput = new NumberInputField(90, 32, 0, 0, 100, 1);
    private final TextInputField hexInput = new TextInputField(88, 32, true, "");

    private final ColorSlider topSlider = new ColorSlider(384, 0, 255, 127);
    private final ColorSlider bottomSlider = new ColorSlider(384, 0, 255, 127);
    private final Slider speedSlider = new Slider(384, 1, 60, 20);


    public ColorSelector(Color color, int mouseX, int mouseY) {
        this.color = color;
        this.x = mouseX - 208;
        this.y = mouseY - 776;
    }

    public void draw(long vg) {
        int width = 416;
        int height = 768;
        int mode = 1;

        RenderManager.drawHollowRoundRect(vg, x - 3, y - 3, width + 4, height + 4, new Color(204, 204, 204, 77).getRGB(), 20f, 2f);
        RenderManager.drawRoundedRect(vg, x, y, width, height, OneConfigConfig.GRAY_800, 20f);
        RenderManager.drawString(vg, "Color Selector", x + 16, y + 32, OneConfigConfig.WHITE_90, 18f, Fonts.SEMIBOLD);
        closeBtn.draw(vg, x + 368, y + 16);
        RenderManager.drawImage(vg, Images.CLOSE, x + 369, y + 17, 30, 30);
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
        RenderManager.drawString(vg, "Hex (RGBA)", x + 16, y + 641, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
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
                break;
            case 1:
                RenderManager.drawRoundedRect(vg, x + 64, y + 120, 288, 288, OneConfigConfig.WHITE, 144f);

                RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                hueInput.draw(vg, x + 104, y + 544);

                Color colorMax = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
                float[] hsbColor = new float[3];
                Color.RGBtoHSB(colorMax.getRed(), colorMax.getGreen(), colorMax.getBlue(), hsbColor);
                hsbColor[2] = topSlider.getValue() / 255f;
                color = new Color(Color.HSBtoRGB(hsbColor[0], hsbColor[1], hsbColor[2]), true);
                bottomSlider.setGradient(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25), colorMax);
                topSlider.setGradient(Color.BLACK, colorMax);
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) bottomSlider.getValue());

                //RenderManager.drawRoundedRect(vg, bottomSlider.currentDragPoint - 8, y + 456, 16, 16, color.getRGB(), 16f);

                topSlider.draw(vg, x + 16, y + 424);
                RenderManager.drawImage(vg, Images.COLOR_BASE_LONG, x + 16, y + 456, 384, 16);
                bottomSlider.draw(vg, x + 16, y + 456);
                break;
            case 2:
                break;
        }

        float[] hsbColor = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbColor);
        hueInput.setInput(String.format("%.01f", hsbColor[0] * 360f));
        saturationInput.setInput(String.format("%.01f", hsbColor[1] * 100f));
        brightnessInput.setInput(String.format("%.01f", hsbColor[2] * 100f));
        alphaInput.setInput(String.format("%.01f", color.getAlpha() / 255f * 100f));


        RenderManager.drawHollowRoundRect(vg, x + 15, y + 487, 384, 40, OneConfigConfig.GRAY_300, 12f, 2f);
        RenderManager.drawImage(vg, Images.COLOR_BASE_LARGE, x + 20, y + 492, 376, 32);
        RenderManager.drawRoundedRect(vg, x + 20, y + 492, 376, 32, color.getRGB(), 8f);
    }

    public Color getColor() {
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
        protected Color gradColorStart, gradColorEnd;

        public ColorSlider(int length, float min, float max, float startValue) {
            super(length, min, max, startValue);
            super.height = 16;
        }

        @Override
        public void draw(long vg, int x, int y) {
            update(x, y);
            RenderManager.drawHollowRoundRect(vg, x - 1.5f, y - 1.5f, width + 2, height + 2, new Color(204, 204, 204, 77).getRGB(), 8f, 1f);
            RenderManager.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart.getRGB(), gradColorEnd.getRGB(), 8f);
            RenderManager.drawHollowRoundRect(vg, currentDragPoint - 9, y - 2, 18, 18, OneConfigConfig.WHITE, 7f, 1f);
        }

        public void setGradient(Color start, Color end) {
            gradColorStart = start;
            gradColorEnd = end;
        }
    }
}

