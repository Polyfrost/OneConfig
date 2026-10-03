package org.polyfrost.oneconfig.internal.mixin;

//? if > 1.8.9 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Options.class)
public class Mixin_VersionedResourcePacks {
    @Unique
    private final Map<String, Object> oneconfig$legacyPacks = new HashMap<>();

    @WrapOperation(
        method = "processOptions",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options$FieldAccess;process(Ljava/lang/String;Ljava/lang/Object;Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/lang/Object;")
    )
    private Object oneconfig$carryLegacyPackLists(Options.FieldAccess access, String name, Object value, Function<String, Object> reader, Function<Object, String> writer, Operation<Object> original) {
        if (name.equals("resourcePacks") || name.equals("incompatibleResourcePacks")) {
            String key = "oneconfig_legacy_" + name;
            oneconfig$legacyPacks.put(key, original.call(access, key, oneconfig$legacyPacks.getOrDefault(key, "[]"), Function.identity(), Function.identity()));
        }
        return original.call(access, name, value, reader, writer);
    }
}
//?}
