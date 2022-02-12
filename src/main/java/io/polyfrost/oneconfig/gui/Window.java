package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

import static io.polyfrost.oneconfig.renderer.Renderer.easeOut;

public class Window extends GuiScreen {
    private static ResourceLocation location = new ResourceLocation("oneconfig", "textures/hudsettings128.png");
    private float currentProgress = 0f;
    public static Window currentWindow;

    public Window() {
        super.initGui();
        currentWindow = this;
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        currentProgress = easeOut(currentProgress, 1f);
        int alphaVal = (int) (50 * currentProgress);
        //drawGradientRect(0, 0, super.width, super.height, new Color(80, 80, 80, alphaVal).getRGB(), new Color(80, 80, 80, alphaVal + 10).getRGB());
        drawWindow();

    }

    public void drawWindow() {

    }

    public static Window getWindow() {
        return currentWindow;
    }


}
