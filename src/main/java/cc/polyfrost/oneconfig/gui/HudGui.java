package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.hud.Position;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.MathUtils;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

public class HudGui extends UScreen implements GuiPause {
    private final ArrayList<Hud> editingHuds = new ArrayList<>(); // allow selection of multiple HUDS
    private boolean isDragging;
    private float lastX;
    private float lastY;
    private boolean isSelecting;
    private float selectX;
    private float selectY;
    private boolean isScaling;

    public HudGui() {
        super();
    }

    @Override
    public void initScreen(int width, int height) {
        HudCore.editing = true;
        UKeyboard.allowRepeatEvents(true);
        super.initScreen(width, height);
    }

    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isDragging) {
            setHudPositions(mouseX - lastX, mouseY - lastY, true);
            this.lastX = mouseX;
            this.lastY = mouseY;
        } else if (isSelecting) {
            getHudsInRegion(selectX, selectY, mouseX, mouseY);
        } else if (isScaling && editingHuds.size() == 1) {
            Hud hud = editingHuds.get(0);
            Position position = hud.position;
            hud.setScale(MathUtils.clamp((mouseX - position.getX()) / (position.getWidth() / hud.getScale()), 0.3f, 20f));
        }

        float scaleFactor = (float) UResolution.getScaleFactor();
        int width = Math.max(1, Math.round(Math.min(UResolution.getWindowWidth() / 1920f, UResolution.getWindowHeight() / 1080f)));
        for (Hud hud : HudCore.huds) {
            if (!hud.isEnabled()) continue;
            Position position = hud.position;
            hud.drawExampleAll(matrixStack);
            if (editingHuds.contains(hud))
                RenderManager.setupAndDraw(true, vg -> RenderManager.drawRect(vg, position.getX(), position.getY(), position.getWidth(), position.getHeight(), new Color(0, 128, 128, 60).getRGB()));
            RenderManager.setupAndDraw(vg -> {
                RenderManager.drawLine(vg, position.getX() * scaleFactor - width / 2f, position.getY() * scaleFactor - width / 2f, position.getRightX() * scaleFactor + width / 2f, position.getY() * scaleFactor - width / 2f, width, new Color(255, 255, 255).getRGB());
                RenderManager.drawLine(vg, position.getX() * scaleFactor - width / 2f, position.getBottomY() * scaleFactor + width / 2f, position.getRightX() * scaleFactor + width / 2f, position.getBottomY() * scaleFactor + width / 2f, width, new Color(255, 255, 255).getRGB());
                RenderManager.drawLine(vg, position.getX() * scaleFactor - width / 2f, position.getY() * scaleFactor - width / 2f, position.getX() * scaleFactor - width / 2f, position.getBottomY() * scaleFactor + width / 2f, width, new Color(255, 255, 255).getRGB());
                RenderManager.drawLine(vg, position.getRightX() * scaleFactor + width / 2f, position.getY() * scaleFactor - width / 2f, position.getRightX() * scaleFactor + width / 2f, position.getBottomY() * scaleFactor + width / 2f, width, new Color(255, 255, 255).getRGB());
            });
            if (editingHuds.contains(hud) && editingHuds.size() == 1)
                RenderManager.setupAndDraw(true, vg -> RenderManager.drawRect(vg, position.getRightX() - 5, position.getBottomY() - 5, 10, 10, new Color(0, 128, 128, 200).getRGB()));
        }

        if (isSelecting)
            RenderManager.setupAndDraw(true, vg -> RenderManager.drawRect(vg, selectX, selectY, mouseX - selectX, mouseY - selectY, new Color(0, 0, 255, 100).getRGB()));
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.onMouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        isDragging = false;
        isSelecting = false;
        isScaling = false;
        if (editingHuds.size() == 1) {
            Position position = editingHuds.get(0).position;
            if (mouseX >= position.getRightX() - 7 && mouseX <= position.getRightX() + 7 && mouseY >= position.getBottomY() - 7 && mouseY <= position.getBottomY() + 7) {
                isScaling = true;
                return;
            }
        }
        for (Hud hud : HudCore.huds) {
            if (!hud.isEnabled() || !mouseClickedHud(hud, (float) mouseX, (float) mouseY)) continue;
            if (!editingHuds.contains(hud)) {
                if (!UKeyboard.isCtrlKeyDown()) editingHuds.clear();
                editingHuds.add(hud);
            }
            isDragging = true;
            this.lastX = (float) mouseX;
            this.lastY = (float) mouseY;
            return;
        }
        isSelecting = true;
        selectX = (float) mouseX;
        selectY = (float) mouseY;
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, int state) {
        isDragging = false;
        isSelecting = false;
        isScaling = false;
        super.onMouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void onKeyPressed(int keyCode, char typedChar, @Nullable UKeyboard.Modifiers modifiers) {
        if (keyCode == UKeyboard.KEY_UP) {
            setHudPositions(0f, -1f, false);
        } else if (keyCode == UKeyboard.KEY_DOWN) {
            setHudPositions(0f, 1f, false);
        } else if (keyCode == UKeyboard.KEY_LEFT) {
            setHudPositions(-1f, 0f, false);
        } else if (keyCode == UKeyboard.KEY_RIGHT) {
            setHudPositions(1f, 0f, false);
        }
        super.onKeyPressed(keyCode, typedChar, modifiers);
    }

    @Override
    public void onScreenClose() {
        super.onScreenClose();
        HudCore.editing = false;
        UKeyboard.allowRepeatEvents(false);
        ConfigCore.saveAll();
        GuiUtils.displayScreen(OneConfigGui.create());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void getHudsInRegion(float x1, float y1, float x2, float y2) {
        if (x1 > x2) {
            float temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2) {
            float temp = y1;
            y1 = y2;
            y2 = temp;
        }

        editingHuds.clear();
        for (Hud hud : HudCore.huds) {
            if (!hud.isEnabled()) continue;
            Position pos = hud.position;
            if ((x1 <= pos.getX() && x2 >= pos.getX() || x1 <= pos.getRightX() && x2 >= pos.getRightX())
                    && (y1 <= pos.getY() && y2 >= pos.getY() || y1 <= pos.getBottomY() && y2 >= pos.getBottomY()))
                editingHuds.add(hud);
        }
    }

    private void setHudPositions(float xOffset, float yOffset, boolean locked) {
        for (Hud hud : editingHuds) {
            Position position = hud.position;
            float x = position.getX() + xOffset;
            float y = position.getY() + yOffset;

            if (locked) {
                if (x < 0) x = 0;
                else if (x + position.getWidth() > UResolution.getScaledWidth())
                    x = UResolution.getScaledWidth() - position.getWidth();
                if (y < 0) y = 0;
                else if (y + position.getHeight() > UResolution.getScaledHeight())
                    y = UResolution.getScaledHeight() - position.getHeight();
            }

            position.setPosition(x, y);
        }
    }

    private boolean mouseClickedHud(Hud hud, float mouseX, float mouseY) {
        Position position = hud.position;
        return mouseX >= position.getX() && mouseX <= position.getRightX() &&
                mouseY >= position.getY() && mouseY <= position.getBottomY();
    }
}