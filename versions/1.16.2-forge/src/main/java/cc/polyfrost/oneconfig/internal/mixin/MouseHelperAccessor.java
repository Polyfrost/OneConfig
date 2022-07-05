package cc.polyfrost.oneconfig.internal.mixin;

import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHelper.class)
public interface MouseHelperAccessor {
    @Accessor
    double getAccumulatedScrollDelta();
}
