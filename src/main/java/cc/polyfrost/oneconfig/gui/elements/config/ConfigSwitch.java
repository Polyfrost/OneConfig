package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;

import java.lang.reflect.Field;

public class ConfigSwitch extends BasicOption {
    private int color;
    private float percentOn = 0f;

    public ConfigSwitch(Field field, String name, int size) {
        super(field, name, size);

    }

    @Override
    public void draw(long vg, int x, int y) {
        boolean toggled = false;
        try {
            toggled = (boolean) get();
        } catch (IllegalAccessException ignored) {
        }
        int x2 = x + 3 + (int) (percentOn * 18);
        color = ColorUtils.smoothColor(color, OneConfigConfig.GRAY_400, OneConfigConfig.BLUE_500, toggled, 20f);
        if (color == -15123643) {
            color = OneConfigConfig.GRAY_400;
        }
        RenderManager.drawRoundedRect(vg, x, y + 4, 42, 24, color, 12f);
        RenderManager.drawRoundedRect(vg, x2, y + 7, 18, 18, OneConfigConfig.WHITE, 9f);
        RenderManager.drawString(vg, name, x + 50, y + 17, OneConfigConfig.WHITE, 14f, Fonts.MEDIUM);

        if (InputUtils.isAreaClicked(x, y, 42, 32)) {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        percentOn = MathUtils.clamp(MathUtils.easeOut(percentOn, toggled ? 1f : 0f, 10));
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
