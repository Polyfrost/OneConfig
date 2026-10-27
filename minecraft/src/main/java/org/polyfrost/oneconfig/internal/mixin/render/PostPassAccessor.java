package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.11 {
//? if >= 26.3 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//?} else
//import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostPass.class)
public interface PostPassAccessor {
    @Accessor("pipeline")
    RenderPipeline oneconfig$getPipeline();

    @Accessor("customUniforms")
    Map<String, GpuBuffer> oneconfig$getCustomUniforms();
}
//?}
