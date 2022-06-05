package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UScreen;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TestNanoVGGui_Test extends UScreen {

    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks);
        drawRect(0, 0, width, height, Color.BLACK.getRGB());
        long startTime = System.nanoTime();
        RenderManager.setupAndDraw((vg) -> {
            RenderManager.drawRect(vg, 0, 0, 100, 100, Color.BLUE.getRGB());
            RenderManager.drawRoundedRect(vg, 305, 305, 100, 100, Color.YELLOW.getRGB(), 8);
            RenderManager.drawText(vg, "Hello!", 100, 100, Color.WHITE.getRGB(), 50, Fonts.BOLD);
            RenderManager.drawLine(vg, 0, 0, 100, 100, 7, Color.PINK.getRGB());
            RenderManager.drawCircle(vg, 200, 200, 50, Color.WHITE.getRGB());
            RenderManager.drawText(vg, (float) (System.nanoTime() - startTime) / 1000000f + "ms", 500, 500, Color.WHITE.getRGB(), 100, Fonts.BOLD);
        });
    }
}
