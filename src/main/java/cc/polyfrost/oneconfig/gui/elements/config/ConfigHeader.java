package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Header;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;

import java.lang.reflect.Field;

public class ConfigHeader extends BasicOption {

    public ConfigHeader(Field field, Object parent, String name, String category, String subcategory, int size) {
        super(field, parent, name, category, subcategory, size);
    }

    public static ConfigHeader create(Field field, Object parent)  {
        Header header = field.getAnnotation(Header.class);
        return new ConfigHeader(field, parent, header.text(), header.category(), header.subcategory(), header.size());
    }

    @Override
    public void draw(long vg, int x, int y) {
        Scissor scissor = ScissorManager.scissor(vg, x, y, size == 1 ? 480 : 992, 32);
        RenderManager.drawText(vg, name, x, y + 17, Colors.WHITE_90, 24, Fonts.MEDIUM);
        ScissorManager.resetScissor(vg, scissor);
    }

    @Override
    public float getHeight() {
        return 32;
    }
}
