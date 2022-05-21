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
    private int colorEnabled;
    private int colorDisabled;
    private float percentOn = 0f;

    public ConfigSwitch(Field field, Object parent, String name, int size) {
        super(field, parent, name, size);

    }

    @Override
    public void draw(long vg, int x, int y) {
        boolean toggled = false;
        try {
            toggled = (boolean) get();
        } catch (IllegalAccessException ignored) {
        }
        int x2 = x + 3 + (int) (percentOn * 18);
        boolean hovered = InputUtils.isAreaHovered(x, y, 42, 32);
        colorDisabled = ColorUtils.smoothColor(colorDisabled, OneConfigConfig.GRAY_400, OneConfigConfig.GRAY_300, hovered, 40f);
        colorEnabled = ColorUtils.smoothColor(colorEnabled, OneConfigConfig.PRIMARY_600, OneConfigConfig.PRIMARY_500, hovered, 40f);
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawRoundedRect(vg, x, y + 4, 42, 24, colorDisabled, 12f);
        if (percentOn != 0)
            RenderManager.drawRoundedRect(vg, x, y + 4, 42, 24, ColorUtils.setAlpha(colorEnabled, (int) (255 * percentOn)), 12f);
        RenderManager.drawRoundedRect(vg, x2, y + 7, 18, 18, OneConfigConfig.WHITE, 9f);
        RenderManager.drawString(vg, name, x + 50, y + 17, OneConfigConfig.WHITE, 14f, Fonts.MEDIUM);

        if (InputUtils.isAreaClicked(x, y, 42, 32) && isEnabled()) {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        percentOn = MathUtils.clamp(MathUtils.easeOut(percentOn, toggled ? 1f : 0f, 75));
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
