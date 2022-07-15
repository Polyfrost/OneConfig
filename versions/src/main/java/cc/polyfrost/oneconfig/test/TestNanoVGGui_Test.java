package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.gui.OneUIScreen;

import java.awt.*;

/**
 * A GUI that uses RenderManager, NanoVG, and OneUIScreen to render a simple GUI.
 *
 * @see OneUIScreen
 * @see TestKotlinNanoVGGui_Test
 * @see RenderManager
 */
public class TestNanoVGGui_Test extends OneUIScreen {

    @Override
    public void draw(long vg, float partialTicks) {
        long startTime = System.nanoTime();
        RenderManager.drawRect(vg, 0, 0, 100, 100, Color.BLUE.getRGB());
        RenderManager.drawRoundedRect(vg, 305, 305, 100, 100, Color.YELLOW.getRGB(), 8);
        RenderManager.drawText(vg, "Hello!", 100, 100, Color.WHITE.getRGB(), 50, Fonts.BOLD);
        RenderManager.drawLine(vg, 0, 0, 100, 100, 7, Color.PINK.getRGB());
        RenderManager.drawCircle(vg, 200, 200, 50, Color.WHITE.getRGB());
        RenderManager.drawText(vg, (float) (System.nanoTime() - startTime) / 1000000f + "ms", 500, 500, Color.WHITE.getRGB(), 100, Fonts.BOLD);
    }
}
