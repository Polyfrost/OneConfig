package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;

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
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        if (!hexField.isToggled()) hexField.setInput("#" + color.getHex());
        hexField.setErrored(false);
        if (hexField.isToggled()) {
            try {
                color.setColorFromHex(hexField.getInput());
            } catch (NumberFormatException e) {
                hexField.setErrored(true);
            }
        }
        hexField.draw(vg, x1 + 224, y);

        if (!alphaField.isToggled()) alphaField.setInput(Math.round(color.getAlpha() / 2.55f) + "%");
        alphaField.setErrored(false);
        if (alphaField.isToggled()) {
            try {
                float input = Float.parseFloat(alphaField.getInput().replace("%", ""));
                if (input < 0f) {
                    alphaField.setErrored(true);
                    input = 100f;
                }
                if (input > 100f) {
                    alphaField.setErrored(true);
                    input = 100f;
                }
                color = new OneColor((float) color.getHue(), color.getSaturation(), color.getBrightness(), Math.round(input * 2.55f));
            } catch (NumberFormatException e) {
                alphaField.setErrored(true);
            }
        }
        alphaField.draw(vg, x1 + 336, y);

        element.update(x1 + 416, y);
        RenderManager.drawHollowRoundRect(vg, x1 + 415, y - 1, 64, 32, OneConfigConfig.GRAY_300, 12f, 2f);
        RenderManager.drawRoundImage(vg, Images.ALPHA_GRID, x1 + 420, y + 4, 56, 24, 8f);
        RenderManager.drawRoundedRect(vg, x1 + 420, y + 4, 56, 24, color.getRGB(), 8f);
        if (element.isClicked() && !element.isToggled()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, InputUtils.mouseX(), InputUtils.mouseY()));
        }
        if (OneConfigGui.INSTANCE != null && OneConfigGui.INSTANCE.currentColorSelector != null) {
            color = (OneConfigGui.INSTANCE.getColor());
        }
        setColor(color);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        alphaField.keyTyped(key, keyCode);
        hexField.keyTyped(key, keyCode);
    }

    protected void setColor(OneColor color) {
        try {
            set(color);
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
