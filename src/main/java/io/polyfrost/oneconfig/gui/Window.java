package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.gui.elements.OCBlock;
import io.polyfrost.oneconfig.themes.Theme;
import io.polyfrost.oneconfig.themes.ThemeElement;
import io.polyfrost.oneconfig.themes.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

import static io.polyfrost.oneconfig.renderer.Renderer.clamp;
import static io.polyfrost.oneconfig.renderer.Renderer.easeOut;

public class Window extends GuiScreen {
    private float currentProgress = 0f;
    public static Window currentWindow;
    private final Theme t = Themes.getActiveTheme();
    private final int guiScaleToRestore;
    OCBlock block = new OCBlock(-1, 100, 200);

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
        int left = middleX - 800;
        int right = (int) (1600 * currentProgress);
        int top = middleY - 512;
        int bottom = (int) (1024 * currentProgress);
        //Gui.drawRect(left - 1, top - 1, right + 1, bottom + 1, testingColor.getRGB());
        //new Color(16, 17, 19, 255).getRGB()
        t.getTextureManager().draw(ThemeElement.BACKGROUND, left, top, right, bottom);
        t.getTextureManager().draw(ThemeElement.BUTTON_OFF, left + 480, top + 40, 640, 48);
        t.getTextureManager().draw(ThemeElement.SEARCH, left + 504, top + 48, 32, 32);
        t.getFont().drawString("Search all of OneConfig", left + 548, top + 48, 1.1f, 1f, new Color(242,242,242,255).getRGB());
        t.getTextureManager().draw(ThemeElement.BUTTON_OFF, left + 1504, top + 32, 64, 64);
        t.getTextureManager().draw(ThemeElement.BUTTON_OFF, left + 1424, top + 32, 64, 64);
        t.getTextureManager().draw(ThemeElement.BUTTON_OFF, left + 1344, top + 32, 64, 64);
        block.draw(200, 300);
        //t.getTextureManager().draw(ThemeElement.CLOSE, left + 1504, top + 32, 64, 64);
        //t.getTextureManager().draw(ThemeElement.BUTTON_OFF, left + 100, top + 100, 296, 64);
        //t.getTextureManager().draw(ThemeElement.CLOSE);

        //Renderer.drawRoundRect(left,top,right,bottom,30, testingColor.getRGB());
        //Renderer.drawRoundRect(left + 1,top + 1,right - 2,bottom - 2,30, t.getBaseColor().getRGB());
        //t.getTextureManager().draw(ThemeElement.LOGO, left + 24, top + 24, 64, 64);     // 0.875
        //t.getBoldFont().drawString("OneConfig", left + 93f, top + 25, 1f,1f);
        //Gui.drawRect(left, top, right, bottom, t.getBaseColor().getRGB());

        //Gui.drawRect(left, top, right, top + 100, t.getTitleBarColor().getRGB());
        //Gui.drawRect(left, top + 100, right, top + 101, testingColor.getRGB());


        //font.drawString("OneConfig is pog!\nWow, this font renderer actually works :D", 50, 50, 1f, 1f);
    }

    public static Window getWindow() {
        return currentWindow;
    }

    @Override
    public void onGuiClosed() {
        Minecraft.getMinecraft().gameSettings.guiScale = guiScaleToRestore;
    }
}
