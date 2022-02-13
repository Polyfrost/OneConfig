package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.themes.Theme;
import io.polyfrost.oneconfig.themes.ThemeElement;
import io.polyfrost.oneconfig.themes.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

import static io.polyfrost.oneconfig.renderer.Renderer.clamp;
import static io.polyfrost.oneconfig.renderer.Renderer.easeOut;

public class Window extends GuiScreen {
    private float currentProgress = 0f;
    public static Window currentWindow;
    private final Theme t = Themes.getActiveTheme();
    private int guiScaleToRestore = -1;

    public Window() {
        super.initGui();
        currentWindow = this;
        guiScaleToRestore = Minecraft.getMinecraft().gameSettings.guiScale;
        Minecraft.getMinecraft().gameSettings.guiScale = 1;
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        currentProgress = clamp(easeOut(currentProgress, 1f));
        int alphaVal = (int) (50 * currentProgress);
        drawGradientRect(0, 0, super.width, super.height, new Color(80, 80, 80, alphaVal).getRGB(), new Color(80, 80, 80, alphaVal + 10).getRGB());
        drawWindow();

    }

    public void drawWindow() {
        Color testingColor = new Color(127, 144, 155, 255);
        //System.out.println(testingColor.getRGB());
        int middleX = this.width / 2;
        int middleY = this.height / 2;
        int left = middleX - 600;
        int right = (int) (left + 1200 * currentProgress);
        int top = middleY - 350;
        int bottom = (int) (top + 700 * currentProgress);
        Gui.drawRect(left - 1, top - 1, right + 1, bottom + 1, testingColor.getRGB());
        Gui.drawRect(left, top, right, bottom, t.getBaseColor().getRGB());

        Gui.drawRect(left, top, right, top + 100, t.getTitleBarColor().getRGB());
        Gui.drawRect(left, top + 100, right, top + 101, testingColor.getRGB());

        t.getTextureManager().draw(ThemeElement.ALL_MODS, 10, 10, 32, 32);
    }

    public static Window getWindow() {
        return currentWindow;
    }

    @Override
    public void onGuiClosed() {
        if (guiScaleToRestore != -1) {
            Minecraft.getMinecraft().gameSettings.guiScale = guiScaleToRestore;
        }
    }
}
