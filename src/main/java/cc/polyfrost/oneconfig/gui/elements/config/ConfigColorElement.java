package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigColorElement extends BasicOption {
    private float alpha;
    private Color color = Color.BLUE;
    private String hex;

    private final TextInputField hexField = new TextInputField(104, 32, "", false, false);
    private final TextInputField alphaField = new TextInputField(72, 32, "", false, false);
    private final BasicElement element = new BasicElement(64, 32, false);

    public ConfigColorElement(Field field, String name, int size) {
        super(field, name, size);
        hexField.setCentered(true);
        alphaField.setCentered(true);
        alphaField.onlyAcceptNumbers(true);
        String buf = Integer.toHexString(color.getRGB());
        hex = "#" + buf.substring(buf.length() - 6);
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawString(vg, name, x, y + 15, OneConfigConfig.WHITE_90, 18f, Fonts.MEDIUM);
        hexField.draw(vg, x + 240, y);

        if (!alphaField.isToggled()) alphaField.setInput(String.format("%.01f", alpha * 100f) + "%");
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
                alpha = input / 100f;
            } catch (NumberFormatException e) {
                alphaField.setErrored(true);
            }
        }
        alphaField.draw(vg, x + 352, y);

        if (!hexField.isToggled()) hexField.setInput(hex);
        hexField.setErrored(false);
        if (hexField.isToggled()) {
            try {
                color = HexToColor(hexField.getInput());
                String buf = Integer.toHexString(color.getRGB());
                hex = "#" + buf.substring(buf.length() - 6);
            } catch (NumberFormatException e) {
                hexField.setErrored(true);
            }
        }
        hexField.draw(vg, x + 352, y);

        element.update(x + 432, y);
        RenderManager.drawRoundedRect(vg, x + 432, y, 64, 32, OneConfigConfig.GRAY_300, 12f);
        RenderManager.drawImage(vg, Images.COLOR_BASE, x + 948, y + 4, 56, 24, color.getRGB());
        if (element.isClicked() && !element.isToggled()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, InputUtils.mouseX(), InputUtils.mouseY()));
        }
        if (element.isToggled() && element.isClicked()) {
            color = OneConfigGui.INSTANCE.closeColorSelector();
            alpha = color.getAlpha() / 255f;
            String buf = Integer.toHexString(color.getRGB());
            hex = "#" + buf.substring(buf.length() - 6);
        }

    }

    // thanks stack overflow
    public static Color HexToColor(String hex) throws NumberFormatException {
        hex = hex.replace("#", "");
        switch (hex.length()) {
            case 6:
                return new Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16));
            case 8:
                return new Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        Integer.valueOf(hex.substring(6, 8), 16));
        }
        throw new NumberFormatException("Invalid hex string: " + hex);
    }
}
