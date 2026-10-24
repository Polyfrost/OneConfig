package org.polyfrost.oneconfig.internal.mixin.fixes;

//? if > 1.8.9 {
import com.google.gson.JsonParseException;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Options.class)
public class Mixin_SkipMalformedOptions {
    @WrapOperation(
        method = "processDumpedOptions",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options$OptionAccess;process(Ljava/lang/String;Lnet/minecraft/client/OptionInstance;)V")
    )
    private <T> void oneconfig$skipMalformedDumped(Options.OptionAccess access, String name, OptionInstance<T> option, Operation<Void> original) {
        oneconfig$processSafely(access, name, option, original);
    }

    @WrapOperation(
        method = "processOptions",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options$FieldAccess;process(Ljava/lang/String;Lnet/minecraft/client/OptionInstance;)V")
    )
    private <T> void oneconfig$skipMalformed(Options.FieldAccess access, String name, OptionInstance<T> option, Operation<Void> original) {
        oneconfig$processSafely(access, name, option, original);
    }

    @Unique
    private static void oneconfig$processSafely(Options.OptionAccess access, String name, OptionInstance<?> option, Operation<Void> original) {
        try {
            original.call(access, name, option);
        } catch (JsonParseException e) {
            LogManager.getLogger("OneConfig/Options").warn("Skipping malformed option '{}', keeping its current value: {}", name, e.getMessage());
        }
    }
}
//?}
