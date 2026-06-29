package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;

import java.awt.image.BufferedImage;

public interface HttpTextureCompat {
    default NativeImage getPixels() {
        BufferedImage image = ((HttpTexture) (Object) this).image;
        return image == null ? null : new NativeImage(image);
    }
}
*///?}
