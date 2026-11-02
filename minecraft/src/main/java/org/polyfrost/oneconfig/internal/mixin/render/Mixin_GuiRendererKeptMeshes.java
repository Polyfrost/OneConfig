package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.11 {
import com.mojang.blaze3d.pipeline.RenderTarget;
//? if < 26.2 {
/*import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
*///?}
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.gui.render.GuiRenderer;
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
//?} else
//import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.polyfrost.oneconfig.internal.ui.compose.GlHost;
import org.polyfrost.oneconfig.internal.ui.compose.KeptMeshes;
import org.polyfrost.oneconfig.internal.ui.compose.LiquidGlass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public class Mixin_GuiRendererKeptMeshes {
    @Shadow @Final private List<?> draws;
    @Unique private boolean oneconfig$splitting;

    //? if >= 26.2 {
    @Inject(method = "addElementToMesh", at = @At("TAIL"))
    private void oneconfig$noteKept(GuiElementRenderState element, CallbackInfo ci) {
        KeptMeshes.added(draws.get(draws.size() - 1), element);
        LiquidGlass.added(draws.get(draws.size() - 1), element);
    }

    @Inject(method = "executeDraw", at = @At("HEAD"), cancellable = true)
    private void oneconfig$drawKept(@Coerce Object draw, RenderPass pass, CallbackInfo ci) {
        if (KeptMeshes.execute(draw, pass)) ci.cancel();
    }
    //?} else {
    /*@Shadow @Final private List<?> meshesToDraw;

    @Inject(method = "addElementToMesh", at = @At("TAIL"))
    private void oneconfig$noteKept(GuiElementRenderState element, CallbackInfo ci) {
        KeptMeshes.added(meshesToDraw.size(), element);
        LiquidGlass.added(meshesToDraw.size(), element);
    }

    @Inject(method = "recordDraws", at = @At("TAIL"))
    private void oneconfig$bindKept(CallbackInfo ci) {
        KeptMeshes.recorded(draws);
        LiquidGlass.recorded(draws);
    }

    @Inject(method = "executeDraw", at = @At("HEAD"), cancellable = true)
    private void oneconfig$drawKept(@Coerce Object draw, RenderPass pass, @Coerce Object indexBuffer, @Coerce Object indexType, CallbackInfo ci) {
        if (!KeptMeshes.execute(draw, pass)) return;
        // VulkanMod's GuiRenderer patch stops vanilla from binding its own index buffer, so ours would truncate later draws
        if (GlHost.isVulkanModInstalled()) pass.setIndexBuffer(null, null);
        ci.cancel();
    }
    *///?}

    //? if >= 26.2 {
    @Shadow
    private void executeDrawRange(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice dynamicTransforms, int startIndex, int endIndex) {
    }

    @Inject(method = "executeDrawRange", at = @At("HEAD"), cancellable = true)
    private void oneconfig$splitForGlass(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice dynamicTransforms, int startIndex, int endIndex, CallbackInfo ci) {
        if (oneconfig$splitting || !LiquidGlass.has(draws, startIndex, endIndex)) return;
        ci.cancel();
        oneconfig$splitting = true;
        try {
            LiquidGlass.split(draws, startIndex, endIndex, mainRenderTarget, (from, to) -> executeDrawRange(label, mainRenderTarget, dynamicTransforms, from, to));
        } finally {
            oneconfig$splitting = false;
        }
    }
    //?} else {
    /*@Shadow
    private void executeDrawRange(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransforms, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int startIndex, int endIndex) {
    }

    @Inject(method = "executeDrawRange", at = @At("HEAD"), cancellable = true)
    private void oneconfig$splitForGlass(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransforms, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int startIndex, int endIndex, CallbackInfo ci) {
        if (oneconfig$splitting || !LiquidGlass.has(draws, startIndex, endIndex)) return;
        ci.cancel();
        oneconfig$splitting = true;
        try {
            LiquidGlass.split(draws, startIndex, endIndex, mainRenderTarget, (from, to) -> executeDrawRange(label, mainRenderTarget, fogBuffer, dynamicTransforms, indexBuffer, indexType, from, to));
        } finally {
            oneconfig$splitting = false;
        }
    }
    *///?}

    @Inject(method = "render", at = @At("TAIL"))
    private void oneconfig$endFrame(CallbackInfo ci) {
        KeptMeshes.endFrame();
        LiquidGlass.endFrame();
    }
}
//?}
