package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import net.minecraft.client.gui.GuiChat;

import java.util.List;

public abstract class MultiTextHud extends TextHud {
    private transient int width = 100;
    private transient int height;

    public MultiTextHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    public MultiTextHud(boolean enabled, int x, int y) {
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
        if (!showInGuis && UScreen.getCurrentScreen() != null && !(UScreen.getCurrentScreen() instanceof OneConfigGui)) return;
        if (!showInChat && UScreen.getCurrentScreen() instanceof GuiChat) return;
        if (!showInDebug && UMinecraft.getSettings().showDebugInfo) return;

        int textY = y;
        width = 0;
        for (String line : getLines()) {
            RenderManager.drawScaledString(line, x, textY, color.getRGB(), RenderManager.TextType.toType(textType), scale);
            width = Math.max(width, UMinecraft.getFontRenderer().getStringWidth(line));
            textY += 12 * scale;
        }
        height = (int) ((textY - y) / scale - 3);
    }

    @Override
    public void drawExample(int x, int y, float scale) {
        int textY = y;
        width = 0;
        for (String line : getExampleLines()) {
            RenderManager.drawScaledString(line, x, textY, color.getRGB(), RenderManager.TextType.toType(textType), scale);
            width = Math.max(width, UMinecraft.getFontRenderer().getStringWidth(line));
            textY += 12 * scale;
        }
        height = (int) ((textY - y) / scale - 3);
    }

    public abstract List<String> getLines();

    public List<String> getExampleLines() {
        return getLines();
    }
}
