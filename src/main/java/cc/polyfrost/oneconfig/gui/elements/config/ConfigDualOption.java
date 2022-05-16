package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigDualOption extends BasicOption {
    private float percentMove = 0f;
    private final String left, right;

    public ConfigDualOption(Field field, Object parent, String name, int size, String[] options) {
        super(field, parent, name, size);
        this.left = options[0];
        this.right = options[1];

    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void draw(long vg, int x, int y) {
        boolean toggled = false;
        try {
            toggled = (boolean) get();
        } catch (IllegalAccessException ignored) {
        }
        if (!isEnabled()) NanoVG.nvgGlobalAlpha(vg, 0.5f);
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 226, y, 256, 32, OneConfigConfig.GRAY_500, 12f);
        int x1 = (int) (x + 228 + (percentMove * 128));
        RenderManager.drawRoundedRect(vg, x1, y + 2, 124, 28, OneConfigConfig.BLUE_600, 10f);
        RenderManager.drawString(vg, left, x + 290 - RenderManager.getTextWidth(vg, left, 12f, Fonts.MEDIUM) / 2, y + 17, OneConfigConfig.WHITE_90, 12f, Fonts.MEDIUM);
        RenderManager.drawString(vg, right, x + 418 - RenderManager.getTextWidth(vg, right, 12f, Fonts.MEDIUM) / 2, y + 17, OneConfigConfig.WHITE_90, 12f, Fonts.MEDIUM);

        NanoVG.nvgGlobalAlpha(vg, 1);
        if (InputUtils.isAreaClicked(x + 226, y, 256, 32) && isEnabled()) {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        percentMove = MathUtils.clamp(MathUtils.easeOut(percentMove, toggled ? 1f : 0f, 75));
    }
}
