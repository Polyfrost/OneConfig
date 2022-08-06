package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;

import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption {
    private final TextInputField textField;

    public ConfigTextBox(Field field, Object parent, String name, String category, String subcategory, int size, String placeholder, boolean secure, boolean multiLine) {
        super(field, parent, name, category, subcategory, size);
        this.textField = new TextInputField(size == 1 ? 256 : 640, multiLine ? 512 : 32, placeholder, secure, false, null);
    }

    public static ConfigTextBox create(Field field, Object parent) {
        Text text = field.getAnnotation(Text.class);
        return new ConfigTextBox(field, parent, text.name(), text.category(), text.subcategory(), text.secure() || text.multiline() ? 2 : text.size(), text.placeholder(), text.secure(), text.multiline());
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        textField.disable(!isEnabled());
        RenderManager.drawText(vg, name, x, y + 16, Colors.WHITE_90, 14, Fonts.MEDIUM);

        try {
            String value = (String) get();
            textField.setInput(value == null ? "" : value);
        } catch (IllegalAccessException ignored) {
        }

        textField.draw(vg, x + (size == 1 ? 224 : 352), y);

        RenderManager.setAlpha(vg, 1f);
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
    public float getHeight() {
        return textField.getHeight();
    }
}
