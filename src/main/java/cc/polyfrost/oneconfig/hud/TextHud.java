package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.renderer.RenderManager;

import java.util.List;

public abstract class TextHud extends Hud {
    protected transient List<String> lines = null;
    private transient int width;
    private transient int height;

    @Color(
            name = "Text Color"
    )
    public OneColor color = new OneColor(255, 255, 255);

    @Dropdown(
            name = "Text Type",
            options = {"No Shadow", "Shadow", "Full Shadow"}
    )
    public int textType = 0;

    public TextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    public TextHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    /**
     * This function is called every tick
     *
     * @return The new lines
     */
    protected abstract List<String> getLines();

    /**
     * This function is called every frame
     *
     * @return The new lines, null if you want to use the cached lines
     */
    protected List<String> getLinesFrequent() {
        return null;
    }

    /**
     * This function is called every tick in the move GUI
     *
     * @return The new lines
     */
    protected List<String> getExampleLines() {
        return getLines();
    }

    /**
     * This function is called every frame in the move GUI
     *
     * @return The new lines, null if you want to use the cached lines
     */
    protected List<String> getExampleLinesFrequent() {
        return getLinesFrequent();
    }

    @Override
    public void draw(int x, int y, float scale) {
        List<String> frequentLines = HudCore.editing ? getExampleLinesFrequent() : getLinesFrequent();
        if (frequentLines != null) lines = frequentLines;
        if (lines == null) return;

        int textY = y;
        width = 0;
        for (String line : lines) {
            RenderManager.drawScaledString(line, x, textY, color.getRGB(), RenderManager.TextType.toType(textType), scale);
            width = Math.max(width, UMinecraft.getFontRenderer().getStringWidth(line));
            textY += 12 * scale;
        }
        height = (int) ((textY - y) / scale - 3);
    }

    @Override
    public int getWidth(float scale) {
        return (int) (width * scale);
    }

    @Override
    public int getHeight(float scale) {
        return (int) (height * scale);
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (!HudCore.editing) lines = getLines();
        else lines = getExampleLines();
    }
}