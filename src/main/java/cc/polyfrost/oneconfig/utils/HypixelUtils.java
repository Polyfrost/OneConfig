package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.wrappers.UPlayer;
import cc.polyfrost.oneconfig.utils.hypixel.HypixelLowcraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.Locale;

public class HypixelUtils {
    private static final Minecraft mc = UMinecraft.getMinecraft();

    public HypixelLowcraw lowcraw;


    /**
     * Returns if the player is on Hypixel.
     * @return Boolean weather or not the player is on Hypixel.
     * @author jade#5000
     */
    public boolean isHypixel() {
        if (mc.theWorld == null || mc.thePlayer == null || mc.isSingleplayer()) return false;

        EntityPlayerSP player = UPlayer.getPlayer();
        String serverBrand = player.getServerBrand();

        if (serverBrand == null) return false;

        return serverBrand.toLowerCase(Locale.ENGLISH).contains("hypixel");
    }
}
