package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.internal.hook.FramebufferHook;
import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

/**
 * Modified from MinecraftForge
 * <a href="https://github.com/MinecraftForge/MinecraftForge/blob/1.19.x/LICENSE.txt">...</a>
 */
@Mixin(Framebuffer.class)
public abstract class FramebufferMixin implements FramebufferHook {
    @Shadow public int textureWidth;
    @Shadow public int textureHeight;
    @Shadow private int depthAttachment;

    @Shadow public abstract void resize(int width, int height, boolean getError);

    @Shadow public int viewportWidth;
    @Shadow public int viewportHeight;
    private boolean stencilEnabled = false;

    @Redirect(method = "initFbo", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;texImage2D(IIIIIIIILjava/nio/IntBuffer;)V", ordinal = 0))
    private void stencilSupport(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        if (stencilEnabled) {
            GlStateManager.texImage2D(3553, 0, 36013, textureWidth, textureHeight, 0, 34041, 36269, null);
        } else {
            GlStateManager.texImage2D(3553, 0, 6402, textureWidth, textureHeight, 0, 6402, 5126, null);
        }
    }

    @Redirect(method = "initFbo", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;framebufferTexture2D(IIIII)V", ordinal = 1))
    private void stencilSupport(int target, int attachment, int textureTarget, int texture, int level) {
        if (stencilEnabled) {
            GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, 36096, 3553, depthAttachment, 0);
            GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, 36128, 3553, depthAttachment, 0);
        } else {
            GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.DEPTH_ATTACHMENT, 3553, depthAttachment, 0);
        }
    }

    @Override
    public boolean isStencilEnabled() {
        return stencilEnabled;
    }

    @Override
    public void enableStencil() {
        if (!this.stencilEnabled) {
            this.stencilEnabled = true;
            resize(viewportWidth, viewportHeight, MinecraftClient.IS_SYSTEM_MAC);
        }
    }
}
