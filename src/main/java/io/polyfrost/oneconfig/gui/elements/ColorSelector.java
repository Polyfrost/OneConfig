package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;

public class ColorSelector {
    private Color color;
    private final int x, y;
    private final int width = 416;
    private final int height = 768;

    private final BasicElement HSBButton = new BasicElement(128, 32, -1, true);
    private final BasicElement RGBButton = new BasicElement(128, 32, -1, true);
    private final BasicElement ChromaButton = new BasicElement(128, 32, -1, true);

    private final ArrayList<BasicElement> faves = new ArrayList<>();
    private final ArrayList<BasicElement> history = new ArrayList<>();
    private final BasicElement closeButton = new BasicElement(32, 32, -1, true);


    public ColorSelector(Color color, int mouseX, int mouseY) {
        this.color = color;
        this.y = mouseY - 768;
        this.x = mouseX - 208;

    }

    public void draw(long vg) {
        RenderManager.drawRoundedRect(vg, x, y, width, height, OneConfigConfig.GRAY_800, 20f);

    }

    public Color getColor() {
        return color;
    }



    private class HSBSelector extends ColorSelectorBase {


        public HSBSelector(Color color) {
            super(color);
        }

        @Override
        public void drawBox(long vg, int x, int y) {

        }

        @Override
        public void setColor(Color color) {

        }

        @Override
        public int[] drawTopSlider() {
            return new int[0];
        }

        @Override
        public int[] drawBottomSlider() {
            return new int[0];
        }

        @Override
        public float[] getColorAtPos(int clickX, int clickY) {
            return new float[0];
        }
    }


    private class RGBSelector extends ColorSelectorBase {

        public RGBSelector(Color color) {
            super(color);
        }

        @Override
        public void drawBox(long vg, int x, int y) {

        }

        @Override
        public void setColor(Color color) {

        }

        @Override
        public int[] drawTopSlider() {
            return new int[0];
        }

        @Override
        public int[] drawBottomSlider() {
            return new int[0];
        }


        @Override
        public float[] getColorAtPos(int clickX, int clickY) {
            return new float[0];
        }
    }



    private abstract class ColorSelectorBase {

        private int selectedX;
        private int selectedY;
        private float[] hsb = new float[3];
        private float[] rgba;
        private final TextInputFieldNumber hueField = new TextInputFieldNumber(72, 32, "", 0, 100);
        private final TextInputFieldNumber saturationField = new TextInputFieldNumber(72, 32, "", 0, 100);
        private final TextInputFieldNumber brightnessField = new TextInputFieldNumber(72, 32, "", 0, 100);
        private final TextInputFieldNumber alphaField = new TextInputFieldNumber(72, 32, "", 0, 100);

        private final TextInputField hexField = new TextInputField(107, 32, true, false, "");
        private final TextInputFieldNumber redField = new TextInputFieldNumber(44, 32, "", 0, 255);
        private final TextInputFieldNumber greenField = new TextInputFieldNumber(44, 32, "", 0, 255);
        private final TextInputFieldNumber blueField = new TextInputFieldNumber(44, 32, "", 0, 255);

        private final Slider sliderTop = new Slider(0);
        private final Slider sliderBottom = new Slider(0);

        public ColorSelectorBase(Color color) {
            rgba = new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f};
        }

        public void updateElements(float[] rgba) {
            this.rgba = rgba;
            hsb = Color.RGBtoHSB((int) (rgba[0] * 255), (int) (rgba[1] * 255), (int) (rgba[2] * 255), hsb);
            hueField.setInput(String.valueOf(hsb[0]));
            saturationField.setInput(String.valueOf(hsb[1]));
            brightnessField.setInput(String.valueOf(hsb[2]));
            alphaField.setInput(String.valueOf(rgba[3]));
            redField.setInput(String.valueOf(rgba[0]));
            greenField.setInput(String.valueOf(rgba[1]));
            blueField.setInput(String.valueOf(rgba[2]));
        }
        public abstract void drawBox(long vg, int x, int y);

        /** draw the color selector contents, including the box, and the input fields. If it is clicked, getColorAtPos is called. updateElements is also called to update all the input fields. */
        public void draw(long vg, int x, int y) {
            drawBox(vg, x + 16, y + 120);
            if(InputUtils.isAreaHovered(x + 16, y + 120, 384, 288) && Mouse.isButtonDown(0)) {
                selectedX = InputUtils.mouseX() - x - 16;
                selectedY = InputUtils.mouseY() - y - 120;
                rgba = getColorAtPos(selectedX, selectedY);
            }           // TODO all of this
            hueField.draw(vg, x + 104, y + 544);
            saturationField.draw(vg, x + 312, y + 544);
            brightnessField.draw(vg, x + 103, y + 584);
            alphaField.draw(vg, x + 103, y + 584);
            hexField.draw(vg, x + 96, y + 624);
            redField.draw(vg, x + 228, y + 624);
            greenField.draw(vg, x + 292, y + 664);
            blueField.draw(vg, x + 356, y + 664);
            sliderTop.draw(vg, x + 16, y + 424, drawTopSlider()[0], drawTopSlider()[1]);
            sliderBottom.draw(vg, x + 16, y + 576, drawBottomSlider()[0], drawBottomSlider()[1]);
            updateElements(rgba);
            Color color1 = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
            setColor(color1);
            RenderManager.drawRoundedRect(vg, x + 16, y + 488, 384, 40, color1.getRGB(), 12f);
        }

        /** called to set the color of the color selector box based on the values of the input fields. */
        public abstract void setColor(Color color);

        /** return an array of two ints of the start color of the gradient and the end color of the gradient. */
        public abstract int[] drawTopSlider();
        /** return an array of two ints of the start color of the gradient and the end color of the gradient. */
        public abstract int[] drawBottomSlider();

        /**
         * This method is called when the color selector is clicked. It needs to return color at the clicked position.
         * @return color at the clicked position as a <code>float[] rgba.</code>
         */
        public abstract float[] getColorAtPos(int clickX, int clickY);

        public float getRed() {
            return rgba[0];
        }
        public float getGreen(){
            return rgba[1];
        }
        public float getBlue(){
            return rgba[2];
        }
        public float getAlpha(){
            return rgba[3];
        }

        public float getHue(){
            return hsb[0];
        }

        public float getSaturation(){
            return hsb[1];
        }

        public float getBrightness(){
            return hsb[2];
        }

        public String getHex() {
            return null;
        };

        public Color getColor() {
            return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

    }

    private class TextInputFieldNumber extends TextInputField {
        private final float min, max;
        public TextInputFieldNumber(int width, int height, String defaultValue, float min, float max) {
            super(width, height, true, true, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        public void draw(long vg, int x, int y) {
            super.draw(vg, x, y);

        }
    }

    private class Slider {
        private final int style;

        public Slider(int style) {
            this.style = style;
        }

        public void draw(long vg, int x, int y, int color1, int color2) {

        }
    }
}


