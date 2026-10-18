package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

//? if >= 1.21.1 && < 1.21.10 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class Mixin_Toggle_DetailArmorBarVanilla {

    // Detail Armor Bar before 5.2 hides the vanilla bar by reporting no armor from its own mixin
    // in here which leaves nothing of the mod to hook
    @ModifyExpressionValue(
        method = "renderArmor",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getArmorValue()I"),
        require = 0
    )
    private static int oneconfig$realArmor(int original, @Local(argsOnly = true) Player player) {
        return ModToggles.isEnabled("detailabreconst") ? original : player.getArmorValue();
    }
}
*///?}
