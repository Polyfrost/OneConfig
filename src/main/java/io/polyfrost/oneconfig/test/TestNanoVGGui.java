package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.lwjgl.RenderManager;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

public class TestNanoVGGui extends GuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawRect(0, 0, width, height, Color.BLACK.getRGB());
        RenderManager.setupAndDraw((vg) -> {
            RenderManager.drawRect(vg, 0, 0, 100, 100, Color.BLUE.getRGB());
            RenderManager.drawRoundedRect(vg, 305, 305, 100, 100, Color.YELLOW.getRGB(), 8);
            RenderManager.drawString(vg, "Hello!", 80, 20, Color.WHITE.getRGB(), 50, "mc-regular");
            RenderManager.drawString(vg, "Hello!", 100, 100, Color.WHITE.getRGB(), 50, "inter-bold");
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/hudsettings.png", 10, 10, 400, 400);
            RenderManager.drawLine(vg, 0, 0, 100, 100, 7, Color.PINK.getRGB());
        });
        drawString(fontRendererObj, "Hello!", 0, 0, -1);
    }
}
