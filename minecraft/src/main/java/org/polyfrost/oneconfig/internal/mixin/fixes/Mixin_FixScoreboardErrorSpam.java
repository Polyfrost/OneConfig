package org.polyfrost.oneconfig.internal.mixin.fixes;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public class Mixin_FixScoreboardErrorSpam {

    @Inject(method = "removePlayerTeam", at = @At("HEAD"), cancellable = true)
    private void checkNPETeam(PlayerTeam team, CallbackInfo ci) {
        if (team == null) {
            ci.cancel();
        }
    }

    @Inject(method = "removeObjective", at = @At("HEAD"), cancellable = true)
    private void checkNPEObj(Objective objective, CallbackInfo ci) {
        if (objective == null) {
            ci.cancel();
        }
    }

}
