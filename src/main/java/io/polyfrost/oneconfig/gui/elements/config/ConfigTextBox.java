package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.TextInputField;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.InputUtils;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption {
    private final boolean secure;
    private final boolean multiLine;
    private final TextInputField textField;

    public ConfigTextBox(Field field, String name, int size, String placeholder, boolean secure, boolean multiLine) {
        super(field, name, size);
        this.secure = secure;
        this.multiLine = multiLine;
        this.textField = new TextInputField(size == 1 && hasHalfSize() ? 256 : 640, multiLine ? 64 : 32, placeholder, multiLine, secure);
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14, Fonts.INTER_MEDIUM);

        try {
            String value = (String) get();
           textField.setInput(value == null ? "" : value);
        } catch (IllegalAccessException ignored) {
        }


        textField.draw(vg, x + (size == 1 && hasHalfSize() ? 224 : 352), y);

        if (secure) RenderManager.drawImage(vg, "/assets/oneconfig/textures/eye.png", x + 967, y + 7, 18, 18, new Color(196,196,196).getRGB());
        if (secure && InputUtils.isAreaClicked(x + 967, y + 7, 18, 18)) textField.setPassword(!textField.getPassword());
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        textField.keyTyped(key, keyCode);
        try {
            set(textField.getInput());
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public int getHeight() {
        return multiLine ? 64 : 32;
    }

    @Override
    public boolean hasHalfSize() {
        return !secure && !multiLine;
    }
}
