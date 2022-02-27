package io.polyfrost.oneconfig.hud.interfaces;

import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class TextHud extends BasicHud {
    /**
     * Currently doesn't work because of double extend, will have to be redone somehow (I have no idea how yet)
     */
    private final FontRenderer fb = Minecraft.getMinecraft().fontRendererObj;
    boolean shadow = false;
    private List<String> cachedLines;
    private int cachedWidth;
    private int cachedHeight;
    boolean doExample = false;
    private List<String> cachedExampleLines;
    private int cachedExampleWidth;
    private int cachedExampleHeight;

    protected List<String> update() {
        return null;
    }

    @SubscribeEvent
    private void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        cachedLines = update();
        if (cachedLines != null) {
            cachedHeight = cachedLines.size() * (fb.FONT_HEIGHT + 3);
            cachedWidth = 0;
            for (String line : cachedLines) {
                int width = fb.getStringWidth(line);
                if (width > cachedWidth)
                    cachedWidth = width;
            }
        }
        if (doExample) {
            cachedExampleLines = updateExample();
            if (cachedExampleLines != null) {
                cachedExampleHeight = cachedExampleLines.size() * 12;
                cachedExampleWidth = 0;
                for (String line : cachedExampleLines) {
                    int width = fb.getStringWidth(line);
                    if (width > cachedExampleWidth)
                        cachedExampleWidth = width;
                }
            }
        }
    }

    protected List<String> updateExample() {
        return update();
    }

    @Override
    public void draw(int x, int y, float scale) {
        if (cachedLines != null)
            drawText(cachedLines, x, y, scale);
    }

    @Override
    public void drawExample(int x, int y, float scale) {
        doExample = true;
        if (cachedExampleLines != null)
            drawText(cachedExampleLines, x, y, scale);
    }

    private void drawText(List<String> lines, int x, int y, float scale) {
        for (int i = 0; i < lines.size(); i++) {
            Renderer.drawTextScale(lines.get(i), x, y + i * 12, 0xffffff, shadow, scale);
        }
    }

    @Override
    public int getWidth(float scale) {
        return (int) (cachedWidth * scale);
    }

    @Override
    public int getHeight(float scale) {
        return (int) (cachedHeight * scale);
    }

    @Override
    public int getExampleWidth(float scale) {
        return (int) (cachedExampleWidth * scale);
    }

    @Override
    public int getExampleHeight(float scale) {
        return (int) (cachedExampleHeight * scale);
    }
}
