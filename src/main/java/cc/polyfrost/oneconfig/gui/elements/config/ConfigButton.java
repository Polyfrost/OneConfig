package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.lang.reflect.Field;

public class ConfigButton extends BasicOption {
    private final BasicButton button;

    public ConfigButton(Runnable runnable, Object parent, String name, int size, String text) {
        super(null, parent, name, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, null, null, 1, BasicButton.ALIGNMENT_CENTER, runnable);
    }

    public ConfigButton(Field field, Object parent, String name, int size, String text) {
        super(field, parent, name, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, null, null, 1, BasicButton.ALIGNMENT_CENTER, getRunnableFromField(field, parent));
    }

    @Override
    public void draw(long vg, int x, int y) {
        button.disable(!isEnabled());
        if(!isEnabled()) RenderManager.withAlpha(vg, 0.5f);
        RenderManager.drawString(vg, name, x, y + 17, OneConfigConfig.WHITE, 14f, Fonts.MEDIUM);
        button.draw(vg, x + (size == 1 ? 352 : 736), y);
        RenderManager.withAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }

    private static Runnable getRunnableFromField(Field field, Object parent) {
        Runnable runnable = () -> {};
        try {
            runnable = (Runnable) field.get(parent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return runnable;
    }
}
