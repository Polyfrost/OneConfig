package org.polyfrost.oneconfig.internal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Options.class)
public class Mixin_VersionedResourcePacks {
    @WrapOperation(
        method = "processOptions",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options$FieldAccess;process(Ljava/lang/String;Ljava/lang/Object;Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/lang/Object;")
    )
    private Object oneconfig$versionPackLists(Options.FieldAccess access, String name, Object value, Function<String, Object> reader, Function<Object, String> writer, Operation<Object> original) {
        Object shared = original.call(access, name, value, reader, writer);
        if (!name.equals("resourcePacks") && !name.equals("incompatibleResourcePacks")) return shared;
        return original.call(access, oneconfig$versionedKey(name), shared, reader, writer);
    }

    @Unique
    private static String oneconfig$versionedKey(String option) {
        //? >= 1.21.8 {
        String version = SharedConstants.getCurrentVersion().name();
        //?} else
        //String version = SharedConstants.getCurrentVersion().getName();
        return "oneconfig_" + option + '_' + version.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
