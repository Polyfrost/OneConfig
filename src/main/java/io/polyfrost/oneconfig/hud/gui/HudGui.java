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
import java.util.ArrayList;

public class HudGui extends GuiScreen {
    private final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
    private BasicHud editingHud;
    private boolean isDragging;
    private boolean isScaling;
    private int xOffset;
    private int yOffset;
    private boolean wereKeypressesEnabled;

    @Override
    public void initGui() {
        HudCore.editing = true;
        wereKeypressesEnabled = Keyboard.areRepeatEventsEnabled();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, this.width, this.height, new Color(80, 80, 80, 50).getRGB());

        if (isDragging) {
            setPosition(mouseX - xOffset, mouseY - yOffset, true);
        }

        for (BasicHud hud : HudCore.huds) {
            if (hud == editingHud && isScaling) {
                float xFloat = hud.getXScaled(this.width);
                float yFloat = hud.getYScaled(this.height);
                float pos = getXSnapping(mouseX, true);
                float newWidth = pos - xFloat;
                float newScale = newWidth / ((hud.getWidth(hud.scale) + hud.paddingX * hud.scale) / hud.scale);
                if (newScale > 20) newScale = 20;
                else if (newScale < 0.3) newScale = 0.3f;
                hud.scale = newScale;

                if (xFloat / this.width > 0.5)
                    editingHud.xUnscaled = (xFloat + (hud.getWidth(hud.scale) + hud.paddingX * hud.scale)) / (double) this.width;
                if (yFloat / this.height > 0.5)
                    editingHud.yUnscaled = (yFloat + (hud.getHeight(hud.scale) + hud.paddingY * hud.scale)) / (double) this.height;
            }

            int width = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
            int height = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
            int x = (int) hud.getXScaled(this.width);
            int y = (int) hud.getYScaled(this.height);

            hud.drawExampleAll(x, y, hud.scale);
            int color = new Color(215, 224, 235).getRGB();
            if (editingHud == hud) {
                color = new Color(43, 159, 235).getRGB();
                if (isDragging) Gui.drawRect(x, y, x + width, y + height, new Color(108, 176, 255, 60).getRGB());
            }
            Renderer.drawLine(x - 2 / 4f, y, x + width + 2 / 4f, y, 2, color);
            Renderer.drawLine(x, y, x, y + height, 2, color);
            Renderer.drawLine(x + width, y, x + width, y + height, 2, color);
            Renderer.drawLine(x - 2 / 4f, y + height, x + width + 2 / 4f, y + height, 2, color);

            if (hud == editingHud && !isDragging) {
                Gui.drawRect(x + width - 3, y + height - 3, x + width + 3, y + height + 3, new Color(43, 159, 235).getRGB());
                Gui.drawRect(x + width - 2, y + height - 2, x + width + 2, y + height + 2, new Color(252, 252, 252).getRGB());
            }
        }
    }

    private void setPosition(float newX, float newY, boolean snap) {
        float width = editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale;
        float height = editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale;

        if (newX < 0) newX = 0;
        else if (newX + width > this.width) newX = this.width - width;
        if (newY < 0) newY = 0;
        else if (newY + height > this.height) newY = this.height - height;

        if (snap) {
            newX = getXSnapping(newX, false);
            newY = getYSnapping(newY);
        }

        if (newX / this.width <= 0.5) editingHud.xUnscaled = newX / (double) this.width;
        else editingHud.xUnscaled = (newX + width) / (double) this.width;
        if (newY / this.height <= 0.5) editingHud.yUnscaled = newY / (double) this.height;
        else editingHud.yUnscaled = (newY + height) / (double) this.height;
    }

    private float getXSnapping(float pos, boolean rightOnly) {
        float width = editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale;
        ArrayList<Float> verticalLines = new ArrayList<>();
        verticalLines.add(this.width / 2f);
        for (BasicHud hud : HudCore.huds) {
            if (hud == editingHud) continue;
            int hudWidth = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
            int hudX = (int) hud.getXScaled(this.width);
            verticalLines.add((float) hudX);
            verticalLines.add((float) (hudX + hudWidth));
            verticalLines.add(hudX + hudWidth / 2f);
        }
        float smallestDiff = -1;
        float smallestLine = 0;
        float smallestOffset = 0;
        for (float lineX : verticalLines) {
            for (float offset = 0; offset <= (rightOnly ? 0 : width); offset += width / 2f) {
                if (Math.abs(lineX - pos - offset) < 5 && (Math.abs(lineX - pos - offset) < smallestDiff || smallestDiff == -1)) {
                    smallestDiff = Math.abs(lineX - pos);
                    smallestLine = lineX;
                    smallestOffset = offset;
                }
            }
        }
        if (smallestDiff != -1) {
            Renderer.drawDottedLine(smallestLine, 0, smallestLine, this.height, 2, 12, new Color(255, 255, 255).getRGB());
            return smallestLine - smallestOffset;
        }
        return pos;
    }

    private float getYSnapping(float pos) {
        float height = editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale;
        ArrayList<Float> horizontalLines = new ArrayList<>();
        horizontalLines.add(this.height / 2f);
        for (BasicHud hud : HudCore.huds) {
            if (hud == editingHud) continue;
            int hudHeight = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
            int hudY = (int) hud.getYScaled(this.height);
            horizontalLines.add((float) hudY);
            horizontalLines.add((float) (hudY + hudHeight));
            horizontalLines.add(hudY + hudHeight / 2f);
        }
        float smallestDiff = -1;
        float smallestLine = 0;
        float smallestOffset = 0;
        for (float lineY : horizontalLines) {
            for (float offset = 0; offset <= height; offset += height / 2f) {
                if (Math.abs(lineY - pos - offset) < 5 && (Math.abs(lineY - pos - offset) < smallestDiff || smallestDiff == -1)) {
                    smallestDiff = Math.abs(lineY - pos);
                    smallestLine = lineY;
                    smallestOffset = offset;
                }
            }
        }
        if (smallestDiff != -1) {
            Renderer.drawDottedLine(0, smallestLine, this.width, smallestLine, 2, 12, new Color(255, 255, 255).getRGB());
            return smallestLine - smallestOffset;
        }
        return pos;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (editingHud != null) {
                int width = (int) (editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale);
                int height = (int) (editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale);
                float x = editingHud.getXScaled(this.width);
                float y = editingHud.getYScaled(this.height);
                if (mouseX >= x + width - 3 && mouseX <= x + width + 3 && mouseY >= y + height - 3 && mouseY <= y + height + 3) {
                    isScaling = true;
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
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
