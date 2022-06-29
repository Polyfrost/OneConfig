package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.TickDelay;

import java.util.ArrayList;
import java.util.List;

public abstract class TextHud extends Hud {
    protected transient List<String> lines = new ArrayList<>();
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

    @Switch(
            name = "Cache Text"
    )
    public boolean cacheText;

    public TextHud(boolean enabled, int x, int y, boolean caching) {
        super(enabled, x, y);
        cacheText = caching;
        new TickDelay(() -> {
            try {
                BasicOption option = BasicOption.getOption(getClass().getField("cacheText"));
                option.addHideCondition(() -> !caching);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }, 3);
        if (caching) {
            EventManager.INSTANCE.register(new TickHandler());
        }
    }

    public TextHud() {
        this(true);
    }

    public TextHud(boolean enabled) {
        this(enabled, true);
    }

    public TextHud(boolean enabled, boolean caching) {
        this(enabled, 0, 0, caching);
    }

    /**
     * This function is called every tick
     *
     * @param lines The current lines of the hud
     */
    protected abstract void getLines(List<String> lines);

    /**
     * This function is called every tick in the move GUI
     *
     * @param lines The current lines of the hud
     */
    protected void getExampleLines(List<String> lines) {
        getLines(lines);
    }

    @Override
    public void draw(int x, int y, float scale) {
// todo
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

    private class TickHandler {
        @Subscribe
        private void onTick(TickEvent event) {
            if (event.stage != Stage.START) return;
            lines.clear();
            if (!HudCore.editing) getLines(lines);
            else getExampleLines(lines);
        }
    }
}