package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.core.ConfigCore;
import cc.polyfrost.oneconfig.hud.HudCore;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class HudGui extends GuiScreen {
    private BasicHud editingHud;
    private boolean isDragging;
    private boolean isScaling;
    private int xOffset;
    private int yOffset;

    @Override
    public void initGui() {
        HudCore.editing = true;
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderManager.drawGlRect(0, 0, this.width, this.height, new Color(80, 80, 80, 50).getRGB());

        if (isDragging) {
            setPosition(mouseX - xOffset, mouseY - yOffset, true);
        }

        for (BasicHud hud : HudCore.huds) {
            if (hud.enabled) processHud(hud, mouseX);
        }
    }

    private void processHud(BasicHud hud, int mouseX) {
        if (hud == editingHud && isScaling) {
            float xFloat = hud.getXScaled(this.width);
            float yFloat = hud.getYScaled(this.height);
            float pos = getXSnapping(mouseX, true);
            float newWidth = pos - xFloat;
            float newScale = newWidth / ((hud.getWidth(hud.scale) + hud.paddingX * hud.scale) / hud.scale);
            if (newScale > 20)
                newScale = 20;
            else if (newScale < 0.3)
                newScale = 0.3f;
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

        if (hud.parent == null)
            hud.drawExampleAll(x, y, hud.scale, true);
        int color = new Color(215, 224, 235).getRGB();
        if (editingHud == hud) {
            color = new Color(43, 159, 235).getRGB();
            if (isDragging)
                RenderManager.drawGlRect(x, y, width, height, new Color(108, 176, 255, 60).getRGB());
        }
        int finalColor = color;
        RenderManager.setupAndDraw(true, (vg) -> {
            RenderManager.drawLine(vg, x - 2 / 4f, y, x + width + 2 / 4f, y, 1, finalColor);
            RenderManager.drawLine(vg, x, y, x, y + height, 1, finalColor);
            RenderManager.drawLine(vg, x + width, y, x + width, y + height, 1, finalColor);
            RenderManager.drawLine(vg, x - 2 / 4f, y + height, x + width + 2 / 4f, y + height, 1, finalColor);
        });

        if (hud == editingHud && !isDragging) {
            RenderManager.setupAndDraw(true, (vg) -> {
                RenderManager.drawCircle(vg, x + width, y + height, 3, new Color(43, 159, 235).getRGB());
            });
        }

        if (hud.childBottom != null) processHud(hud.childBottom, mouseX);
        if (hud.childRight != null) processHud(hud.childRight, mouseX);
    }

    private void setPosition(float newX, float newY, boolean snap) {
        float width = editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale;
        float height = editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale;

        /* Childing disabled since it still needs some extra work
        if (editingHud.childRight != null) {
            HudCore.huds.add(editingHud.childRight);
            editingHud.childRight.parent = null;
            editingHud.childRight = null;
        }
        if (editingHud.childBottom != null) {
            HudCore.huds.add(editingHud.childBottom);
            editingHud.childBottom.parent = null;
            editingHud.childBottom = null;
        }
        if (editingHud.parent != null) {
            HudCore.huds.add(editingHud);
            if (editingHud.parent.childBottom == editingHud)
                editingHud.parent.childBottom = null;
            else if (editingHud.parent.childRight == editingHud)
                editingHud.parent.childRight = null;
            editingHud.parent = null;
        }*/

        if (newX < 0)
            newX = 0;
        else if (newX + width > this.width)
            newX = this.width - width;
        if (newY < 0)
            newY = 0;
        else if (newY + height > this.height)
            newY = this.height - height;

        if (snap) {
            float snapX = getXSnapping(newX, false);
            float snapY = getYSnapping(newY);
            if (snapX != newX || snapY != newY) {
                newX = snapX;
                newY = snapY;
                for (BasicHud hud : HudCore.huds) {
                    if (!hud.enabled) continue;
                    if (findParent(hud, snapX, snapY))
                        break;
                }
            }
        }

        if (newX / this.width <= 0.5)
            editingHud.xUnscaled = newX / (double) this.width;
        else
            editingHud.xUnscaled = (newX + width) / (double) this.width;
        if (newY / this.height <= 0.5)
            editingHud.yUnscaled = newY / (double) this.height;
        else
            editingHud.yUnscaled = (newY + height) / (double) this.height;
    }

    private boolean findParent(BasicHud hud, float snapX, float snapY) {
        int hudWidth = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
        int hudX = (int) hud.getXScaled(this.width);
        int hudHeight = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
        int hudY = (int) hud.getYScaled(this.height);
        if (hudX + hudWidth == snapX && hudY == snapY && hud.childRight == null) {
            editingHud.parent = hud;
            hud.childRight = editingHud;
            HudCore.huds.remove(editingHud);
            return true;
        } else if (hudX == snapX && hudY + hudHeight == snapY && hud.childBottom == null) {
            editingHud.parent = hud;
            hud.childBottom = editingHud;
            HudCore.huds.remove(editingHud);
            return true;
        }
        return hud.childRight != null && findParent(hud.childRight, snapX, snapY) || hud.childBottom != null && findParent(hud.childBottom, snapX, snapY);
    }

    private float getXSnapping(float pos, boolean rightOnly) {
        float width = editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale;
        ArrayList<Float> verticalLines = new ArrayList<>();
        for (BasicHud hud : HudCore.huds) {
            if (!hud.enabled) continue;
            verticalLines.addAll(getXSnappingHud(hud));
        }
        getSpaceSnapping(verticalLines);
        verticalLines.add(this.width / 2f);
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
            float finalSmallestLine = smallestLine;
            RenderManager.setupAndDraw(true, (vg) -> RenderManager.drawLine(vg, finalSmallestLine, 0, finalSmallestLine, this.height, 1, new Color(255, 255, 255).getRGB()));
            return smallestLine - smallestOffset;
        }
        return pos;
    }

    private ArrayList<Float> getXSnappingHud(BasicHud hud) {
        ArrayList<Float> verticalLines = new ArrayList<>();
        if (hud == editingHud) return verticalLines;
        if (hud.childRight != null) verticalLines.addAll(getXSnappingHud(hud.childRight));
        int hudWidth = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
        int hudX = (int) hud.getXScaled(this.width);
        verticalLines.add((float) hudX);
        verticalLines.add((float) (hudX + hudWidth));
        return verticalLines;
    }

    private float getYSnapping(float pos) {
        float height = editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale;
        ArrayList<Float> horizontalLines = new ArrayList<>();
        for (BasicHud hud : HudCore.huds) {
            if (!hud.enabled) continue;
            horizontalLines.addAll(getYSnappingHud(hud));
        }
        getSpaceSnapping(horizontalLines);
        horizontalLines.add(this.height / 2f);
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
            float finalSmallestLine = smallestLine;
            RenderManager.setupAndDraw(true, (vg) -> RenderManager.drawLine(vg, 0, finalSmallestLine, this.width, finalSmallestLine, 1, new Color(255, 255, 255).getRGB()));
            return smallestLine - smallestOffset;
        }
        return pos;
    }

    private ArrayList<Float> getYSnappingHud(BasicHud hud) {
        ArrayList<Float> horizontalLines = new ArrayList<>();
        if (hud == editingHud) return horizontalLines;
        if (hud.childBottom != null) horizontalLines.addAll(getYSnappingHud(hud.childBottom));
        int hudHeight = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
        int hudY = (int) hud.getYScaled(this.height);
        horizontalLines.add((float) hudY);
        horizontalLines.add((float) (hudY + hudHeight));
        return horizontalLines;
    }

    private void getSpaceSnapping(ArrayList<Float> lines) {
        ArrayList<Float> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            for (int l = i + 1; l < lines.size(); l++) {
                newLines.add(Math.max(lines.get(i), lines.get(l)) + Math.abs(lines.get(i) - lines.get(l)));
                newLines.add(Math.min(lines.get(i), lines.get(l)) - Math.abs(lines.get(i) - lines.get(l)));
            }
        }
        lines.addAll(newLines);
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
                if (!hud.enabled) continue;
                if (mouseClickedHud(hud, mouseX, mouseY))
                    break;
            }
        }
    }

    private boolean mouseClickedHud(BasicHud hud, int mouseX, int mouseY) {
        int width = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
        int height = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
        float x = hud.getXScaled(this.width);
        float y = hud.getYScaled(this.height);
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            editingHud = hud;
            xOffset = (int) (mouseX - x);
            yOffset = (int) (mouseY - y);
            isDragging = true;
            return true;
        }
        return hud.childBottom != null && mouseClickedHud(hud.childBottom, mouseX, mouseY) || hud.childRight != null && mouseClickedHud(hud.childRight, mouseX, mouseY);
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
        Keyboard.enableRepeatEvents(false);
        ConfigCore.saveAll();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}