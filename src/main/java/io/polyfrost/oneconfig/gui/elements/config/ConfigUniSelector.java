package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.lang.reflect.Field;

public class ConfigUniSelector extends BasicOption {
    String[] options;

    public ConfigUniSelector(Field field, String name, int size, String[] options) {
        super(field, name, size);
        this.options = options;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y) {
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 18f, Fonts.INTER_MEDIUM);
        RenderManager.drawString(vg, options[selected], x + 16, y + 16, OneConfigConfig.WHITE_90, 18f, Fonts.INTER_MEDIUM);
        RenderManager.drawImage(vg, "/assets/oneconfig/textures/arrow.png", x + 230, y + 7, 13, 22, OneConfigConfig.BLUE_400);  // TODO
    }
}
