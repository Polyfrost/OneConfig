package org.polyfrost.oneconfig.internal.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class PlayerHeadTextureAccess {
    private PlayerHeadTextureAccess() {
    }

    /**
     * @return the texture's live image, owned by the render thread and freed when the texture is next uploaded, so it
     * must be consumed on the client thread before the calling task returns
     */
    public static NativeImage readPixels(AbstractTexture texture) {
        if (texture == null) {
            return null;
        }
        if (texture instanceof net.minecraft.client.renderer.texture.DynamicTexture dynamicTexture) {
            return dynamicTexture.getPixels();
        }
        return null;
    }
}
