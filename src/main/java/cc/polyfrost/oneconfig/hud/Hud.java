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

package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import javax.swing.*;

/**
 * Represents a HUD element in OneConfig.
 * A HUD element can be used to display useful information to the user, like FPS or CPS.
 * <p>
 * If you simply want to display text, extend {@link TextHud} or {@link SingleTextHud},
 * whichever applies to the use case. Then, override the required methods.
 * <p>
 * If you want to display something else, extend this class and override {@link Hud#getWidth(float, boolean)}, {@link Hud#getHeight(float, boolean)}, and {@link Hud#draw(UMatrixStack, float, float, float, boolean)} with the width, height, and the drawing code respectively.
 * </p>
 * <p>
 * It should also be noted that additional options to the HUD can be added simply by declaring them.
 * <pre>{@code
 *     public class TestHud extends SingleTextHud {
 *         @literal @Switch(
 *             name = "Additional Option"
 *         )
 *         public boolean additionalOption = true;
 *     }
 *     }</pre>
 * </p>
 * To register an element, add it to your OneConfig {@link Config}.
 * <pre>{@code
 *  *     public class YourConfig extends Config {
 *  *         @literal @HUD(
 *  *             name = "HUD Element"
 *  *         )
 *  *         public YourHudElement hudElement = new YourHudElement("Title");
 *  *     }
 *  *     }</pre>
 */
public abstract class Hud {
    protected boolean enabled;
    protected boolean locked;
    transient private Config config;
    public Position position;
    protected float scale;
    public int positionAlignment;
    @Exclude
    public float deltaTicks, defaultX, defaultY, defaultScale;
    @Exclude
    private boolean loaded = false;
    @Exclude
    private Position defaultPosition;

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     * @param positionAlignment Alignment of the hud
     * @param scale   Scale of the hud
     */
    public Hud(boolean enabled, float x, float y, int positionAlignment, float scale) {
        this.enabled = enabled;
        this.scale = scale;
        this.positionAlignment = positionAlignment;
        position = new Position(this, x, y, getWidth(scale, true), getHeight(scale, true));
        if (!loaded) {
            defaultPosition = position;
            defaultX = x;
            defaultY = y;
            defaultScale = scale;
            loaded = true;
        }
    }

    public Hud(boolean enabled, float x, float y, float scale) {
        this(enabled, x, y, 0, scale);
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     */
    public Hud(boolean enabled, float x, float y) {
        this(enabled, x, y, 1);
    }

    /**
     * @param enabled If the hud is enabled
     */
    public Hud(boolean enabled) {
        this(enabled, 0, 0, 1);
    }

    public Hud() {
        this(false, 0, 0, 1);
    }

    /**
     * Function called when drawing the hud
     *
     * @param matrices The UMatrixStack used for rendering in higher versions
     * @param x        Top left x-coordinate of the hud
     * @param y        Top left y-coordinate of the hud
     * @param scale    Scale of the hud
     * @param example  If the HUD is being rendered in example form
     */
    protected abstract void draw(UMatrixStack matrices, float x, float y, float scale, boolean example);

    /**
     * @param scale   Scale of the hud
     * @param example If the HUD is being rendered in example form
     * @return The width of the hud
     */
    protected abstract float getWidth(float scale, boolean example);

    /**
     * @param scale   Scale of the hud
     * @param example If the HUD is being rendered in example form
     * @return The height of the hud
     */
    protected abstract float getHeight(float scale, boolean example);

    /**
     * Function to do things before rendering anything
     *
     * @param example If the HUD is being rendered in example form
     */
    protected void preRender(boolean example) {
    }

    protected void resetPosition() {
        scale = defaultScale;
        Timer timer = new Timer(10, (wait) -> {
            Position pos = defaultPosition;
            position.setSize(pos.getWidth(), position.getHeight());
            position.setPosition(defaultX, defaultY, 1920, 1080);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Draw the background, the hud and all childed huds, used by HudCore
     */
    public void drawAll(UMatrixStack matrices, boolean example) {
        if (!example && !shouldShow()) return;
        preRender(example);
        position.setSize(getWidth(scale, example), getHeight(scale, example));
        draw(matrices, position.getX(), position.getY(), scale, example);
    }

    protected boolean shouldShow() {
        if (!showInGuis && Platform.getGuiPlatform().getCurrentScreen() != null && !(Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui))
            return false;
        if (!showInChat && Platform.getGuiPlatform().isInChat()) return false;
        return showInDebug || !Platform.getGuiPlatform().isInDebug();
    }

    /**
     * @return If the hud is enabled
     */
    public boolean isEnabled() {
        return enabled && (config == null || config.enabled);
    }

    /**
     * @return If the hud is locked
     */
    public boolean isLocked() {
        return locked && (config == null || config.enabled);
    }

    /**
     * Set the config to disable accordingly, intended for internal use
     *
     * @param config The config instance
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * @return The config of this HUD
     */
    public Config getConfig() {
        return this.config;
    }

    /**
     * @return The scale of the Hud
     */
    public float getScale() {
        return scale;
    }

    /**
     * Set a new scale value
     *
     * @param scale   The new scale
     * @param example If the HUD is being rendered in example form
     */
    public void setScale(float scale, boolean example) {
        this.scale = scale;
        position.updateSizePosition(getWidth(scale, example), getHeight(scale, example));
    }

    @Switch(
            name = "Show in Chat"
    )
    public boolean showInChat = true;

    @Switch(
            name = "Show in F3 (Debug)"
    )
    public boolean showInDebug = false;

    @Switch(
            name = "Show in GUIs"
    )
    public boolean showInGuis = true;
}