package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.TextInputField;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption {
    private final boolean secure;
    private final boolean multiLine;
    private final TextInputField textField;

    public ConfigTextBox(Field field, String name, int size, boolean secure, boolean multiLine) {
        super(field, name, size);
        this.secure = secure;
        this.multiLine = multiLine;
        String value = null;
        try {
            value = (String) get();
        } catch (IllegalAccessException ignored) {
        }
        if (value == null) value = "";
        this.textField = new TextInputField(size == 1 && hasHalfSize() ? 216 : 640, multiLine ? 64 : 32, value, multiLine, secure);
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE, 14, Fonts.INTER_MEDIUM);

        textField.draw(vg, x + (size == 1 && hasHalfSize() ? 224 : 352), y);

        if (secure) RenderManager.drawImage(vg, "/assets/oneconfig/textures/eye.png", x + 967, y + 7, 18, 18);
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
