package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.renderer.Renderer;
import io.polyfrost.oneconfig.renderer.TrueTypeFont;
import io.polyfrost.oneconfig.themes.Theme;
import io.polyfrost.oneconfig.themes.ThemeElement;
import io.polyfrost.oneconfig.themes.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

import static io.polyfrost.oneconfig.renderer.Renderer.clamp;
import static io.polyfrost.oneconfig.renderer.Renderer.easeOut;

public class Window extends GuiScreen {
    private float currentProgress = 0f;
    public static Window currentWindow;
    private final Theme t = Themes.getActiveTheme();
    private final int guiScaleToRestore;
    TrueTypeFont font;

    public Window() {
        try {
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Window.class.getResourceAsStream("/assets/oneconfig/fonts/font.ttf")));
            font = new TrueTypeFont(tempFont.deriveFont(30f), true);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
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
        Renderer.drawRoundRect(left,top,right,bottom,30, testingColor.getRGB());
        Renderer.drawRoundRect(left + 1,top + 1,right - 2,bottom - 2,30, t.getBaseColor().getRGB());
        t.getTextureManager().draw(ThemeElement.LOGO, left + 24, top + 24, 64, 64);     // 0.875
        font.drawString("OneConfig", left + 93f, top + 25, 1f,1f);
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
        font.destroy();
    }
}
