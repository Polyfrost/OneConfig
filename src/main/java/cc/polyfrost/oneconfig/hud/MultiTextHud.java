package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigSwitch;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.renderer.RenderManager;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiTextHud extends TextHud implements Conditional {
    private transient int width = 100;
    private transient int height;
    private transient List<String> cachedStrings = null;
    private transient final boolean caching;

    public MultiTextHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    public MultiTextHud(boolean enabled, boolean caching) {
        this(enabled, 0, 0, caching);
    }

    public MultiTextHud(boolean enabled, int x, int y) {
        this(enabled, x, y, true);
    }

    public MultiTextHud(boolean enabled, int x, int y, boolean caching) {
        super(enabled, x, y);
        this.caching = caching;
        if (caching) {
            EventManager.INSTANCE.register(new TextCacher());
        }
    }

    @Override
    public final void addNewOptions(String category, String subcategory, ArrayList<BasicOption> options) {
        if (caching) {
            try {
                options.add(new ConfigSwitch(getClass().getField("cacheText"), this, "Cache Text", category, subcategory, 1));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    @Exclude(type = Exclude.ExcludeType.HUD)
    public boolean cacheText = true;

    @Override
    public final int getWidth(float scale) {
        return (int) (width * scale);
    }

    @Override
    public final int getHeight(float scale) {
        return (int) (height * scale);
    }

    @Override
    public final void draw(int x, int y, float scale) {
        update(x, y, scale);
        int textY = y;
        width = 0;
        for (String line : cachedStrings == null ? getLines() : cachedStrings) {
            RenderManager.drawScaledString(line, x, textY, color.getRGB(), RenderManager.TextType.toType(textType), scale);
            width = Math.max(width, UMinecraft.getFontRenderer().getStringWidth(line));
            textY += 12 * scale;
        }
        height = (int) ((textY - y) / scale - 3);
    }

    @Override
    public final void drawExample(int x, int y, float scale) {
        update(x, y, scale);
        int textY = y;
        width = 0;
        for (String line : getExampleLines()) {
            RenderManager.drawScaledString(line, x, textY, color.getRGB(), RenderManager.TextType.toType(textType), scale);
            width = Math.max(width, UMinecraft.getFontRenderer().getStringWidth(line));
            textY += 12 * scale;
        }
        height = (int) ((textY - y) / scale - 3);
    }

    protected abstract List<String> getLines();

    protected List<String> getExampleLines() {
        return getLines();
    }

    /**
     * Ran before the HUD is drawn.
     *
     * Can be used to update values but not necessarily process / render them.
     * This should only be used if absolutely necessary.
     */
    protected void update(int x, int y, float scale) {

    }

    private class TextCacher {
        @Subscribe
        private void onTick(TickEvent event) {
            if (event.stage == Stage.START) {
                if (cacheText) {
                    cachedStrings = getLines();
                } else {
                    cachedStrings = null;
                }
            }
        }
    }
}
