/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.internal.gui;

import cc.polyfrost.oneconfig.gui.GuiPause;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.hud.Position;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.internal.hud.utils.GrabOffset;
import cc.polyfrost.oneconfig.internal.hud.utils.SnappingLine;
import cc.polyfrost.oneconfig.libs.universal.UGraphics;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.AssetHelper;
import cc.polyfrost.oneconfig.renderer.asset.NVGAsset;
import cc.polyfrost.oneconfig.utils.MathUtils;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class HudGui extends UScreen implements GuiPause {
    private static final int SNAPPING_DISTANCE = 10;
    private final HashMap<Hud, GrabOffset> editingHuds = new HashMap<>();
    private boolean isDragging;
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
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        int lineWidth = Math.max(1, Math.round(Math.min(UResolution.getWindowWidth() / 1920f, UResolution.getWindowHeight() / 1080f)));
        if (isDragging) {
            nanoVGHelper.setupAndDraw(vg -> setHudPositions(vg, mouseX, mouseY, true, true, lineWidth));
        } else if (isSelecting) {
            getHudsInRegion(selectX, selectY, mouseX, mouseY);
        } else if (isScaling && editingHuds.size() == 1) {
            nanoVGHelper.setupAndDraw(vg -> {
                Hud hud = (Hud) editingHuds.keySet().toArray()[0];
                Position position = hud.position;
                float scaleX = getXSnapping(vg, lineWidth, mouseX, position.getWidth(), false);
                hud.setScale(MathUtils.clamp((scaleX - position.getX()) / (position.getWidth() / hud.getScale()), 0.3f, 10f), true);
            });
        }

        float scaleFactor = (float) UResolution.getScaleFactor();
        for (Hud hud : HudCore.huds.values()) {
            if (!hud.isEnabled()) continue;
            Position position = hud.position;
            UGraphics.enableAlpha();
            UGraphics.enableBlend();
            hud.drawAll(matrixStack, true);
            if (editingHuds.containsKey(hud)) {
                nanoVGHelper.setupAndDraw(true, vg -> nanoVGHelper.drawRect(vg, position.getX(), position.getY(), position.getWidth(), position.getHeight(), ColorUtils.getColor(0, 128, 128, 60)));
            }
            if (hud.isLocked()) {
                nanoVGHelper.setupAndDraw(true, vg -> nanoVGHelper.drawRect(vg, position.getX(), position.getY(), position.getWidth(), position.getHeight(), ColorUtils.getColor(238, 36, 36, 60)));
            }
            nanoVGHelper.setupAndDraw(vg -> {
                nanoVGHelper.drawLine(vg, position.getX() * scaleFactor - lineWidth / 2f, position.getY() * scaleFactor - lineWidth / 2f, position.getRightX() * scaleFactor + lineWidth / 2f, position.getY() * scaleFactor - lineWidth / 2f, lineWidth, ColorUtils.getColor(255, 255, 255));
                nanoVGHelper.drawLine(vg, position.getX() * scaleFactor - lineWidth / 2f, position.getBottomY() * scaleFactor + lineWidth / 2f, position.getRightX() * scaleFactor + lineWidth / 2f, position.getBottomY() * scaleFactor + lineWidth / 2f, lineWidth, ColorUtils.getColor(255, 255, 255));
                nanoVGHelper.drawLine(vg, position.getX() * scaleFactor - lineWidth / 2f, position.getY() * scaleFactor - lineWidth / 2f, position.getX() * scaleFactor - lineWidth / 2f, position.getBottomY() * scaleFactor + lineWidth / 2f, lineWidth, ColorUtils.getColor(255, 255, 255));
                nanoVGHelper.drawLine(vg, position.getRightX() * scaleFactor + lineWidth / 2f, position.getY() * scaleFactor - lineWidth / 2f, position.getRightX() * scaleFactor + lineWidth / 2f, position.getBottomY() * scaleFactor + lineWidth / 2f, lineWidth, ColorUtils.getColor(255, 255, 255));
            });
            if (editingHuds.containsKey(hud) && editingHuds.size() == 1) {
                nanoVGHelper.setupAndDraw(true, vg -> nanoVGHelper.drawRect(vg, position.getRightX() - 4, position.getBottomY() - 4, 8, 8, ColorUtils.getColor(0, 128, 128, 200)));
            }
        }

        if (isSelecting) {
            nanoVGHelper.setupAndDraw(true, vg -> nanoVGHelper.drawRect(vg, selectX, selectY, mouseX - selectX, mouseY - selectY, ColorUtils.getColor(0, 0, 255, 100)));
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.onMouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        isDragging = false;
        isSelecting = false;
        isScaling = false;
        if (editingHuds.size() == 1) {
            Position position = ((Hud) editingHuds.keySet().toArray()[0]).position;
            if (mouseX >= position.getRightX() - 7 && mouseX <= position.getRightX() + 7 && mouseY >= position.getBottomY() - 7 && mouseY <= position.getBottomY() + 7) {
                isScaling = true;
                return;
            }
        }
        for (Hud hud : HudCore.huds.values()) {
            if (!hud.isEnabled() || !mouseClickedHud(hud, (float) mouseX, (float) mouseY) || hud.isLocked()) continue;
            if (!editingHuds.containsKey(hud)) {
                if (!UKeyboard.isCtrlKeyDown()) editingHuds.clear();
                editingHuds.put(hud, new GrabOffset());
            }
            isDragging = true;
            editingHuds.forEach((hud2, grabOffset) -> grabOffset.setOffset((float) (mouseX - hud2.position.getX()), (float) (mouseY - hud2.position.getY())));
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
        if (isDragging) return;
        editingHuds.forEach((hud, grabOffset) -> grabOffset.setOffset(-hud.position.getX(), -hud.position.getY()));
        if (keyCode == UKeyboard.KEY_UP) {
            setHudPositions(0f, -1f, false);
        } else if (keyCode == UKeyboard.KEY_DOWN) {
            setHudPositions(0f, 1f, false);
        } else if (keyCode == UKeyboard.KEY_LEFT) {
            setHudPositions(-1f, 0f, false);
        } else if (keyCode == UKeyboard.KEY_RIGHT) {
            setHudPositions(1f, 0f, false);
        }
        superSecretMethod(typedChar);
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
        for (Hud hud : HudCore.huds.values()) {
            if (!hud.isEnabled() || hud.isLocked()) continue;
            Position pos = hud.position;
            if ((x1 <= pos.getX() && x2 >= pos.getX() || x1 <= pos.getRightX() && x2 >= pos.getRightX())
                    && (y1 <= pos.getY() && y2 >= pos.getY() || y1 <= pos.getBottomY() && y2 >= pos.getBottomY()))
                editingHuds.put(hud, new GrabOffset());
        }
    }

    private void setHudPositions(long vg, float mouseX, float mouseY, boolean snap, boolean locked, int lineWidth) {
        for (Hud hud : editingHuds.keySet()) {
            GrabOffset grabOffset = editingHuds.get(hud);
            Position position = hud.position;
            float x = mouseX - grabOffset.getX();
            float y = mouseY - grabOffset.getY();

            if (editingHuds.size() == 1 && snap) {
                x = getXSnapping(vg, lineWidth, x, position.getWidth(), true);
                y = getYSnapping(vg, lineWidth, y, position.getHeight(), true);
            }

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

    private void setHudPositions(float mouseX, float mouseY, boolean locked) {
        setHudPositions(0, mouseX, mouseY, false, locked, 0);
    }

    private boolean mouseClickedHud(Hud hud, float mouseX, float mouseY) {
        Position position = hud.position;
        return mouseX >= position.getX() && mouseX <= position.getRightX() &&
                mouseY >= position.getY() && mouseY <= position.getBottomY();
    }

    private float getXSnapping(long vg, float lineWidth, float x, float width, boolean multipleSides) {
        ArrayList<Float> lines = getXSnappingLines();
        ArrayList<SnappingLine> snappingLines = new ArrayList<>();
        float closest = (float) (SNAPPING_DISTANCE / UResolution.getScaleFactor());
        for (Float line : lines) {
            SnappingLine snappingLine = new SnappingLine(line, x, width, multipleSides);
            if (Math.round(snappingLine.getDistance()) == Math.round(closest)) snappingLines.add(snappingLine);
            else if (snappingLine.getDistance() < closest) {
                closest = snappingLine.getDistance();
                snappingLines.clear();
                snappingLines.add(snappingLine);
            }
        }
        if (snappingLines.isEmpty()) return x;
        for (SnappingLine snappingLine : snappingLines) {
            snappingLine.drawLine(vg, lineWidth, true);
        }
        return snappingLines.get(0).getPosition();
    }

    private ArrayList<Float> getXSnappingLines() {
        ArrayList<Float> lines = new ArrayList<>();
        lines.add(UResolution.getScaledWidth() / 2f);
        for (Hud hud : HudCore.huds.values()) {
            if (!hud.isEnabled() || editingHuds.containsKey(hud)) continue;
            lines.add(hud.position.getX());
            lines.add(hud.position.getCenterX());
            lines.add(hud.position.getRightX());
        }
        return lines;
    }

    private float getYSnapping(long vg, float lineWidth, float y, float height, boolean multipleSides) {
        ArrayList<Float> lines = getYSnappingLines();
        ArrayList<SnappingLine> snappingLines = new ArrayList<>();
        float closest = (float) (SNAPPING_DISTANCE / UResolution.getScaleFactor());
        for (Float line : lines) {
            SnappingLine snappingLine = new SnappingLine(line, y, height, multipleSides);
            if (Math.round(snappingLine.getDistance()) == Math.round(closest)) snappingLines.add(snappingLine);
            else if (snappingLine.getDistance() < closest) {
                closest = snappingLine.getDistance();
                snappingLines.clear();
                snappingLines.add(snappingLine);
            }
        }
        if (snappingLines.isEmpty()) return y;
        for (SnappingLine snappingLine : snappingLines) {
            snappingLine.drawLine(vg, lineWidth, false);
        }
        return snappingLines.get(0).getPosition();
    }

    private ArrayList<Float> getYSnappingLines() {
        ArrayList<Float> lines = new ArrayList<>();
        lines.add(UResolution.getScaledHeight() / 2f);
        for (Hud hud : HudCore.huds.values()) {
            if (!hud.isEnabled() || editingHuds.containsKey(hud)) continue;
            lines.add(hud.position.getY());
            lines.add(hud.position.getCenterY());
            lines.add(hud.position.getBottomY());
        }
        return lines;
    }

    private String superSecretString = "";

    private void superSecretMethod(char charTyped) {
        superSecretString += charTyped;
        superSecretString = superSecretString.toLowerCase();
        if (!"blahaj".substring(0, superSecretString.length()).equals(superSecretString)
                && !"blåhaj".substring(0, superSecretString.length()).equals(superSecretString)
                && !"bigrat".substring(0, superSecretString.length()).equals(superSecretString)) {
            superSecretString = "";
            return;
        } else if (!"blahaj".equals(superSecretString)
                && !"blåhaj".equals(superSecretString)
                && !"bigrat".equals(superSecretString)) {
            return;
        }
        String url;
        switch (superSecretString) {
            case "blahaj":
            case "blåhaj":
                url = "https://blahaj.shop/api/random/image?" + UUID.randomUUID();
                break;
            case "bigrat":
                url = "https://bigrat.monster/media/bigrat.png";
                break;
            default:
                return;
        }
        superSecretString = "";
        AtomicBoolean loaded = new AtomicBoolean();
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        NanoVGHelper.INSTANCE.setupAndDraw((vg) -> loaded.set(assetHelper.loadImage(vg, url, HudGui.class)));
        if (!loaded.get()) return;
        NVGAsset image = assetHelper.getNVGImage(url);
        int w = image.getWidth();
        int h = image.getHeight();
        float s = Math.min(300f / w, 300f / h);
        float width = w * s;
        float height = h * s;
        HudCore.huds.put(new Map.Entry<Field, Object>() {
            @Override
            public Field getKey() {
                return null;
            }

            @Override
            public Object getValue() {
                return null;
            }

            @Override
            public Object setValue(Object o) {
                return null;
            }
        }, new Hud(true) {
            @Override
            protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
                NanoVGHelper.INSTANCE.setupAndDraw(true, (vg) -> NanoVGHelper.INSTANCE.drawImage(vg, url, x, y, width * scale, height * scale));
            }

            @Override
            protected float getWidth(float scale, boolean example) {
                return width * scale;
            }

            @Override
            protected float getHeight(float scale, boolean example) {
                return height * scale;
            }
        });
    }
}