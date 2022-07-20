package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.Platform;
import net.minecraft.client.Minecraft;

public class PlatformImpl implements Platform {
    @Override
    public boolean isCallingFromMinecraftThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    @Override
    public int getMinecraftVersion() {
        //#if MC>=11900
        //$$ return 11900;
        //#elseif MC>=11800
        //$$ return 11800;
        //#elseif MC>=11700
        //$$ return 11700;
        //#elseif MC>=11600
        //$$ return 11600;
        //#elseif MC>=11500
        //$$ return 11500;
        //#elseif MC>=11400
        //$$ return 11400;
        //#elseif MC>=11300
        //$$ return 11300;
        //#elseif MC>=11200
        //$$ return 11200;
        //#elseif MC>=11100
        //$$ return 11100;
        //#elseif MC>=11000
        //$$ return 11000;
        //#elseif MC>=10900
        //$$ return 10900;
        //#else
        return 10800;
        //#endif
    }

    @Override
    public Loader getLoader() {
        //#if FORGE==1
        return Loader.FORGE;
        //#else
        //$$ return Loader.FABRIC;
        //#endif
    }
}
