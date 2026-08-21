package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if > 1.8.9 {
import java.util.Map;
//?} else
//import java.util.List;

@Mixin(KeyMapping.class)
public interface KeyMappingRegistryAccessor {
    @SuppressWarnings("rawtypes")
    @Accessor("ALL")
    //~ if = 1.8.9 'Map' -> 'List'
    static Map oneconfig$all() {
        throw new AssertionError();
    }

    //? if > 1.8.9 {
    @SuppressWarnings("rawtypes")
    @Accessor("MAP")
    static Map oneconfig$map() {
        throw new AssertionError();
    }
    //?}
}
