package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import net.minecraft.client.gui.GuiChat;

public abstract class SingleTextHud extends TextHud {

    public SingleTextHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    public SingleTextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Switch(
            name = "Brackets"
    )
    public boolean brackets = false;

    @Text(
            name = "Title"
    )
    public String title = getDefaultTitle();

    @Dropdown(
            name = "Title Location",
            options = {"Left", "Right"}
    )
    public int titleLocation = 0;

    public abstract String getDefaultTitle();

    @Override
    public int getWidth(float scale) {
        return (int) (UMinecraft.getFontRenderer().getStringWidth(getCompleteText(false)) * scale);
    }

    @Override
    public int getExampleWidth(float scale) {
        return (int) (UMinecraft.getFontRenderer().getStringWidth(getCompleteText(true)) * scale);
    }

    @Override
    public int getHeight(float scale) {
        return (int) (UMinecraft.getFontRenderer().FONT_HEIGHT * scale);
    }

    @Override
    public void draw(int x, int y, float scale) {
        if (!showInGuis && UScreen.getCurrentScreen() != null && !(UScreen.getCurrentScreen() instanceof OneConfigGui)) return;
        if (!showInChat && UScreen.getCurrentScreen() instanceof GuiChat) return;
        if (!showInDebug && UMinecraft.getSettings().showDebugInfo) return;

        RenderManager.drawScaledString(getCompleteText(false), x, y, color.getRGB(), RenderManager.TextType.toType(textType), scale);
    }

    @Override
    public void drawExample(int x, int y, float scale) {
        RenderManager.drawScaledString(getCompleteText(true), x, y, color.getRGB(), RenderManager.TextType.toType(textType), scale);
    }

    protected final String getCompleteText(boolean example) {
        boolean showTitle = !title.trim().isEmpty();
        StringBuilder builder = new StringBuilder();
        if (brackets) {
            builder.append("[");
        }

        if (showTitle && titleLocation == 0) {
            builder.append(title).append(": ");
        }

        builder.append(example ? getExampleText() : getText());

        if (showTitle && titleLocation == 1) {
            builder.append(" ").append(title);
        }

        if (brackets) {
            builder.append("]");
        }
        return builder.toString();
    }

    public abstract String getText();

    public String getExampleText() {
        return getText();
    }
}
