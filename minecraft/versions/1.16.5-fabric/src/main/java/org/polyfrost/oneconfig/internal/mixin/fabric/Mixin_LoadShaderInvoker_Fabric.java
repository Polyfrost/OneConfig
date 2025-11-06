package org.polyfrost.oneconfig.internal.mixin.fabric;

//#if FABRIC && MC < 1.21.2
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface Mixin_LoadShaderInvoker_Fabric {
    @Invoker("loadEffect") void invokeLoadShader(ResourceLocation location);
}
//#endif
