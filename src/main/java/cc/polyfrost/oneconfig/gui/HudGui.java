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
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

public class HudGui extends UScreen implements GuiPause {
    private final ArrayList<Hud> editingHuds = new ArrayList<>(); // allow selection of multiple HUDS
    private Hud draggingHud;
    private float lastX;
    private float lastY;

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
        if (draggingHud != null) {
            setHudPositions(mouseX - lastX, mouseY - lastY, true);
            this.lastX = mouseX;
            this.lastY = mouseY;
        }

        RenderManager.setupAndDraw(true, vg -> {
            for (Hud hud : HudCore.huds) {
                if (!hud.isEnabled()) continue;
                Position position = hud.position;
                if (editingHuds.contains(hud))
                    RenderManager.drawRectangle(vg, position.getX(), position.getY(), position.getWidth(), position.getHeight(), new Color(0, 128, 128, 60).getRGB());
                hud.drawExampleAll(matrixStack);
                RenderManager.drawLine(vg, position.getX(), position.getY(), position.getRightX(), position.getY(), 1, new Color(255, 255, 255).getRGB());
                RenderManager.drawLine(vg, position.getX(), position.getBottomY(), position.getRightX(), position.getBottomY(), 1, new Color(255, 255, 255).getRGB());
                RenderManager.drawLine(vg, position.getX(), position.getY(), position.getX(), position.getBottomY(), 1, new Color(255, 255, 255).getRGB());
                RenderManager.drawLine(vg, position.getRightX(), position.getY(), position.getRightX(), position.getBottomY(), 1, new Color(255, 255, 255).getRGB());
            }
        });
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.onMouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            editingHuds.clear();
            draggingHud = null;
            for (Hud hud : HudCore.huds) {
                if (!hud.isEnabled() || !mouseClickedHud(hud, (float) mouseX, (float) mouseY)) continue;
                if (!editingHuds.contains(hud)) editingHuds.add(hud);
                draggingHud = hud;
                this.lastX = (float) mouseX;
                this.lastY = (float) mouseY;
            }
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, int state) {
        super.onMouseReleased(mouseX, mouseY, state);
        draggingHud = null;
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