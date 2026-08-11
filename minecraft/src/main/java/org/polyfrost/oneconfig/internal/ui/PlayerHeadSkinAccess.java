package org.polyfrost.oneconfig.internal.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
//~if >= 1.21.11 'ResourceLocation;' -> 'Identifier;'
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Resolves the local player's head out of the skin texture the game already has loaded
 * <p>
 * The tasks returned here touch the texture manager and a live {@link NativeImage} so they must run
 * on the client thread while resolving the skin can block and must not
 * <p>
 * Before 1.21.4 skins are registered as {@code HttpTexture} with no readable pixel buffer so
 * extraction always yields null there and callers fall through to their own fallback
 */
public final class PlayerHeadSkinAccess {
    private PlayerHeadSkinAccess() {
    }

    /** Client-thread task reading the skin the local player is rendering with and never blocking */
    public static Supplier<NativeImage> localPlayerHeadTask(Minecraft mc) {
        return () -> {
            var player = mc.player;
            if (player == null) {
                return null;
            }
            //~if >= 1.21.10 'texture()' -> 'body().texturePath()'
            return extractHead(mc, player.getSkin().body().texturePath());
        };
    }

    /**
     * Awaits the profile's skin then returns a client-thread task producing a detached 32x32 head
     * <p>
     * Blocks for up to two seconds so this must not run on the client thread
     *
     * @return null when the skin could not be resolved
     */
    public static @Nullable Supplier<NativeImage> profileHeadTask(Minecraft mc) {
        GameProfile profile = mc.getGameProfile();

        try {
            var skin = mc
                    .getSkinManager()
                     //~if >= 1.21.10 '.getOrLoad(profile)' -> '.get(profile)'
                    .get(profile)
                    .get(2, TimeUnit.SECONDS)
                    //?if >= 1.21.4 {
                     .orElse(null);
                    //?} else {
                    /*;
                    *///?}
            if (skin == null) {
                return null;
            }
            //~if >= 1.21.10 'texture();' -> 'body().texturePath();'
            var textureId = skin.body().texturePath();
            return () -> extractHead(mc, textureId);
        } catch (Exception ignored) {
            return null;
        }
    }

    //~if >= 1.21.11 'ResourceLocation' -> 'Identifier'
    private static @Nullable NativeImage extractHead(Minecraft mc, Identifier textureId) {
        NativeImage pixels = PlayerHeadTextureAccess.readPixels(mc.getTextureManager().getTexture(textureId));
        if (pixels == null) {
            return null;
        }
        return PlayerHeadBlit.extractHead(pixels);
    }
}
