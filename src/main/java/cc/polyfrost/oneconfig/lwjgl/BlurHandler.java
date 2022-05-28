package cc.polyfrost.oneconfig.lwjgl;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.ScreenOpenEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.mixin.ShaderGroupAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * An implementation of the BlurMC mod by tterrag1098.
 * <p>
 * For the original source see https://github.com/tterrag1098/Blur/blob/1.8.9/src/main/java/com/tterrag/blur/Blur.java
 * For the public license, see https://github.com/tterrag1098/Blur/blob/1.8.9/LICENSE
 * <p>
 * License available under https://github.com/boomboompower/ToggleChat/blob/master/src/main/resources/licenses/BlurMC-License.txt
 *
 * @author tterrag1098, boomboompower
 * <p>
 * Taken from ToggleChat
 * https://github.com/boomboompower/ToggleChat/blob/master/LICENSE
 */
public class BlurHandler {
    private final ResourceLocation blurShader = new ResourceLocation("shaders/post/fade_in_blur.json");
    private final Logger logger = LogManager.getLogger("OneConfig - Blur");

    private long start;
    private float lastProgress = 0;

    public static BlurHandler INSTANCE = new BlurHandler();

    /**
     * Simply initializes the blur mod so events are properly handled by forge.
     */
    public void load() {
        EventManager.INSTANCE.register(this);
    }

    @Subscribe
    private void onGuiChange(ScreenOpenEvent event) {
        reloadBlur(event.screen);
    }

    @Subscribe
    private void onRenderTick(RenderEvent event) {
        if (event.stage != Stage.END) {
            return;
        }

        // Only blur on our own menus
        if (UScreen.getCurrentScreen() == null) {
            return;
        }

        // Only update the shader if one is active
        if (!UMinecraft.getMinecraft().entityRenderer.isShaderActive()) {
            return;
        }

        float progress = getBlurStrengthProgress();

        // If the new progress value matches the old one this
        // will skip the frame update, which (hopefully) resolves the issue
        // with the heavy computations after the "animation" is complete.
        if (progress == this.lastProgress) {
            return;
        }

        // Store it for the next iteration!
        this.lastProgress = progress;

        // This is hilariously bad, and could cause frame issues on low-end computers.
        // Why is this being computed every tick? Surely there is a better way?
        // This needs to be optimized.
        try {
            final List<Shader> listShaders = ((ShaderGroupAccessor) Minecraft.getMinecraft().entityRenderer.getShaderGroup()).getListShaders();

            // Should not happen. Something bad happened.
            if (listShaders == null) {
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
        } catch (IllegalArgumentException ex) {
            this.logger.error("An error.png occurred while updating OneConfig's blur. Please report this!", ex);
        }
    }

    /**
     * Activates/deactivates the blur in the current world if
     * one of many conditions are met, such as no current other shader
     * is being used, we actually have the blur setting enabled
     */
    private void reloadBlur(GuiScreen gui) {
        // Don't do anything if no world is loaded
        if (UMinecraft.getWorld() == null) {
            return;
        }

        // If a shader is not already active and the UI is
        // a one of ours, we should load our own blur!
        if (!UMinecraft.getMinecraft().entityRenderer.isShaderActive() && gui instanceof OneConfigGui) {
            UMinecraft.getMinecraft().entityRenderer.loadShader(this.blurShader);

            this.start = System.currentTimeMillis();

            // If a shader is active and the incoming UI is null or we have blur disabled, stop using the shader.
        } else if (UMinecraft.getMinecraft().entityRenderer.isShaderActive() && (gui == null)) {
            String name = UMinecraft.getMinecraft().entityRenderer.getShaderGroup().getShaderGroupName();

            // Only stop our specific blur ;)
            if (!name.endsWith("fade_in_blur.json")) {
                return;
            }

            UMinecraft.getMinecraft().entityRenderer.stopUseShader();
        }
    }

    /**
     * Returns the strength of the blur as determined by the duration the effect of the blur.
     * <p>
     * The strength of the blur does not go below 5.0F.
     */
    private float getBlurStrengthProgress() {
        return Math.min((System.currentTimeMillis() - this.start) / 50F, 5.0F);
    }
}
