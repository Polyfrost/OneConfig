package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.lang.reflect.Field;

public class ConfigPageButton extends BasicOption {
    public final Page page;
    public final String description;
    private final ColorAnimation backgroundColor = new ColorAnimation(ColorPalette.SECONDARY);

    public ConfigPageButton(Field field, Object parent, String name, String description, String category, String subcategory, OptionPage page) {
        super(field, parent, name, category, subcategory, 2);
        this.description = description;
        this.page = new ModConfigPage(page);
    }

    public ConfigPageButton(Field field, Object parent, String name, String description, String category, String subcategory, Page page) {
        super(field, parent, name, category, subcategory, 2);
        this.description = description;
        this.page = page;
    }

    @Override
    public void draw(long vg, int x, int y) {
        int height = description.equals("") ? 64 : 96;
        boolean hovered = InputUtils.isAreaHovered(x - 16, y, 1024, height) && isEnabled();
        boolean clicked = hovered && InputUtils.isClicked();

        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);

        RenderManager.drawRoundedRect(vg, x - 16, y, 1024, height, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 20);
        RenderManager.drawText(vg, name, x + 10, y + 32, Colors.WHITE_90, 24, Fonts.MEDIUM);
        if (!description.equals(""))
            RenderManager.drawText(vg, name, x + 10, y + 70, Colors.WHITE_90, 14, Fonts.MEDIUM);
        RenderManager.drawSvg(vg, SVGs.CARET_RIGHT, x + 981f, y + (description.equals("") ? 20f : 36f), 13, 22);

        if (clicked) OneConfigGui.INSTANCE.openPage(page);
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return description.equals("") ? 64 : 96;
    }
}
