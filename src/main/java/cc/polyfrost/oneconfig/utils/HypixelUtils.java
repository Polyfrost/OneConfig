package cc.polyfrost.oneconfig.utils;

import net.minecraft.client.Minecraft;

import java.util.Locale;

public class HypixelUtils {
    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Returns if the player is on Hypixel.
     * @return Boolean weather or not the player is on Hypixel.
     * @author jade#5000
     */
    public boolean isHypixel() {
        if (mc.theWorld == null || mc.thePlayer == null ) return false;

        // String serverBrand = mc.thePlayer.getServerBrand();
        // if (serverBrand == null) return false;

        // return serverBrand.toLowerCase(Locale.ENGLISH).contains("hypixel");
        return true;
    }
}
