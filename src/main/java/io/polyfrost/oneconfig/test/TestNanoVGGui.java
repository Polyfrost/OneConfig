package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.lwjgl.RenderManager;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

public class TestNanoVGGui extends GuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawRect(0, 0, width, height, Color.BLACK.getRGB());
        long startTime = System.nanoTime();
        RenderManager.setupAndDraw((vg) -> {
            //RenderManager.drawRect(vg, 0, 0, 100, 100, Color.BLUE.getRGB());
            //RenderManager.drawRoundedRect(vg, 305, 305, 100, 100, Color.YELLOW.getRGB(), 8);
            //RenderManager.drawString(vg, "Hello!", 80, 20, Color.WHITE.getRGB(), 50, Fonts.MC_REGULAR);
            //RenderManager.drawString(vg, "Hello!", 100, 100, Color.WHITE.getRGB(), 50, Fonts.INTER_BOLD);
            //RenderManager.drawLine(vg, 0, 0, 100, 100, 7, Color.PINK.getRGB());
            //RenderManager.drawCircle(vg, 200, 200, 50, Color.WHITE.getRGB());
            //RenderManager.drawString(vg, (float) (System.nanoTime() - startTime) / 1000000f + "ms", 500, 500, Color.WHITE.getRGB(), 100, Fonts.INTER_BOLD);
        });
        drawString(fontRendererObj, "Hello!", 0, 0, -1);
    } // hi
}
