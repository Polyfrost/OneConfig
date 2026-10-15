package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;

public interface HttpTextureCompat {
    default NativeImage getPixels() {
        HttpTexture texture = (HttpTexture) (Object) this;
        BufferedImage image = texture.image;
        if (image != null) {
            return new NativeImage(image);
        }
        if (!texture.uploaded) {
            return null;
        }
        // Sarcio drops the image once it is uploaded, so read back from GPU
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.bindTexture(texture.getGlId());
        try {
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (width <= 0 || height <= 0) {
                return null;
            }
            NativeImage pixels = new NativeImage(width, height, false);
            pixels.downloadTexture(0, false);
            return pixels;
        } finally {
            GlStateManager.bindTexture(previous);
        }
    }
}
*///?}
