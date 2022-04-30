package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.InputUtils;
import io.polyfrost.oneconfig.utils.MathUtils;

import java.lang.reflect.Field;

public class ConfigDualOption extends BasicOption {
    private float percentMove = 0f;
    private final String left, right;

    public ConfigDualOption(Field field, String name, int size, String left, String right) {
        super(field, name, size);
        this.left = left;
        this.right = right;

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
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 18f, Fonts.INTER_MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 226, y, 256, 32, OneConfigConfig.GRAY_500, 12f);
        int x1 = (int) (x + 228 + (percentMove * 128));
        RenderManager.drawRoundedRect(vg, x1, y + 2, 124, 28, OneConfigConfig.BLUE_600, 10f);
        RenderManager.drawString(vg, left, x + 290 - RenderManager.getTextWidth(vg, left, 14f, Fonts.INTER_MEDIUM) / 2, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);
        RenderManager.drawString(vg, right, x + 418 - RenderManager.getTextWidth(vg, right, 14f, Fonts.INTER_MEDIUM) / 2, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);

        if (InputUtils.isAreaClicked(x + 226, y, 256, 32)) {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        percentMove = MathUtils.clamp(MathUtils.easeOut(percentMove, toggled ? 1f : 0f, 10));
    }
}
