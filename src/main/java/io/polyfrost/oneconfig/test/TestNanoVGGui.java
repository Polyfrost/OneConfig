package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.lwjgl.NanoVGUtils;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

public class TestNanoVGGui extends GuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        NanoVGUtils.setupAndDraw((vg) -> {
            NanoVGUtils.drawRect(vg, 0, 0, 300, 300, Color.BLUE.getRGB());
            NanoVGUtils.drawRoundedRect(vg, 305, 305, 100, 100, Color.BLACK.getRGB(), 8);
            NanoVGUtils.drawString(vg, "Hello!", 500, 500, Color.BLACK.getRGB(), 50);
        });
    }
}
