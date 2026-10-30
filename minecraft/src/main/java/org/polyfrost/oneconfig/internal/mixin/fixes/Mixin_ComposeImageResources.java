package org.polyfrost.oneconfig.internal.mixin.fixes;

import androidx.compose.ui.graphics.ImageBitmap;
import org.polyfrost.compose.backend.PolyImageBitmap;
import org.polyfrost.compose.render.PolyBitmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "androidx.compose.ui.res.ImageResources_desktopKt", remap = false)
public class Mixin_ComposeImageResources {
    /**
     * @author Polyfrost
     * @reason The original body calls Skia, which is not shipped. An inject would leave that bytecode for the verifier to resolve
     */
    @Overwrite
    public static ImageBitmap loadImageBitmap(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        PolyBitmap bitmap = PolyBitmap.decode(bytes);
        if (bitmap == null) throw new IOException("cannot decode image (" + bytes.length + " bytes)");
        return new PolyImageBitmap(bitmap);
    }
}
