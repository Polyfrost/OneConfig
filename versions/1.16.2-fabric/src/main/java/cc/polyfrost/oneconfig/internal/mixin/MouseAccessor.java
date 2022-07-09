package cc.polyfrost.oneconfig.internal.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface MouseAccessor {
    @Accessor("eventDeltaWheel")
    double getEventDeltaWheel();

    @Accessor
    double getCursorDeltaX();

    @Accessor
    double getCursorDeltaY();
}
