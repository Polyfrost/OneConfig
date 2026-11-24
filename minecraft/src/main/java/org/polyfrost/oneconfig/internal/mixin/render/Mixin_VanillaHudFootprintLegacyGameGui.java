package org.polyfrost.oneconfig.internal.mixin.render;

//? if = 1.8.9 {
/*import net.minecraft.client.gui.GameGui;
import org.objectweb.asm.Opcodes;
import org.polyfrost.oneconfig.internal.LegacyVanillaHudSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public abstract class Mixin_VanillaHudFootprintLegacyGameGui {
    @Inject(
        method = "render(F)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GameGui;overlayMessageCooldown:I", opcode = Opcodes.GETFIELD, ordinal = 0)
    )
    private void oneconfig$beginActionBarSample(float tickDelta, CallbackInfo ci) {
        LegacyVanillaHudSampler.beginInlineActionBar(this);
    }

    @Inject(
        method = "render(F)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GameGui;titleTime:I", opcode = Opcodes.GETFIELD, ordinal = 0)
    )
    private void oneconfig$beginTitleSample(float tickDelta, CallbackInfo ci) {
        LegacyVanillaHudSampler.beginInlineTitle(this);
    }

    @Inject(
        method = "render(F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getScoreboard()Lnet/minecraft/world/scores/Scoreboard;", ordinal = 0)
    )
    private void oneconfig$endInlineSample(float tickDelta, CallbackInfo ci) {
        LegacyVanillaHudSampler.endInline(this);
    }
}
*///?}
