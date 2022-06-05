package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.renderer.RenderManager;
import gg.essential.universal.UMinecraft;

import java.util.List;

public abstract class TextHud extends BasicHud {
    private transient int width = 100;
    private transient int height;
    public TextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Override
    public int getWidth(float scale) {
        return (int) (width * scale);
    }

    @Override
    public int getHeight(float scale) {
        return (int) (height * scale);
    }

    @Override
    public void draw(int x, int y, float scale) {
        int textY = y;
        width = 0;
        for (String line : getLines()) {
            RenderManager.drawScaledString(line, x, textY, 0xffffff, false, scale);
            width = Math.max(width, UMinecraft.getFontRenderer().getStringWidth(line));
            textY += 12 * scale;
        }
        height = (int) ((textY - y) / scale);
    }

    public abstract List<String> getLines();
}
