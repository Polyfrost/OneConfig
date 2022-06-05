package cc.polyfrost.oneconfig.hud.elements;

import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import gg.essential.universal.UMinecraft;

public abstract class TextHud extends BasicHud {
    public TextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Override
    public int getWidth(float scale) {
        return (int) (UMinecraft.getFontRenderer().getStringWidth(getText()) * scale);
    }

    @Override
    public int getHeight(float scale) {
        return (int) (9 * scale);
    }

    @Override
    public void draw(int x, int y, float scale) {
        RenderManager.drawScaledString(getText(), x, y, 0xffffff, false, scale);
    }

    public abstract String getText();
}
