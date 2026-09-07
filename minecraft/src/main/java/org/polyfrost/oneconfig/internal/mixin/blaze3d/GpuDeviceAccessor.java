package org.polyfrost.oneconfig.internal.mixin.blaze3d;

//? >= 26.1 {
//? if >= 26.3 {
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
//?} else {
/*import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >= 26.3 {
@Mixin(targets = "com.mojang.renderpearl.frontend.FrontendGpuDevice")
//?} else
//@Mixin(GpuDevice.class)
public interface GpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend oneconfig$getBackend();
}
//? }
