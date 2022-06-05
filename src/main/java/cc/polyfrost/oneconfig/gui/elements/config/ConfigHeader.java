package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.gui.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;

import java.lang.reflect.Field;

public class ConfigHeader extends BasicOption {

    public ConfigHeader(Field field, Object parent, String name, int size) {
        super(field, parent, name, size);
    }

    @Override
    public void draw(long vg, int x, int y) {
        Scissor scissor = ScissorManager.scissor(vg, x, y, size == 1 ? 480 : 992, 32);
        RenderManager.drawText(vg, name, x, y + 17, Colors.WHITE_90, 24, Fonts.MEDIUM);
        ScissorManager.resetScissor(vg, scissor);
    }


    @Override
    public int getHeight() {
        return 32;
    }
}
