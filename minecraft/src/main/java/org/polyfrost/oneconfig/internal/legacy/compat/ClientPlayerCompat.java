package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import org.polyfrost.oneconfig.internal.legacy.PlayerSkin;

public interface ClientPlayerCompat {
    default PlayerSkin getSkin() {
        return new PlayerSkin(((ClientPlayerEntity) (Object) this).getSkinTextureLocation());
    }
}
*///?}
