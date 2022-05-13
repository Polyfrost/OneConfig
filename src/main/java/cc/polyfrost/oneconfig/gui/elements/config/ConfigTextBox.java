package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption {
    private final boolean secure;
    private final boolean multiLine;
    private final TextInputField textField;

    public ConfigTextBox(Field field, Object parent, String name, int size, String placeholder, boolean secure, boolean multiLine) {
        super(field, parent, name, size);
        this.secure = secure;
        this.multiLine = multiLine;
        this.textField = new TextInputField(size == 1 && hasHalfSize() ? 256 : 640, multiLine ? 64 : 32, placeholder, multiLine, secure);
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (!isEnabled()) NanoVG.nvgGlobalAlpha(vg, 0.5f);
        textField.disable(!isEnabled());
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14, Fonts.MEDIUM);

        try {
            String value = (String) get();
            textField.setInput(value == null ? "" : value);
        } catch (IllegalAccessException ignored) {
        }

        textField.draw(vg, x + (size == 1 && hasHalfSize() ? 224 : 352), y);

        if (secure)
            RenderManager.drawImage(vg, Images.EYE, x + 967, y + 7, 18, 18, new Color(196, 196, 196).getRGB());
        if (secure && InputUtils.isAreaClicked(x + 967, y + 7, 18, 18)) textField.setPassword(!textField.getPassword());
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (!isEnabled()) return;
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
