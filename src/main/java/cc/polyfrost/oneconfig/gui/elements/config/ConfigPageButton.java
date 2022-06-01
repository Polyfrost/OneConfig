package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.OptionPage;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class ConfigPageButton extends BasicOption {
    public final OptionPage page;
    public final String description;
    private int backgroundColor = OneConfigConfig.GRAY_500;

    public ConfigPageButton(Field field, Object parent, String name, String description, OptionPage page) {
        super(field, parent, name, 2);
        this.description = description;
        this.page = page;
    }

    @Override
    public void draw(long vg, int x, int y) {
        int height = description.equals("") ? 64 : 96;
        boolean hovered = InputUtils.isAreaHovered(x - 2, y, 1024, height) && isEnabled();
        boolean clicked = hovered && InputUtils.isClicked();
        backgroundColor = ColorUtils.getColor(backgroundColor, ColorPalette.SECONDARY, hovered, false);

        if (hovered && Mouse.isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);

        RenderManager.drawRoundedRect(vg, x - 16, y, 1024, height, backgroundColor, 20);
        RenderManager.drawText(vg, name, x + 10, y + 32, OneConfigConfig.WHITE_90, 24, Fonts.MEDIUM);
        if (!description.equals(""))
            RenderManager.drawText(vg, name, x + 10, y + 70, OneConfigConfig.WHITE_90, 14, Fonts.MEDIUM);
        RenderManager.drawSvg(vg, SVGs.CHEVRON_RIGHT, x + 981f, y + (description.equals("") ? 20f : 36f), 13, 22);

        if (clicked) OneConfigGui.INSTANCE.openPage(new ModConfigPage(page));
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return description.equals("") ? 64 : 96;
    }

    @Override
    public boolean hasHalfSize() {
        return false;
    }
}
