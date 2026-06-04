package org.polyfrost.oneconfig.internal.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class PlayerHeadTextureAccess {
    private PlayerHeadTextureAccess() {
    }

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
