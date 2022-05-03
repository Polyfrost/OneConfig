package io.polyfrost.oneconfig.lwjgl;

import io.polyfrost.oneconfig.gui.OneConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * An implementation of the BlurMC mod by tterrag1098.
 *
 * For the original source see https://github.com/tterrag1098/Blur/blob/1.8.9/src/main/java/com/tterrag/blur/Blur.java
 * For the public license, see https://github.com/tterrag1098/Blur/blob/1.8.9/LICENSE
 *
 * License available under https://github.com/boomboompower/ToggleChat/blob/master/src/main/resources/licenses/BlurMC-License.txt
 *
 * @author tterrag1098, boomboompower
 *
 * Taken from ToggleChat
 * https://github.com/boomboompower/ToggleChat/blob/master/LICENSE
 */
public class BlurHandler {
    private final ResourceLocation blurShader = new ResourceLocation("shaders/post/fade_in_blur.json");
    private final Logger logger = LogManager.getLogger("OneConfig - Blur");
    private final Minecraft mc = Minecraft.getMinecraft();

    private long start;
    private float lastProgress = 0;

    public static BlurHandler INSTANCE = new BlurHandler();

    /**
     * Simply initializes the blur mod so events are properly handled by forge.
     */
    public void load() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGuiChange(GuiOpenEvent event) {
        reloadBlur(event.gui);
    }

    @SubscribeEvent
    public void onRenderTick(final TickEvent.RenderTickEvent event) {
        this.mc.mcProfiler.startSection("blur");

        if (event.phase != TickEvent.Phase.END) {
            this.mc.mcProfiler.endSection();
            return;
        }

        // Only blur on our own menus
        if (this.mc.currentScreen == null) {
            this.mc.mcProfiler.endSection();
            return;
        }

        // Only update the shader if one is active
        if (!this.mc.entityRenderer.isShaderActive()) {
            this.mc.mcProfiler.endSection();
            return;
        }

        float progress = getBlurStrengthProgress();

        // If the new progress value matches the old one this
        // will skip the frame update, which (hopefully) resolves the issue
        // with the heavy computations after the "animation" is complete.
        if (progress == this.lastProgress) {
            this.mc.mcProfiler.endSection();
            return;
        }

        // Store it for the next iteration!
        this.lastProgress = progress;

        // This is hilariously bad, and could cause frame issues on low-end computers.
        // Why is this being computed every tick? Surely there is a better way?
        // This needs to be optimized.
        try {
            final List<Shader> listShaders = this.mc.entityRenderer.getShaderGroup().listShaders;

            // Should not happen. Something bad happened.
            if (listShaders == null) {
                this.mc.mcProfiler.endSection();
                return;
            }

            // Iterate through the list of shaders.
            for (Shader shader : listShaders) {
                ShaderUniform su = shader.getShaderManager().getShaderUniform("Progress");

                if (su == null) {
                    continue;
                }

                // All this for this.
                su.set(progress);
            }
        } catch (IllegalArgumentException  ex) {
            this.logger.error("An error occurred while updating ToggleChat's blur. Please report this!", ex);
        }

        this.mc.mcProfiler.endSection();
    }

    /**
     * Activates/deactivates the blur in the current world if
     * one of many conditions are met, such as no current other shader
     * is being used, we actually have the blur setting enabled
     */
    public void reloadBlur(GuiScreen gui) {
        // Don't do anything if no world is loaded
        if (this.mc.theWorld == null) {
            return;
        }

        EntityRenderer er = this.mc.entityRenderer;

        // If a shader is not already active and the UI is
        // a one of ours, we should load our own blur!
        if (!er.isShaderActive() && gui instanceof OneConfigGui) {
            this.mc.entityRenderer.loadShader(this.blurShader);

            this.start = System.currentTimeMillis();

            // If a shader is active and the incoming UI is null or we have blur disabled, stop using the shader.
        } else if (er.isShaderActive() && (gui == null)) {
            String name = er.getShaderGroup().getShaderGroupName();

            // Only stop our specific blur ;)
            if (!name.endsWith("fade_in_blur.json")) {
                return;
            }

            er.stopUseShader();
        }
    }

    /**
     * Returns the strength of the blur as determined by the duration the effect of the blur.
     *
     * The strength of the blur does not go below 5.0F.
     */
    private float getBlurStrengthProgress() {
        return Math.min((System.currentTimeMillis() - this.start) / 50F, 5.0F);
    }
}
