package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;

import java.util.ArrayList;
import java.util.List;

public abstract class TextHud extends BasicHud {
    protected transient List<String> lines = new ArrayList<>();
    private transient int width;

    @Color(
            name = "Text Color"
    )
    protected OneColor color = new OneColor(255, 255, 255);

    @Dropdown(
            name = "Text Type",
            options = {"No Shadow", "Shadow", "Full Shadow"}
    )
    protected int textType = 0;

    public TextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
        EventManager.INSTANCE.register(new TickHandler());
    }

    public TextHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    /**
     * This function is called every tick
     *
     * @param lines Empty ArrayList to add your hud text too
     */
    protected abstract void getLines(List<String> lines, boolean example);

    /**
     * This function is called every frame
     *
     * @param lines The current lines of the hud
     */
    protected void getLinesFrequent(List<String> lines, boolean example) {

    }

    @Override
    public void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        getLinesFrequent(lines, example);
        if (lines == null || lines.size() == 0) return;

        float textY = y;
        width = 0;
        for (String line : lines) {
            RenderManager.drawScaledString(line, x, textY, color.getRGB(), RenderManager.TextType.toType(textType), scale);
            width = Math.max(width, Platform.getGLPlatform().getStringWidth(line));
            textY += 12 * scale;
        }
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        return width * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return lines == null ? 0 : (lines.size() * 12 - 4) * scale;
    }

    @Override
    public boolean shouldDrawBackground() {
        return super.shouldDrawBackground() && lines != null && lines.size() > 0;
    }

    private class TickHandler {
        @Subscribe
        private void onTick(TickEvent event) {
            if (event.stage != Stage.START || !isEnabled()) return;
            lines.clear();
            getLines(lines, HudCore.editing);
        }
    }
}