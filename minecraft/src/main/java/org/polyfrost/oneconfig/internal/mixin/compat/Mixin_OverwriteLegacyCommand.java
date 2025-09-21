package org.polyfrost.oneconfig.internal.mixin.compat;

//#if FORGE && MC < 1.13
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "cc.polyfrost.oneconfig.internal.OneConfig", remap = false)
public class Mixin_OverwriteLegacyCommand {

    @Dynamic
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lcc/polyfrost/oneconfig/utils/commands/CommandManager;registerCommand(Ljava/lang/Object;)V"), remap = false)
    private void oneconfig$registerCommand(@Coerce Object instance, Object command) {
        // Prevent the registration of legacy commands
        // This is a no-op to prevent the command from being registered
    }

}
//#endif
