package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

public class HudGui extends UScreen implements GuiPause {
    private Hud editingHud;
    private boolean isDragging;
    private boolean isScaling;
    private final boolean openOneConfigOnClose;
    private int xOffset;
    private int yOffset;

    public HudGui(boolean openOneConfigOnClose) {
        super();
        this.openOneConfigOnClose = openOneConfigOnClose;
    }

    /*@Override
    public void initScreen(int width, int height) {
        HudCore.editing = true;
        UKeyboard.allowRepeatEvents(true);
        super.initScreen(width, height);
    }

    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderManager.drawGlRect(0, 0, UResolution.getScaledWidth(), UResolution.getScaledHeight(), new Color(80, 80, 80, 50).getRGB());

        if (isDragging) {
            setPosition(mouseX - xOffset, mouseY - yOffset, true, false);
        }

        for (Hud hud : HudCore.huds) {
            if (hud.isEnabled()) processHud(matrixStack, hud, mouseX);
        }
    }

    private void processHud(UMatrixStack matrixStack, Hud hud, int mouseX) {
        if (hud == editingHud && isScaling) {
            float xFloat = hud.getXScaled(UResolution.getScaledWidth());
            float yFloat = hud.getYScaled(UResolution.getScaledHeight());
            float pos = getXSnapping(mouseX, true);
            float newWidth = pos - xFloat;
            float newScale = newWidth / ((hud.getWidth(hud.scale) + hud.paddingX * hud.scale) / hud.scale);
            if (newScale > 20)
                newScale = 20;
            else if (newScale < 0.3)
                newScale = 0.3f;
            hud.scale = newScale;

            if (xFloat / UResolution.getScaledWidth() > 0.5)
                editingHud.xUnscaled = (xFloat + (hud.getWidth(hud.scale) + hud.paddingX * hud.scale)) / (double) UResolution.getScaledWidth();
            if (yFloat / UResolution.getScaledHeight() > 0.5)
                editingHud.yUnscaled = (yFloat + (hud.getHeight(hud.scale) + hud.paddingY * hud.scale)) / (double) UResolution.getScaledHeight();
        }

        int width = (int) (hud.getExampleWidth(hud.scale) + hud.paddingX * hud.scale);
        int height = (int) (hud.getExampleHeight(hud.scale) + hud.paddingY * hud.scale);
        int x = (int) hud.getXScaled(UResolution.getScaledWidth());
        int y = (int) hud.getYScaled(UResolution.getScaledHeight());

        hud.drawExampleAll(matrixStack, x, y, hud.scale);
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
            RenderManager.setupAndDraw(true, (vg) -> RenderManager.drawCircle(vg, x + width, y + height, 3, new Color(43, 159, 235).getRGB()));
        }
    }

    private void setPosition(float newX, float newY, boolean snap, boolean loose) {
        float width = editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale;
        float height = editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale;

        if (newX < (loose ? -width * 2 : 0))
            newX = 0;
        else if (newX + width + (loose ? width * 2 : 0) > UResolution.getScaledWidth())
            newX = UResolution.getScaledWidth() - width;
        if (newY < (loose ? -height * 2 : 0))
            newY = 0;
        else if (newY + height + (loose ? height * 2 : 0) > UResolution.getScaledHeight())
            newY = UResolution.getScaledHeight() - height;

        if (snap) {
            float snapX = getXSnapping(newX, false);
            float snapY = getYSnapping(newY);
            if (snapX != newX || snapY != newY) {
                newX = snapX;
                newY = snapY;
            }
        }

        if (newX / UResolution.getScaledWidth() <= 0.5)
            editingHud.xUnscaled = newX / (double) UResolution.getScaledWidth();
        else
            editingHud.xUnscaled = (newX + width) / (double) UResolution.getScaledWidth();
        if (newY / UResolution.getScaledHeight() <= 0.5)
            editingHud.yUnscaled = newY / (double) UResolution.getScaledHeight();
        else
            editingHud.yUnscaled = (newY + height) / (double) UResolution.getScaledHeight();
    }

    private float getXSnapping(float pos, boolean rightOnly) {
        float width = editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale;
        ArrayList<Float> verticalLines = new ArrayList<>();
        for (Hud hud : HudCore.huds) {
            if (!hud.isEnabled()) continue;
            verticalLines.addAll(getXSnappingHud(hud));
        }
        getSpaceSnapping(verticalLines);
        verticalLines.add(UResolution.getScaledWidth() / 2f);
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
            RenderManager.setupAndDraw(true, (vg) -> RenderManager.drawLine(vg, finalSmallestLine, 0, finalSmallestLine, UResolution.getScaledHeight(), 1, new Color(255, 255, 255).getRGB()));
            return smallestLine - smallestOffset;
        }
        return pos;
    }

    private ArrayList<Float> getXSnappingHud(Hud hud) {
        ArrayList<Float> verticalLines = new ArrayList<>();
        if (hud == editingHud) return verticalLines;
        int hudWidth = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
        int hudX = (int) hud.getXScaled(UResolution.getScaledWidth());
        verticalLines.add((float) hudX);
        verticalLines.add((float) (hudX + hudWidth));
        return verticalLines;
    }

    private float getYSnapping(float pos) {
        float height = editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale;
        ArrayList<Float> horizontalLines = new ArrayList<>();
        for (Hud hud : HudCore.huds) {
            if (!hud.isEnabled()) continue;
            horizontalLines.addAll(getYSnappingHud(hud));
        }
        getSpaceSnapping(horizontalLines);
        horizontalLines.add(UResolution.getScaledHeight() / 2f);
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
            RenderManager.setupAndDraw(true, (vg) -> RenderManager.drawLine(vg, 0, finalSmallestLine, UResolution.getScaledWidth(), finalSmallestLine, 1, new Color(255, 255, 255).getRGB()));
            return smallestLine - smallestOffset;
        }
        return pos;
    }

    private ArrayList<Float> getYSnappingHud(Hud hud) {
        ArrayList<Float> horizontalLines = new ArrayList<>();
        if (hud == editingHud) return horizontalLines;
        int hudHeight = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
        int hudY = (int) hud.getYScaled(UResolution.getScaledHeight());
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
    public void onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.onMouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (editingHud != null) {
                int width = (int) (editingHud.getWidth(editingHud.scale) + editingHud.paddingX * editingHud.scale);
                int height = (int) (editingHud.getHeight(editingHud.scale) + editingHud.paddingY * editingHud.scale);
                float x = editingHud.getXScaled(UResolution.getScaledWidth());
                float y = editingHud.getYScaled(UResolution.getScaledHeight());
                if (mouseX >= x + width - 3 && mouseX <= x + width + 3 && mouseY >= y + height - 3 && mouseY <= y + height + 3) {
                    isScaling = true;
                    return;
                }
            }
            editingHud = null;
            for (Hud hud : HudCore.huds) {
                if (!hud.isEnabled()) continue;
                if (mouseClickedHud(hud, (int) mouseX, (int) mouseY))
                    break;
            }
        }
    }

    private boolean mouseClickedHud(Hud hud, int mouseX, int mouseY) {
        int width = (int) (hud.getWidth(hud.scale) + hud.paddingX * hud.scale);
        int height = (int) (hud.getHeight(hud.scale) + hud.paddingY * hud.scale);
        float x = hud.getXScaled(UResolution.getScaledWidth());
        float y = hud.getYScaled(UResolution.getScaledHeight());
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            editingHud = hud;
            xOffset = (int) (mouseX - x);
            yOffset = (int) (mouseY - y);
            isDragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, int state) {
        super.onMouseReleased(mouseX, mouseY, state);
        isDragging = false;
        isScaling = false;
    }

    @Override
    public void onKeyPressed(int keyCode, char typedChar, @Nullable UKeyboard.Modifiers modifiers) {
        if (editingHud != null) {
            float x = editingHud.getXScaled(UResolution.getScaledWidth());
            float y = editingHud.getYScaled(UResolution.getScaledHeight());
            if (keyCode == UKeyboard.KEY_UP) {
                setPosition(x, y - 1, false, true);
            } else if (keyCode == UKeyboard.KEY_DOWN) {
                setPosition(x, y + 1, false, true);
            } else if (keyCode == UKeyboard.KEY_LEFT) {
                setPosition(x - 1, y, false, true);
            } else if (keyCode == UKeyboard.KEY_RIGHT) {
                setPosition(x + 1, y, false, true);
            }
        }
        super.onKeyPressed(keyCode, typedChar, modifiers);
    }

    @Override
    public void onScreenClose() {
        super.onScreenClose();
        HudCore.editing = false;
        UKeyboard.allowRepeatEvents(false);
        ConfigCore.saveAll();
        if(openOneConfigOnClose) GuiUtils.displayScreen(OneConfigGui.create());
    }*/

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}