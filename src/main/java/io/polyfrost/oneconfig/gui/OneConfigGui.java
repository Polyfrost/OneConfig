package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.OneConfig;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.gui.elements.BasicElement;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

public class OneConfigGui extends GuiScreen {
    private final BasicElement element = new BasicElement(200, 200, 1, true);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        RenderManager.setupAndDraw((vg) -> {
            if(OneConfigConfig.ROUNDED_CORNERS) {
                RenderManager.drawRoundedRect(vg, 544, 140, 1056, 800, OneConfigConfig.GRAY_800, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRoundedRect(vg, 320, 140, 244, 800, OneConfigConfig.GRAY_900_80, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRect(vg, 544, 140, 20, 800, OneConfigConfig.GRAY_800);
            } else {
                // L;
            }

            RenderManager.drawLine(vg, 544, 212, 1600, 212, 1,  OneConfigConfig.GRAY_700);
            RenderManager.drawLine(vg, 544, 140, 544, 940, 1, OneConfigConfig.GRAY_700);

            RenderManager.drawString(vg, "OneConfig", 389, 163, OneConfigConfig.WHITE, 18f, "inter-bold");
            RenderManager.drawString(vg, "By Polyfrost", 389, 183, OneConfigConfig.WHITE, 12f, "inter-regular");
            element.setColorPalette(0);
            element.draw(vg, 0, 0);

            //RenderManager.drawGradientRoundedRect(vg, 100, 100, 500, 100, OneConfigConfig.BLUE_600, OneConfigConfig.BLUE_500, OneConfigConfig.CORNER_RADIUS_WIN);



        });
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

}
