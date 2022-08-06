package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ConfigButton extends BasicOption {
    private final BasicButton button;

    public ConfigButton(Runnable runnable, Object parent, String name, String category, String subcategory, int size, String text) {
        super(null, parent, name, category, subcategory, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
        this.button.setClickAction(runnable);
    }

    public ConfigButton(Field field, Object parent, String name, String category, String subcategory, int size, String text) {
        super(field, parent, name, category, subcategory, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
        this.button.setClickAction(getRunnableFromField(field, parent));
    }

    public ConfigButton(Method method, Object parent, String name, String category, String subcategory, int size, String text) {
        super(null, parent, name, category, subcategory, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
        this.button.setClickAction(() -> {
            try {
                method.invoke(parent);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("wrong number of arguments")) {
                    throw new IllegalArgumentException("Button method " + method.getDeclaringClass().getName() + "." + method.getName() + "(" + Arrays.toString(method.getGenericParameterTypes()) + ") must take no arguments!");
                } else e.printStackTrace();
            }
        });
    }

    public static ConfigButton create(Field field, Object parent) {
        Button button = field.getAnnotation(Button.class);
        return new ConfigButton(field, parent, button.name(), button.category(), button.subcategory(), button.size(), button.text());
    }

    public static ConfigButton create(Method method, Object parent) {
        method.setAccessible(true);
        Button button = method.getAnnotation(Button.class);
        return new ConfigButton(method, parent, button.name(), button.category(), button.subcategory(), button.size(), button.text());
    }

    private static Runnable getRunnableFromField(Field field, Object parent) {
        Runnable runnable = () -> {
        };
        try {
            runnable = (Runnable) field.get(parent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return runnable;
    }

    @Override
    public void draw(long vg, int x, int y) {
        button.disable(!isEnabled());
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawText(vg, name, x, y + 17, Colors.WHITE, 14f, Fonts.MEDIUM);
        button.draw(vg, x + (size == 1 ? 352 : 736), y);
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public float getHeight() {
        return 32;
    }
}
