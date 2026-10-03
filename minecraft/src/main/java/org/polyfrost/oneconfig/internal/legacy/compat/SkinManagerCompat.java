package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.resource.skin.SkinManager;
import org.polyfrost.oneconfig.internal.legacy.PlayerSkin;

import java.util.concurrent.CompletableFuture;

public interface SkinManagerCompat {
    default CompletableFuture<PlayerSkin> getOrLoad(GameProfile profile) {
        SkinManager manager = (SkinManager) (Object) this;
        CompletableFuture<PlayerSkin> future = new CompletableFuture<>();
        manager.register(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                future.complete(new PlayerSkin(location));
            }
        }, true);
        return future;
    }
}
*///?}
