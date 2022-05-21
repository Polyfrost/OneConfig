package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;

import java.lang.reflect.Field;

public class ConfigDualOption extends BasicOption {
    private float percentMove = 0f;
    private final String left, right;
    int colorSelected;
    int colorUnselected;

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
        if (!isEnabled()) RenderManager.withAlpha(vg, 0.5f);
        boolean hovered = InputUtils.isAreaHovered(x + 226, y, 256, 32) && isEnabled();
        colorSelected = ColorUtils.smoothColor(colorSelected, OneConfigConfig.BLUE_600, OneConfigConfig.BLUE_500, hovered, 40f);
        colorUnselected = ColorUtils.smoothColor(colorUnselected, OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, hovered, 40f);
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 226, y, 256, 32, colorUnselected, 12f);
        int x1 = (int) (x + 228 + (percentMove * 128));
        RenderManager.drawRoundedRect(vg, x1, y + 2, 124, 28, colorSelected, 10f);
        RenderManager.drawString(vg, left, x + 290 - RenderManager.getTextWidth(vg, left, 12f, Fonts.MEDIUM) / 2, y + 17, OneConfigConfig.WHITE_90, 12f, Fonts.MEDIUM);
        RenderManager.drawString(vg, right, x + 418 - RenderManager.getTextWidth(vg, right, 12f, Fonts.MEDIUM) / 2, y + 17, OneConfigConfig.WHITE_90, 12f, Fonts.MEDIUM);

        RenderManager.withAlpha(vg, 1);
        if (hovered && InputUtils.isClicked()) {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        percentMove = MathUtils.clamp(MathUtils.easeOut(percentMove, toggled ? 1f : 0f, 75, OneConfigGui.INSTANCE.getDeltaTime()));
    }
}
