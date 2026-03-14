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

import dev.deftu.omnicore.api.OmniResourceLocation;
import net.minecraft.client.Minecraft;
//#if MC <= 1.12.2
//$ import net.minecraft.client.shader.Shader;
//$ import net.minecraft.client.shader.ShaderGroup;
//$ import net.minecraft.client.shader.ShaderUniform;
//$ import net.minecraft.util.ResourceLocation;
//#else
import net.minecraft.resources.ResourceLocation;
//#endif
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.ui.v1.screen.BlurScreen;
//#if MC <= 1.12.2
//$ import org.polyfrost.oneconfig.internal.mixin.Mixin_ShaderListAccessor;
//#endif

//#if MC <= 1.12.2
//$ import java.util.List;
//#endif

//#if MC >= 1.21.2
//$$ import net.minecraft.client.renderer.LevelTargetBundle;
//#endif

//#if FABRIC && MC < 1.21.2
//$$ import org.polyfrost.oneconfig.internal.mixin.fabric.Mixin_LoadShaderInvoker_Fabric;
//#endif

/**
 * An adapted and optimized implementation of the BlurMC mod by tterrag1098, later modified by boomboompower.
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
    private final ResourceLocation blurShader = OmniResourceLocation.createOrThrow("shaders/post/fade_in_blur.json");
    //#if MC <= 1.12.2
    //$ private ShaderUniform su;
    //#endif

    private BlurHandler() {
        EventHandler.ofRemoving(ScreenOpenEvent.class, e -> reloadBlur(e.getScreen())).register();
        //#if MC <= 1.21.5
        EventManager.register(TickEvent.End.class, () -> {
            //#if MC <= 1.12.2
            //$ if (su == null) return;
            //$ su.set(animation.update(50_000_000L)); // TODO: Find GlUniform.set alternative. Blur isnt cinfigured to work anyways with 1.21.5 anyways
            //#endif
        });
        //#endif
    }

    public static void init() {
        // will call <clinit>
    }

    public static boolean isBlurring() {
        //#if MC <= 1.12.2
        //$ return INSTANCE.su != null;
        //#else
        return false;
        //#endif
    }

    /**
     * Activates/deactivates the blur in the current world if
     * one of many conditions are met, such as no current other shader
     * is being used, we actually have the blur setting enabled
     */
    private boolean reloadBlur(Object gui) {
        //#if MC >= 1.21.1
        return false; // TODO: Fix shader usage in 1.21.1+
        //#else
        //$ // Don't do anything if no world is loaded
        //$ if (Minecraft.getMinecraft().theWorld == null) {
        //$     return false;
        //$ }
        //$ if (gui == null) {
        //$     tryStop();
        //$     return false;
        //$ }
        //$
        //$ // If a shader is not already active and the UI is
        //$ // a one of ours, we should load our own blur!
        //$ if (gui instanceof BlurScreen && ((BlurScreen) gui).hasBackgroundBlur()) {
        //$     if (!isShaderActive()) {
        //#if MC >= 1.21.4
        //$$         if (true) return false;
        //#endif
        //$
        //#if FABRIC
        //#if MC >= 1.16.5
        //$$         Mixin_LoadShaderInvoker_Fabric accessor = (Mixin_LoadShaderInvoker_Fabric) Minecraft.getInstance().gameRenderer;
        //#else
        //$$         Mixin_LoadShaderInvoker_Fabric accessor = (Mixin_LoadShaderInvoker_Fabric) Minecraft.getMinecraft().entityRenderer;
        //#endif
        //$$         accessor.invokeLoadShader(this.blurShader);
        //#else
        //#if MC >= 1.21.2
        //$$         Minecraft.getInstance().gameRenderer.setPostEffect(this.blurShader);
        //#else
        //$         Minecraft.getMinecraft().entityRenderer.loadShader(this.blurShader);
        //#endif
        //#endif
        //$
        //$         try {
        //$             ShaderGroup group = getShaderGroup();
        //$             if (group == null) return false;
        //$             List<Shader> shaders = ((Mixin_ShaderListAccessor) group).getListShaders();
        //$             if (shaders == null) return false;
        //$
        //$             // Iterate through the list of shaders.
        //$             for (Shader shader : shaders) {
        //$                 net.minecraft.client.shader.ShaderManager sm = shader.getShaderManager();
        //$                 ShaderUniform su = sm.getShaderUniform("Progress");
        //$                 if (su == null) continue;
        //$                 this.su = su;
        //$                 animation.reset();
        //$                 return false;
        //$             }
        //$             if (su == null) {
        //$                 LOGGER.error("Failed to get ShaderUniform for blur on GUI {}. It has been disabled. Please report this!", gui.getClass().getName());
        //$                 return true;
        //$             }
        //$         } catch (Exception ex) {
        //$             LOGGER.error("An error occurred while updating OneConfig's blur. It has been disabled. Please report this!", ex);
        //$             return true;
        //$         }
        //$     } else {
        //$         tryStop();
        //$     }
        //$ }
        //$
        //$ return false;
        //#endif
    }

    private void tryStop() {
        //#if MC >= 1.21.1
        // no-op: shader API not available in 1.21.1+
        //#else
        //$ ShaderGroup sg = getShaderGroup();
        //$ if (sg == null) return;
        //$ String name =
        //#if MC >= 1.21.2
        //$$         Minecraft.getInstance().gameRenderer.currentPostEffect().toString();
        //#else
        //$         sg.getShaderGroupName();
        //#endif
        //$
        //$ // Only stop our specific blur ;)
        //$ if (!name.endsWith("fade_in_blur.json")) {
        //$     return;
        //$ }
        //$
        //$ su = null;
        //#if MC >= 1.21.2
        //$$         Minecraft.getInstance().gameRenderer.clearPostEffect();
        //#else
        //$ Minecraft.getMinecraft().entityRenderer.stopUseShader();
        //#endif
        //#endif
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isShaderActive() {
        return getShaderGroup() != null
                //#if MC<=11202
                //$ && net.minecraft.client.renderer.OpenGlHelper.shadersSupported
                //#endif
                ;
    }

    private Object getShaderGroup() {
        //#if MC >= 1.21.1
        return null;
        //#else
        //$ return Minecraft.getMinecraft()
        //#if MC >= 1.21.2
        //$$ .getShaderManager().getPostChain(this.blurShader, LevelTargetBundle.MAIN_TARGETS);
        //#else
        //$         .entityRenderer.getShaderGroup();
        //#endif
        //#endif
    }
}
