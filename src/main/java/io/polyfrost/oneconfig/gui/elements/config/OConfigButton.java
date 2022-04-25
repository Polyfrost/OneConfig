package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.Option;
import io.polyfrost.oneconfig.gui.elements.BasicElement;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.lang.reflect.Field;

public class OConfigButton extends Option {
    private final String text;
    private final BasicElement element;

    public OConfigButton(Field field, String name, String description, String text, int size) {
        super(field, name, description, size);
        this.text = text;
        element = new BasicElement(128, 32, 1, true);
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {
        if (size == 0) {
            RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);
            element.setWidth((int) RenderManager.getTextWidth(vg, text, 12f) + 80);
            element.draw(vg, x + 480 - element.getWidth(), y);
            RenderManager.drawString(vg, text, x + element.getWidth() / 2f, y + 16, OneConfigConfig.WHITE, 12f, Fonts.INTER_MEDIUM);
            // ???
        }
    }
}
