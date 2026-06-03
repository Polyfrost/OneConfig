package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.platform.v1.Options;

public class OptionsImpl implements Options {
    @Override
    public float getGuiScale() {
        // Effective scale (never 0/"Auto"), matches the value SkiaCtx.blitHud uses to map the
        // offscreen HUD texture back to screen. options.guiScale().get() returns the raw option
        // (0 = Auto, or a value != the computed scale) which mis-sized the HUD into the top-left.
        return (float) Minecraft.getInstance().getWindow().getGuiScale();
    }
}
