package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.data.OptionPage;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;

import java.lang.reflect.Field;

public class ConfigPage extends BasicOption {
    public final OptionPage page;
    public final String description;

    public ConfigPage(Field field, String name, String description, int size, OptionPage page) {
        super(field, name, size);
        this.description = description;
        this.page = page;
    }

    @Override
    public void draw(long vg, int x, int y) {

    }

    @Override
    public int getHeight() {
        return description.equals("") ? 64 : 96;
    }

    @Override
    public boolean hasHalfSize() {
        return false;
    }
}
