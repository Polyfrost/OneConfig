package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.Platform;
import net.minecraft.client.Minecraft;

@SuppressWarnings("unused")
public class PlatformImpl implements Platform {
    @Override
    public boolean isCallingFromMinecraftThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }
}
