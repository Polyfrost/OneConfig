package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.WorldLoadEvent;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public class WorldClientMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onWorldLoad(ClientPlayNetHandler arg, ClientWorld.ClientWorldInfo arg2, RegistryKey<World> arg3, DimensionType arg4, int i, Supplier<IProfiler> supplier, WorldRenderer arg5, boolean bl, long l, CallbackInfo ci) {
        EventManager.INSTANCE.post(new WorldLoadEvent());
    }
}