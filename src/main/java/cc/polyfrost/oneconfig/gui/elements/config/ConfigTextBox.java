package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.utils.InputUtils;

import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption {
    private final boolean secure;
    private final boolean multiLine;
    private final TextInputField textField;

    public ConfigTextBox(Field field, Object parent, String name, String category, String subcategory, int size, String placeholder, boolean secure, boolean multiLine) {
        super(field, parent, name, category, subcategory, size);
        this.secure = secure;
        this.multiLine = multiLine;
        this.textField = new TextInputField(size == 1 ? 256 : 640, multiLine ? 64 : 32, placeholder, multiLine, secure);
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

        if (multiLine && textField.getLines() > 2) textField.setHeight(64 + 24 * (textField.getLines() - 2));
        else if (multiLine) textField.setHeight(64);
        textField.draw(vg, x + (size == 1 ? 224 : 352), y);

        if (secure) {
            SVGs icon = textField.getPassword() ? SVGs.EYE_OFF : SVGs.EYE;
            boolean hovered = InputUtils.isAreaHovered(x + 967, y + 7, 18, 18) && isEnabled();
            int color = hovered ? Colors.WHITE : Colors.WHITE_80;
            if (hovered && InputUtils.isClicked()) textField.setPassword(!textField.getPassword());
            if (hovered && Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.5f);
            RenderManager.drawSvg(vg, icon, x + 967, y + 7, 18, 18, color);
        }
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
    public int getHeight() {
        return multiLine ? textField.getHeight() : 32;
    }
}
