package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.platform.ServerPlatform;
import net.minecraft.client.entity.EntityPlayerSP;

public class ServerPlatformImpl implements ServerPlatform {

    @Override
    public boolean inMultiplayer() {
        return UMinecraft.getWorld() != null && !UMinecraft.getMinecraft().isSingleplayer();
    }

    @Override
    public String getServerBrand() {
        EntityPlayerSP player = UMinecraft.getPlayer();
        if (player == null)
            return null;
        return player.getClientBrand();
    }
}
