package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingRegistryAccessor {
    @SuppressWarnings("rawtypes")
    @Accessor("ALL")
    static Map oneconfig$all() {
        throw new AssertionError();
    }

    @SuppressWarnings("rawtypes")
    @Accessor("MAP")
    static Map oneconfig$map() {
        throw new AssertionError();
    }
}
