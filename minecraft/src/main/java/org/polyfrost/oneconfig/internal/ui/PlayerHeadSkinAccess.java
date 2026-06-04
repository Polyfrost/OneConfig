package org.polyfrost.oneconfig.internal.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
//~if >= 1.21.11 'ResourceLocation;' -> 'Identifier;'
import net.minecraft.resources.Identifier;

import java.util.concurrent.TimeUnit;

public final class PlayerHeadSkinAccess {
    private PlayerHeadSkinAccess() {
    }

    public static com.mojang.blaze3d.platform.NativeImage loadSkinImage(Minecraft mc) {
        //~if >= 1.21.11 'ResourceLocation' -> 'Identifier'
        Identifier textureId = resolveBodyTextureId(mc);
        if (textureId == null) {
            return null;
        }
        return PlayerHeadTextureAccess.readPixels(mc.getTextureManager().getTexture(textureId));
    }

    //~if >= 1.21.11 'ResourceLocation' -> 'Identifier'
    private static Identifier resolveBodyTextureId(Minecraft mc) {
        GameProfile profile = mc.getGameProfile();

        var skin = mc.player != null ? mc.player.getSkin() : null;

        if (skin == null) {
            try {
                skin = mc
                        .getSkinManager()
                         //~if >= 1.21.10 '.getOrLoad(profile)' -> '.get(profile)'
                        .get(profile)
                        .get(2, TimeUnit.SECONDS)
                        //?if >= 1.21.4 {
                         .orElse(null);
                        //?} else {
                        /*;
                        *///?}
            } catch (Exception ignored) {
                return null;
            }
        }

        if (skin == null) {
            skin = net.minecraft.client.resources.DefaultPlayerSkin.get(profile);
        }

        //~if >= 1.21.10 'texture();' -> 'body().texturePath();'
        return skin.body().texturePath();
    }
}
