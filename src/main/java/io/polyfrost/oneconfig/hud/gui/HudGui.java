package io.polyfrost.oneconfig.hud.gui;

import io.polyfrost.oneconfig.hud.HudCore;
import io.polyfrost.oneconfig.hud.interfaces.BasicHud;
import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

public class HudGui extends GuiScreen {
    private final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
    private BasicHud editingHud;
    private boolean isDragging;
    private boolean isScaling;
    private boolean scaleLeft;
    private boolean scaleBottom;
    private int xOffset;
    private int yOffset;
    private boolean wereKeypressesEnabled;

    @Override
    public void initGui() {
        HudCore.editing = true;
        wereKeypressesEnabled = Keyboard.areRepeatEventsEnabled();
        Keyboard.enableRepeatEvents(true);
    }

    //TODO: making scaling work properly everywhere instead of only in first quadrant
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, this.width, this.height, new Color(80, 80, 80, 50).getRGB());

        for (BasicHud hud : HudCore.huds) {
            int width = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
            int height = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
            int x = (int) hud.getXScaled(this.width);
            int y = (int) hud.getYScaled(this.height);

            if (hud == editingHud && isScaling) {
                int newWidth = mouseX - x;
                if (scaleLeft)
                    newWidth = x - mouseX + width;
                float newScale = (float) newWidth / (width / hud.scale);
                if (newScale > 20)
                    newScale = 20;
                else if (newScale < 0.3)
                    newScale = 0.3f;
                hud.scale = newScale;
                if (scaleLeft || scaleBottom) {
                    int newX = x;
                    int newY = y;
                    if (scaleLeft)
                        newX = x + width - (hud.getWidth(newScale) + (int) (hud.paddingX * newScale));
                    if (scaleBottom)
                        newY = y + height - (hud.getHeight(newScale) + (int) (hud.paddingY * newScale));
                    setPosition(newX, newY, false);
                }
                // updating everything to new values
                width = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
                height = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
                x = (int) hud.getXScaled(this.width);
                y = (int) hud.getYScaled(this.height);
            }

            hud.drawExampleAll(x, y, hud.scale);
            int color = new Color(215, 224, 235).getRGB();
            if (editingHud == hud) {
                color = new Color(43, 159, 235).getRGB();
                if (isDragging)
                    Gui.drawRect(x, y, x + width, y + height, new Color(108, 176, 255, 60).getRGB());
            }
            Renderer.drawLine(x - 2 / 4f, y, x + width + 2 / 4f, y, 2, color);
            Renderer.drawLine(x, y, x, y + height, 2, color);
            Renderer.drawLine(x + width, y, x + width, y + height, 2, color);
            Renderer.drawLine(x - 2 / 4f, y + height, x + width + 2 / 4f, y + height, 2, color);

            if (hud == editingHud && !isDragging) {
                Gui.drawRect(x - 3, y - 3, x + 3, y + 3, new Color(43, 159, 235).getRGB());
                Gui.drawRect(x - 2, y - 2, x + 2, y + 2, new Color(252, 252, 252).getRGB());
                Gui.drawRect(x - 3, y + height - 3, x + 3, y + height + 3, new Color(43, 159, 235).getRGB());
                Gui.drawRect(x - 2, y + height - 2, x + 2, y + height + 2, new Color(252, 252, 252).getRGB());
                Gui.drawRect(x + width - 3, y - 3, x + width + 3, y + 3, new Color(43, 159, 235).getRGB());
                Gui.drawRect(x + width - 2, y - 2, x + width + 2, y + 2, new Color(252, 252, 252).getRGB());
                Gui.drawRect(x + width - 3, y + height - 3, x + width + 3, y + height + 3, new Color(43, 159, 235).getRGB());
                Gui.drawRect(x + width - 2, y + height - 2, x + width + 2, y + height + 2, new Color(252, 252, 252).getRGB());
            }
        }

        if (isDragging) {
            setPosition(mouseX - xOffset, mouseY - yOffset, true);
        }
    }

    private void setPosition(double newX, double newY, boolean snap) {
        double width = (int) (editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale);
        double height = (int) (editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale);

        if (newX < 0)
            newX = 0;
        else if (newX + width > this.width)
            newX = this.width - width;

        if (newY < 0)
            newY = 0;
        else if (newY + height > this.height)
            newY = this.height - height;

        if (newX / this.width <= 0.5)
            editingHud.xUnscaled = newX / (double) this.width;
        else
            editingHud.xUnscaled = (newX + width) / (double) this.width;
        if (newY / this.height <= 0.5)
            editingHud.yUnscaled = newY / (double) this.height;
        else
            editingHud.yUnscaled = (newY + height) / (double) this.height;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (editingHud != null) {
                int width = (int) (editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale);
                int height = (int) (editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale);
                float x = editingHud.getXScaled(this.width);
                float y = editingHud.getYScaled(this.height);
                if ((mouseX >= x - 3 && mouseX <= x + 3 || mouseX >= x + width - 3 && mouseX <= x + width + 3) &&
                        (mouseY >= y - 3 && mouseY <= y + 3 || mouseY >= y + height - 3 && mouseY <= y + height + 3)) {
                    isScaling = true;
                    scaleLeft = mouseX >= x - 3 && mouseX <= x + 3;
                    scaleBottom = mouseY >= y - 3 && mouseY <= y + 3;
                    return;
                }
            }
            editingHud = null;
            for (BasicHud hud : HudCore.huds) {
                int width = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
                int height = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
                float x = hud.getXScaled(this.width);
                float y = hud.getYScaled(this.height);
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    editingHud = hud;
                    xOffset = (int) (mouseX - x);
                    yOffset = (int) (mouseY - y);
                    isDragging = true;
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        isDragging = false;
        isScaling = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (editingHud != null) {
            float x = editingHud.getXScaled(this.width);
            float y = editingHud.getYScaled(this.height);
            switch (keyCode) {
                case Keyboard.KEY_UP:
                    setPosition(x, y - 1, false);
                    break;
                case Keyboard.KEY_DOWN:
                    setPosition(x, y + 1, false);
                    break;
                case Keyboard.KEY_LEFT:
                    setPosition(x - 1, y, false);
                    break;
                case Keyboard.KEY_RIGHT:
                    setPosition(x + 1, y, false);
                    break;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        HudCore.editing = false;
        Keyboard.enableRepeatEvents(wereKeypressesEnabled);
        for (BasicHud hud : HudCore.huds) {
            hud.xUnscaled = 0.1;
            hud.yUnscaled = 0.1;
            hud.scale = 2;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
