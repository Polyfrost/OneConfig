package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.data.OptionSize;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.preview.BasicPreview;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.lang.reflect.Field;

public class ConfigPreview extends BasicOption {
    private final BasicPreview preview;
    private float height;

    public ConfigPreview(Field field, Object instance, String name, String category, String subcategory, BasicPreview preview) {
        super(field, instance, name, "", category, subcategory, OptionSize.DUAL);
        this.preview = preview;
    }

    @Override
    public int getHeight() {
        return (int) height + 28;
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        RenderManager.drawRoundedRect(vg, x - 16, y, 1024, getHeight(), Colors.GRAY_900, 20);
        RenderManager.drawText(vg, name, x, y + 11, Colors.WHITE_60, 10f, Fonts.REGULAR);
        RenderManager.drawHollowRoundRect(vg, x - 8, y + 18, 1008, (height = preview.getHeight()), Colors.GRAY_300, 16f, 1.5f);
        preview.setupCallDraw(vg, x - 8, y + 18, 1008, height);
    }
}
