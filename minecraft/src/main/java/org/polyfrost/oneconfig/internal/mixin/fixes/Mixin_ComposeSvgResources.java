package org.polyfrost.oneconfig.internal.mixin.fixes;

import androidx.compose.ui.graphics.painter.Painter;
import androidx.compose.ui.unit.Density;
import org.polyfrost.oneconfig.internal.ui.components.OversampledSvgPainter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "androidx.compose.ui.res.DesktopSvgResources_desktopKt", remap = false)
public class Mixin_ComposeSvgResources {
    /**
     * @author Polyfrost
     * @reason Skia is not shipped
     */
    @Overwrite
    public static Painter loadSvgPainter(InputStream inputStream, Density density) throws IOException {
        return new OversampledSvgPainter(inputStream.readAllBytes(), 1f, null);
    }
}
