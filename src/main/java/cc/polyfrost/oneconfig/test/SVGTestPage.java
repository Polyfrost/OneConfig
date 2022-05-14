package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.nanovg.NanoVG;

public class SVGTestPage extends GuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderManager.setupAndDraw((vg) -> {
            NanoVG.nvgScale(vg, 25, 25);
            RenderManager.drawSvg(vg, "/assets/oneconfig/svg/Home.svg");
        });
    }
}
