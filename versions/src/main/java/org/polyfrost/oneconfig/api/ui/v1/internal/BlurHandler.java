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

package org.polyfrost.oneconfig.api.ui.v1.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.ui.v1.screen.BlurScreen;
import org.polyfrost.oneconfig.internal.mixin.ShaderGroupAccessor;

import java.util.List;

/**
 * An adapted & optimized implementation of the BlurMC mod by tterrag1098, later modified by boomboompower.
 * <p>
 * For the original source see <a href="https://github.com/tterrag1098/Blur/blob/1.8.9/src/main/java/com/tterrag/blur/Blur.java">here.</a>
 * taken the <a href="https://github.com/tterrag1098/Blur/blob/1.8.9/LICENSE">MIT Licence.</a>
 * <p>
 * Modifications based on source from ToggleChat. See <a href="https://github.com/boomboompower/ToggleChat/blob/master/LICENSE">here</a> for that licence.
 *
 * @author tterrag1098, boomboompower, nextday
 */
public final class BlurHandler {
    public static final BlurHandler INSTANCE = new BlurHandler();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Blur");
    private final ResourceLocation blurShader = new ResourceLocation("shaders/post/fade_in_blur.json");
    private ShaderUniform su;
    private long start;
    private float progress = 0;

    private BlurHandler() {
        EventHandler.ofRemoving(ScreenOpenEvent.class, e -> reloadBlur(e.getScreen())).register();
        EventHandler.of(RenderEvent.End.class, () -> {
            if (su == null) return;
            if (progress >= 5f) return;
            su.set(getBlurStrengthProgress());
        }).register();
    }

    public static void init() {
        // will call <clinit>
    }

    public static boolean isBlurring() {
        return INSTANCE.su != null;
    }

    /**
     * Activates/deactivates the blur in the current world if
     * one of many conditions are met, such as no current other shader
     * is being used, we actually have the blur setting enabled
     */
    private boolean reloadBlur(Object gui) {
        // Don't do anything if no world is loaded
        if (Minecraft.getMinecraft().theWorld == null) {
            return false;
        }
        if (gui == null) {
            tryStop();
            return false;
        }

        // If a shader is not already active and the UI is
        // a one of ours, we should load our own blur!
        if (gui instanceof BlurScreen && ((BlurScreen) gui).hasBackgroundBlur()) {
            if (!isShaderActive()) {
                //#if FABRIC
                //$$ ((org.polyfrost.oneconfig.internal.mixin.fabric.GameRendererAccessor) MinecraftClient.getInstance().gameRenderer).invokeLoadShader(this.blurShader);
                //#else
                Minecraft.getMinecraft().entityRenderer.loadShader(this.blurShader);
                //#endif

                try {
                    ShaderGroup group = Minecraft.getMinecraft().entityRenderer.getShaderGroup();
                    if (group == null) return false;
                    List<Shader> shaders = ((ShaderGroupAccessor) group).getListShaders();
                    if (shaders == null) return false;

                    // Iterate through the list of shaders.
                    for (Shader shader : shaders) {
                        ShaderManager sm = shader.getShaderManager();
                        ShaderUniform su = sm.getShaderUniform("Progress");
                        if (su == null) continue;
                        this.su = su;
                    }
                    if (su == null) {
                        LOGGER.error("Failed to get ShaderUniform for blur on GUI {}. It has been disabled. Please report this!", gui.getClass().getName());
                        return true;
                    }
                    this.start = System.currentTimeMillis();
                    this.progress = 0;
                } catch (Exception ex) {
                    LOGGER.error("An error occurred while updating OneConfig's blur. It has been disabled. Please report this!", ex);
                    return true;
                }
            } else {
                tryStop();
            }
        }
        return false;
    }

    private void tryStop() {
        ShaderGroup sg = Minecraft.getMinecraft().entityRenderer.getShaderGroup();
        if (sg == null) return;
        String name = sg.getShaderGroupName();

        // Only stop our specific blur ;)
        if (!name.endsWith("fade_in_blur.json")) {
            return;
        }
        su = null;
        Minecraft.getMinecraft().entityRenderer.stopUseShader();
    }

    /**
     * Returns the strength of the blur as determined by the duration the effect of the blur.
     * <p>
     * The strength of the blur does not go above 5.0F.
     */
    private float getBlurStrengthProgress() {
        return Math.min((System.currentTimeMillis() - this.start) / 50F, 5.0F);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isShaderActive() {
        return Minecraft.getMinecraft().entityRenderer.getShaderGroup() != null
                //#if MC<=11202
                && net.minecraft.client.renderer.OpenGlHelper.shadersSupported
                //#endif
                ;
    }
}
