package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;

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
