package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.OneColor;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;
import gg.essential.universal.UResolution;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigColorElement extends BasicOption {
    private final TextInputField hexField = new TextInputField(104, 32, "", false, false);
    private final TextInputField alphaField = new TextInputField(72, 32, "", false, false);
    private final BasicElement element = new BasicElement(64, 32, false);

    public ConfigColorElement(Field field, Object parent, String name, int size) {
        super(field, parent, name, size);
        hexField.setCentered(true);
        alphaField.setCentered(true);
        alphaField.onlyAcceptNumbers(true);
    }

    @Override
    public void draw(long vg, int x, int y) {
        int x1 = size == 1 ? x : x + 512;
        OneColor color;
        try {
            color = (OneColor) get();
        } catch (IllegalAccessException e) {
            return;
        }
        String buf1 = Integer.toHexString(color.getRGB());
        String hex = "#" + buf1.substring(buf1.length() - 6);
        hexField.setInput(hex);
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        hexField.draw(vg, x1 + 224, y);

        if (!alphaField.isToggled()) alphaField.setInput(String.valueOf((color.getAlpha())));
        alphaField.setErrored(false);
        if (alphaField.isToggled()) {
            try {
                float input = Float.parseFloat(alphaField.getInput());
                if (input < 0f) {
                    alphaField.setErrored(true);
                    input = 100f;
                }
                if (input > 100f) {
                    alphaField.setErrored(true);
                    input = 100f;
                }
            } catch (NumberFormatException e) {
                alphaField.setErrored(true);
            }
        }
        alphaField.draw(vg, x1 + 336, y);

        element.update(x1 + 416, y);
        RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x1 + 416, y, 64, 32, 12f);
        RenderManager.drawRoundedRect(vg, x1 + 416, y, 64, 32, color.getRGB(), 12f);
        RenderManager.drawHollowRoundRect(vg, x1 + 416, y, 64, 32, OneConfigConfig.GRAY_300, 12f, 2f);
        if (element.isClicked() && !element.isToggled()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(new OneColor(40, 30, 20), InputUtils.mouseX(), InputUtils.mouseY()));
        }
    }

    // thanks stack overflow
    public static OneColor HexToColor(String hex) throws NumberFormatException {
        hex = hex.replace("#", "");
        switch (hex.length()) {
            case 6:
                return new OneColor(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16));
            case 8:
                return new OneColor(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        Integer.valueOf(hex.substring(6, 8), 16));
        }
        throw new NumberFormatException("Invalid hex string: " + hex);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
