package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.scissor.Scissor;
import cc.polyfrost.oneconfig.lwjgl.scissor.ScissorManager;

import java.lang.reflect.Field;

public class ConfigInfo extends BasicOption {
    private Images image;

    public ConfigInfo(Field field, String name, int size, InfoType type) {
        super(field, name, size);
        switch (type) {
            case INFO:
                image = Images.INFO;
                break;
            case SUCCESS:
                image = Images.SUCCESS;
                break;
            case WARNING:
                image = Images.WARNING;
                break;
            case ERROR:
                image = Images.ERROR;
                break;
        }
    }

    @Override
    public void draw(long vg, int x, int y) {
        Scissor scissor = ScissorManager.scissor(vg, x, y, size == 1 ? 448 : 960, 32);
        RenderManager.drawImage(vg, image, x, y + 4, 24, 24);
        RenderManager.drawString(vg, name, x + 32, y + 18, OneConfigConfig.WHITE_90, 12, Fonts.MEDIUM);
        ScissorManager.resetScissor(vg, scissor);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
