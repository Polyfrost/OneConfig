/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.internal.ui;

import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.polyfrost.oneconfig.api.events.event.RenderEvent;
import org.polyfrost.oneconfig.api.events.event.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.events.invoke.EventHandler;
import org.polyfrost.oneconfig.internal.mixin.ShaderGroupAccessor;
import org.polyfrost.oneconfig.libs.universal.UMinecraft;
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.oneconfig.ui.screen.BlurScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * An adapted implementation of the BlurMC mod by tterrag1098.
 * <p>
 * For the original source see <a href="https://github.com/tterrag1098/Blur/blob/1.8.9/src/main/java/com/tterrag/blur/Blur.java">...</a>
 * For the public license, see <a href="https://github.com/tterrag1098/Blur/blob/1.8.9/LICENSE">...</a>
 * <p>
 * License available under <a href="https://github.com/boomboompower/ToggleChat/blob/master/src/main/resources/licenses/BlurMC-License.txt">...</a>
 *
 * @author tterrag1098, boomboompower
 * <p>
 * Taken from ToggleChat
 * <a href="https://github.com/boomboompower/ToggleChat/blob/master/LICENSE">...</a>
 */
public final class BlurHandler {
    public static final BlurHandler INSTANCE = new BlurHandler();
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/Blur");

    private final ResourceLocation blurShader = new ResourceLocation("shaders/post/fade_in_blur.json");
    private ShaderUniform su;
    private long start;
    private float progress = 0;

    public static void init() {
        // will call <clinit>
    }

    private BlurHandler() {
        EventHandler.of(ScreenOpenEvent.class, e -> reloadBlur(e.screen)).register();
        EventHandler.of(RenderEvent.End.class, e -> {
            if (UScreen.getCurrentScreen() == null) {
                return;
            }
            if (!isShaderActive()) {
                return;
            }
            if (progress >= 5f || su == null) return;
            su.set(getBlurStrengthProgress());
        }).register();
    }

    /**
     * Activates/deactivates the blur in the current world if
     * one of many conditions are met, such as no current other shader
     * is being used, we actually have the blur setting enabled
     */
    private void reloadBlur(Object gui) {
        // Don't do anything if no world is loaded
        if (UMinecraft.getWorld() == null) {
            return;
        }
        if (gui == null) {
            tryStop();
            return;
        }

        // If a shader is not already active and the UI is
        // a one of ours, we should load our own blur!
        if (gui instanceof BlurScreen && ((BlurScreen) gui).hasBackgroundBlur()) {
            if (!isShaderActive()) {
                //#if FABRIC
                //$$ ((org.polyfrost.oneconfig.internal.mixin.GameRendererAccessor) UMinecraft.getMinecraft().gameRenderer).invokeLoadShader(this.blurShader);
                //#else
                UMinecraft.getMinecraft().entityRenderer.
                        //#if MC<11700
                        loadShader
                        //#else
                        //$$ loadEffect
                        //#endif
                                (this.blurShader);
                //#endif

                this.start = System.currentTimeMillis();
                this.progress = 0;
                try {
                    final List<Shader> listShaders = ((ShaderGroupAccessor) UMinecraft.getMinecraft().entityRenderer.getShaderGroup()).getListShaders();

                    // Should not happen. Something bad happened.
                    if (listShaders == null) {
                        return;
                    }

                    // Iterate through the list of shaders.
                    for (Shader shader : listShaders) {
                        ShaderUniform su = shader.getShaderManager().getShaderUniform("Progress");
                        if (su == null) continue;

                        this.su = su;
                    }
                    if (su == null) throw new IllegalStateException("Failed to get ShaderUniform for blur on GUI " + gui.getClass().getName());
                } catch (Exception ex) {
                    LOGGER.error("An error.png occurred while updating OneConfig's blur. Please report this!", ex);
                }
            } else {
                tryStop();
            }
        }
    }

    private void tryStop() {
        ShaderGroup sg = UMinecraft.getMinecraft().entityRenderer.getShaderGroup();
        if (sg == null) return;
        String name = sg.getShaderGroupName();

        // Only stop our specific blur ;)
        if (!name.endsWith("fade_in_blur.json")) {
            return;
        }
        su = null;
        UMinecraft.getMinecraft().entityRenderer.stopUseShader();
    }

    public static boolean isBlurring() {
        return (UScreen.getCurrentScreen() instanceof BlurScreen && ((BlurScreen) UScreen.getCurrentScreen()).hasBackgroundBlur());
    }

    /**
     * Returns the strength of the blur as determined by the duration the effect of the blur.
     * <p>
     * The strength of the blur does not go below 5.0F.
     */
    private float getBlurStrengthProgress() {
        return Math.min((System.currentTimeMillis() - this.start) / 50F, 5.0F);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isShaderActive() {
        return UMinecraft.getMinecraft().entityRenderer.getShaderGroup() != null
                //#if MC<=11202
                && net.minecraft.client.renderer.OpenGlHelper.shadersSupported
                //#endif
                ;
    }
}
