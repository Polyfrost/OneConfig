package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

public class ColorSelector {
    private int x;
    private int y;
    private OneColor color;
    private float percentMove = 0f;
    private int mouseX, mouseY;
    private final ArrayList<BasicElement> buttons = new ArrayList<>();
    private final BasicElement closeBtn = new BasicElement(32, 32, false);

    private final BasicButton copyBtn = new BasicButton(32, 32, SVGs.COPY, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);
    private final BasicButton pasteBtn = new BasicButton(32, 32, SVGs.PASTE, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);
    private final BasicButton guideBtn = new BasicButton(112, 32, "Guide", SVGs.HELP_CIRCLE, SVGs.POP_OUT, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);
    private final BasicButton faveBtn = new BasicButton(32, 32, SVGs.HEART_OUTLINE, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);
    private final BasicButton recentBtn = new BasicButton(32, 32, SVGs.HISTORY, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);

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
        buttons.add(new BasicElement(124, 28, ColorUtils.SECONDARY, true, 10f));
        buttons.add(new BasicElement(124, 28, ColorUtils.SECONDARY, true, 10f));
        buttons.add(new BasicElement(124, 28, ColorUtils.SECONDARY, true, 10f));
        hueInput.setCurrentValue(color.getHue());
        saturationInput.setCurrentValue(color.getSaturation());
        brightnessInput.setCurrentValue(color.getBrightness());
        alphaInput.setCurrentValue(color.getAlpha() / 255f * 100f);
        speedSlider.setValue(color.getDataBit());
        topSlider.setValue(color.getHue());
        topSlider.setColor(color);
        bottomSlider.setValue(color.getAlpha() / 255f * 100f);
        hexInput.setInput(color.getHex());
        this.x = mouseX - 208;
        this.y = Math.max(0, mouseY - 776);
        if(color.getDataBit() != -1) mode = 2;
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
        if (InputUtils.isAreaHovered(x, y, 368, 64) && Mouse.isButtonDown(0) && !dragging) {
            int dx = Mouse.getDX();
            int dy = Mouse.getDY();
            x += dx;
            mouseX += dx;
            y -= dy;
            mouseY -= dy;
        }
        int width = 416;
        int height = 768;
        RenderManager.drawHollowRoundRect(vg, x - 3, y - 3, width + 4, height + 4, new Color(204, 204, 204, 77).getRGB(), 20f, 2f);
        RenderManager.drawRoundedRect(vg, x, y, width, height, OneConfigConfig.GRAY_800, 20f);
        RenderManager.drawString(vg, "Color Selector", x + 16, y + 32, OneConfigConfig.WHITE_90, 18f, Fonts.SEMIBOLD);
        if(!closeBtn.isHovered()) RenderManager.setAlpha(vg, 0.8f);
        closeBtn.draw(vg, x + 368, y + 16);
        RenderManager.drawSvg(vg, SVGs.X_CIRCLE, x + 368, y + 16, 32, 32, closeBtn.isHovered() ? OneConfigConfig.ERROR_600 : -1);
        RenderManager.setAlpha(vg, 1f);

        // hex parser
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
            if (mode == 0) topSlider.setValue(color.getHue());
            if (mode == 1) bottomSlider.setValue(color.getBrightness() / 100f * 360f);
        }

        // TODO favorite stuff
        faveBtn.draw(vg, x + 16, y + 672);
        recentBtn.draw(vg, x + 16, y + 720);
        for(int i = 0; i < 7; i++) {
            favoriteColors.get(i).draw(vg, x + 104 + i * 44, y + 672);
        }
        for(int i = 0; i < 7; i++) {
            recentColors.get(i).draw(vg, x + 104 + i * 44, y + 720);
        }

        RenderManager.drawRoundedRect(vg, x + 16, y + 64, 384, 32, OneConfigConfig.GRAY_500, 12f);
        RenderManager.drawRoundedRect(vg, x + 18 + (percentMove * 128), y + 66, 124, 28, OneConfigConfig.PRIMARY_600, 10f);
        int i = 18;
        for (BasicElement button : buttons) {
            button.draw(vg, x + i, y + 66);
            if (button.isClicked()) {
                mode = buttons.indexOf(button);
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
            if (percentMove != mode) {
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
        RenderManager.drawString(vg, color.getDataBit() == -1 ? "Hex (RGB):" : "Color Code:", x + 16, y + 641, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
        hexInput.draw(vg, x + 104, y + 624);

        copyBtn.draw(vg, x + 204, y + 624);
        pasteBtn.draw(vg, x + 244, y + 624);


        guideBtn.draw(vg, x + 288, y + 624);


        boolean isMouseDown = Mouse.isButtonDown(0);
        boolean hovered = Mouse.isButtonDown(0) && InputUtils.isAreaHovered(x + 16, y + 120, 384, 288);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        mouseWasDown = isMouseDown;
        if (mode != 2) color.setChromaSpeed(-1);
        switch (mode) {
            default:
            case 0:
            case 2:
                buttons.get(mode).currentColor = OneConfigConfig.TRANSPARENT;
                topSlider.setImage(Images.HUE_GRADIENT);
                RenderManager.drawHSBBox(vg, x + 16, y + 120, 384, 288, color.getRGBMax(true));
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
                color.setHSBA((int) topSlider.getValue(), Math.round(progressX * 100), Math.round(progressY * 100), (int) ((bottomSlider.getValue() / 100f) * 255));
                if (mode == 0) {
                    topSlider.setColor(color);
                    topSlider.draw(vg, x + 16, y + 424);
                    RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                    hueInput.draw(vg, x + 104, y + 544);
                }
                if (mode == 2) {
                    speedSlider.draw(vg, x + 60, y + 424);
                    RenderManager.drawString(vg, "SLOW", x + 16, y + 429, OneConfigConfig.WHITE_80, 12f, Fonts.REGULAR);
                    RenderManager.drawString(vg, "FAST", x + 370, y + 429, OneConfigConfig.WHITE_80, 12f, Fonts.REGULAR);
                    RenderManager.drawString(vg, "Speed (s)", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                    hueInput.draw(vg, x + 104, y + 544);
                    if (!speedSlider.isDragging()) {
                        color.setChromaSpeed((int) Math.abs(speedSlider.getValue() - 29));
                    }
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
                color.setHSBA(dragging ? angle : color.getHue(), saturation, (int) (topSlider.getValue() / 360 * 100), (int) ((bottomSlider.getValue() / 100f) * 255));
                topSlider.setGradient(OneConfigConfig.BLACK, color.getRGBMax(true));
                topSlider.setImage(null);
                RenderManager.drawString(vg, "Hue", x + 16, y + 560, OneConfigConfig.WHITE_80, 12f, Fonts.MEDIUM);
                hueInput.draw(vg, x + 104, y + 544);
                topSlider.draw(vg, x + 16, y + 424);
                break;
        }
        if (dragging && InputUtils.isClicked(true)) {
            dragging = false;
        }
        bottomSlider.setGradient(OneConfigConfig.TRANSPARENT, color.getRGBNoAlpha());
        RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x + 16, y + 456, 384, 16, 8f);
        bottomSlider.draw(vg, x + 16, y + 456);

        RenderManager.drawRoundedRect(vg, mouseX - 7, mouseY - 7, 14, 14, OneConfigConfig.WHITE, 14f);
        RenderManager.drawRoundedRect(vg, mouseX - 6, mouseY - 6, 12, 12, OneConfigConfig.BLACK, 12f);
        RenderManager.drawRoundedRect(vg, mouseX - 5, mouseY - 5, 10, 10, color.getRGBNoAlpha(), 10f);

        // deal with the input fields
        if (hueInput.isToggled() || saturationInput.isToggled() || brightnessInput.isToggled() || alphaInput.isToggled() || hueInput.arrowsClicked() || saturationInput.arrowsClicked() || brightnessInput.arrowsClicked() || alphaInput.arrowsClicked() || hexInput.isToggled() || pasteBtn.isClicked()) {
            if (mode != 2 && !hexInput.isToggled()) {
                color.setHSBA((int) hueInput.getCurrentValue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
            }
            if (mode == 2) {
                color.setHSBA(color.getHue(), (int) saturationInput.getCurrentValue(), (int) brightnessInput.getCurrentValue(), (int) ((alphaInput.getCurrentValue() / 100f) * 255f));
                color.setChromaSpeed((int) (hueInput.getCurrentValue() / 360f * 30f));
                speedSlider.setValue(hueInput.getCurrentValue() / 360f * 30f);
            }
            bottomSlider.setValue(color.getAlpha() / 255f * 100f);
            if (mode == 0 || mode == 2) {
                mouseX = (int) (saturationInput.getCurrentValue() / 100f * 384 + x + 16);
                mouseY = (int) (Math.abs(brightnessInput.getCurrentValue() / 100f - 1f) * 288 + y + 120);
            } else {
                topSlider.setValue(color.getBrightness() / 100f * 360f);
                mouseX = (int) (Math.sin(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + x + 208);
                mouseY = (int) (Math.cos(Math.toRadians(-color.getHue()) + 1.5708) * (saturationInput.getCurrentValue() / 100 * 144) + y + 264);
            }
            if (mode == 0) {
                topSlider.setValue(color.getHue());
            }
        } else if (OneConfigGui.INSTANCE.mouseDown) {
            saturationInput.setInput(String.format("%.01f", (float) color.getSaturation()));
            brightnessInput.setInput(String.format("%.01f", (float) color.getBrightness()));
            alphaInput.setInput(String.format("%.01f", color.getAlpha() / 255f * 100f));
            if (hexInput.isToggled()) return;
            if (mode != 2) {
                hueInput.setInput(String.format("%.01f", (float) color.getHue()));
                hexInput.setInput("#" + color.getHex());
            } else {
                hueInput.setInput(String.format("%.01f", (float) color.getDataBit()));
                hexInput.setInput("Z" + color.getDataBit());
            }

        }
        if (mode != 2 && !hexInput.isToggled()) {
            hueInput.setInput(String.format("%.01f", (float) color.getHue()));
            hexInput.setInput("#" + color.getHex());
        }
        if(guideBtn.isClicked()) NetworkUtils.browseLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ");


        // draw the color preview
        RenderManager.drawHollowRoundRect(vg, x + 15, y + 487, 384, 40, OneConfigConfig.GRAY_300, 12f, 2f);
        RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x + 20, y + 492, 376, 32, 8f);
        RenderManager.drawRoundedRect(vg, x + 20, y + 492, 376, 32, color.getRGB(), 8f);
        InputUtils.blockClicks(true);
        if (closeBtn.isClicked()) {
            OneConfigGui.INSTANCE.closeColorSelector();
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
            if (image != null) {
                RenderManager.drawRoundImage(vg, image, x + 1, y + 1, width - 2, height - 2, 8f);
            } else {
                RenderManager.drawGradientRoundedRect(vg, x, y, width, height, gradColorStart, gradColorEnd, 8f);
            }

            RenderManager.drawHollowRoundRect(vg, x - 0.5f, y - 0.5f, width, height, new Color(204, 204, 204, 80).getRGB(), 8f, 1f);
            RenderManager.drawHollowRoundRect(vg, currentDragPoint - 1, y - 1, 18, 18, OneConfigConfig.WHITE, 9f, 1f);
            RenderManager.drawHollowRoundRect(vg,  currentDragPoint, y, 16, 16, OneConfigConfig.BLACK, 8f, 1f);
            if (color != null) {
                RenderManager.drawRoundedRect(vg, currentDragPoint + 1.5f, y + 1.5f, 14, 14, color.getRGBMax(true), 7f);
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

