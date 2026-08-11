package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.platform.v1.Options;

public class OptionsImpl implements Options {
    @Override
    public float getGuiScale() {
        // effective scale never 0 or Auto and matches what SkiaCtx.blitHud uses
        // options.guiScale().get() returns the raw option which mis-sizes the HUD into the top-left
        return (float) Minecraft.getInstance().getWindow().getGuiScale();
    }
}
