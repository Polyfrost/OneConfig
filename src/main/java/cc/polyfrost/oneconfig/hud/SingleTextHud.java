package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigSwitch;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import net.minecraft.client.gui.GuiChat;

import java.util.ArrayList;

public abstract class SingleTextHud extends TextHud implements Cacheable {

    private transient String cachedString = null;
    private transient final boolean caching;

    public SingleTextHud(String title) {
        this(title, true);
    }

    public SingleTextHud(String title, boolean enabled) {
        this(title, enabled, 0, 0);
    }

    public SingleTextHud(String title, boolean enabled, boolean caching) {
        this(title, enabled, 0, 0, caching);
    }

    public SingleTextHud(String title, boolean enabled, int x, int y) {
        this(title, enabled, x, y, true);
    }

    public SingleTextHud(String title, boolean enabled, int x, int y, boolean caching) {
        super(enabled, x, y);
        this.title = title;
        this.caching = caching;
        if (caching) {
            EventManager.INSTANCE.register(new TextCacher());
        }
    }

    @Override
    public void addCacheOptions(String category, String subcategory, ArrayList<BasicOption> options) {
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

    @Switch(
            name = "Brackets"
    )
    public boolean brackets = false;

    @Text(
            name = "Title"
    )
    public String title;

    @Dropdown(
            name = "Title Location",
            options = {"Left", "Right"}
    )
    public int titleLocation = 0;

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

        builder.append(example ? getExampleText() : (cachedString == null ? getText() : cachedString));

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

    private class TextCacher {
        @Subscribe
        private void onTick(TickEvent event) {
            if (event.stage == Stage.START) {
                if (cacheText) {
                    cachedString = getText();
                } else {
                    cachedString = null;
                }
            }
        }
    }
}
