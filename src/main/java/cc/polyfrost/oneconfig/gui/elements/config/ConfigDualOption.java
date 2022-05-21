package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;

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
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        boolean hoveredLeft = InputUtils.isAreaHovered(x + 226, y, 128, 32) && isEnabled();
        boolean hoveredRight = InputUtils.isAreaHovered(x + 354, y, 128, 32) && isEnabled();
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 226, y, 256, 32, OneConfigConfig.GRAY_600, 12f);
        int x1 = (int) (x + 228 + (percentMove * 128));
        RenderManager.drawRoundedRect(vg, x1, y + 2, 124, 28, OneConfigConfig.PRIMARY_600, 10f);
        if (!hoveredLeft && isEnabled()) RenderManager.setAlpha(vg, 0.8f);
        RenderManager.drawString(vg, left, x + 290 - RenderManager.getTextWidth(vg, left, 12f, Fonts.MEDIUM) / 2, y + 17, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);
        if (isEnabled()) RenderManager.setAlpha(vg, 1f);
        if (!hoveredRight && isEnabled()) RenderManager.setAlpha(vg, 0.8f);
        RenderManager.drawString(vg, right, x + 418 - RenderManager.getTextWidth(vg, right, 12f, Fonts.MEDIUM) / 2, y + 17, OneConfigConfig.WHITE, 12f, Fonts.MEDIUM);

        RenderManager.setAlpha(vg, 1);
        if ((hoveredLeft && toggled || hoveredRight && !toggled) && InputUtils.isClicked()) {
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
