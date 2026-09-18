package org.polyfrost.oneconfig.internal.mixin.blaze3d;

//? >= 26.1 {
import com.mojang.renderpearl.backend.opengl.DirectStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
//? >= 26.2 {
import com.mojang.renderpearl.backend.opengl.FrameBufferCache;
//? }

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlDevice")
public interface GlDeviceAccessor {
    @Invoker("directStateAccess")
    DirectStateAccess oneconfig$getDirectStateAccess();

    //? >= 26.2 {
    @Invoker("frameBufferCache")
    FrameBufferCache oneconfig$getFrameBufferCache();
    //? }
}
//? }
