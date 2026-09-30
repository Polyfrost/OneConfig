package org.polyfrost.oneconfig.internal.mixin.keybind;

//? if <1.21.10 {
/*import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingCategoryAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> oneconfig$categorySortOrder() {
        throw new AssertionError();
    }
}
*///?}
