package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.platform.v1.Options;

public class OptionsImpl implements Options {
    @Override
    public float getGuiScale() {
        return Minecraft.getInstance().options.guiScale().get();
    }
}
