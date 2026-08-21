package org.polyfrost.oneconfig.internal.mixin.keybind;

//? if < 1.21.10 && > 1.8.9 {
/*import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingCategoryAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> oneconfig$categorySortOrder() {
        throw new AssertionError();
    }
}
*///?}
