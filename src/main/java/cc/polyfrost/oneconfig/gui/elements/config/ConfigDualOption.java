package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.DualOption;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutCubic;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputUtils;

import java.lang.reflect.Field;

public class ConfigDualOption extends BasicOption {
    private final String left, right;
    private Animation posAnimation;

    public ConfigDualOption(Field field, Object parent, String name, String category, String subcategory, int size, String left, String right) {
        super(field, parent, name, category, subcategory, size);
        this.left = left;
        this.right = right;
    }

    public static ConfigDualOption create(Field field, Object parent) {
        DualOption dualOption = field.getAnnotation(DualOption.class);
        return new ConfigDualOption(field, parent, dualOption.name(), dualOption.category(), dualOption.subcategory(), dualOption.size(), dualOption.left(), dualOption.right());
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
            if (posAnimation == null) posAnimation = new DummyAnimation(toggled ? 356 : 228);
        } catch (IllegalAccessException ignored) {
        }
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        boolean hoveredLeft = InputUtils.isAreaHovered(x + 226, y, 128, 32) && isEnabled();
        boolean hoveredRight = InputUtils.isAreaHovered(x + 354, y, 128, 32) && isEnabled();
        RenderManager.drawText(vg, name, x, y + 16, Colors.WHITE_90, 14f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 226, y, 256, 32, Colors.GRAY_600, 12f);
        RenderManager.drawRoundedRect(vg, x + posAnimation.get(), y + 2, 124, 28, Colors.PRIMARY_600, 10f);
        if (!hoveredLeft && isEnabled()) RenderManager.setAlpha(vg, 0.8f);
        RenderManager.drawText(vg, left, x + 290 - RenderManager.getTextWidth(vg, left, 12f, Fonts.MEDIUM) / 2, y + 17, Colors.WHITE, 12f, Fonts.MEDIUM);
        if (isEnabled()) RenderManager.setAlpha(vg, 1f);
        if (!hoveredRight && isEnabled()) RenderManager.setAlpha(vg, 0.8f);
        RenderManager.drawText(vg, right, x + 418 - RenderManager.getTextWidth(vg, right, 12f, Fonts.MEDIUM) / 2, y + 17, Colors.WHITE, 12f, Fonts.MEDIUM);

        RenderManager.setAlpha(vg, 1);
        if ((hoveredLeft && toggled || hoveredRight && !toggled) && InputUtils.isClicked()) {
            toggled = !toggled;
            posAnimation = new EaseInOutCubic(175, 228, 356, !toggled);
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
    }
}
