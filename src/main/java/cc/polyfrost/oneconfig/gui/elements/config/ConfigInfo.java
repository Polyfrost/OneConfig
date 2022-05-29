package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.scissor.Scissor;
import cc.polyfrost.oneconfig.lwjgl.scissor.ScissorManager;

import java.lang.reflect.Field;

public class ConfigInfo extends BasicOption {
    private final InfoType type;

    public ConfigInfo(Field field, Object parent, String name, int size, InfoType type) {
        super(field, parent, name, size);
        this.type = type;
    }

    @Override
    public void draw(long vg, int x, int y) {
        Scissor scissor = ScissorManager.scissor(vg, x, y, size == 1 ? 448 : 960, 32);
        RenderManager.drawInfo(vg, type, x, y + 4, 24);
        RenderManager.drawText(vg, name, x + 32, y + 18, OneConfigConfig.WHITE_90, 14, Fonts.MEDIUM);
        ScissorManager.resetScissor(vg, scissor);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
