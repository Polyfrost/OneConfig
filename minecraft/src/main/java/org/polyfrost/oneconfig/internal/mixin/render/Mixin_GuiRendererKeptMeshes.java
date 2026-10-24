package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.11 {
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.gui.render.GuiRenderer;
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
//?} else
//import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.polyfrost.oneconfig.internal.ui.compose.KeptMeshes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public class Mixin_GuiRendererKeptMeshes {
    @Shadow @Final private List<?> draws;

    //? if >= 26.2 {
    @Inject(method = "addElementToMesh", at = @At("TAIL"))
    private void oneconfig$noteKept(GuiElementRenderState element, CallbackInfo ci) {
        KeptMeshes.added(draws.get(draws.size() - 1), element);
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
    }

    @Inject(method = "recordDraws", at = @At("TAIL"))
    private void oneconfig$bindKept(CallbackInfo ci) {
        KeptMeshes.recorded(draws);
    }

    @Inject(method = "executeDraw", at = @At("HEAD"), cancellable = true)
    private void oneconfig$drawKept(@Coerce Object draw, RenderPass pass, @Coerce Object indexBuffer, @Coerce Object indexType, CallbackInfo ci) {
        if (KeptMeshes.execute(draw, pass)) ci.cancel();
    }
    *///?}

    @Inject(method = "render", at = @At("TAIL"))
    private void oneconfig$endFrame(CallbackInfo ci) {
        KeptMeshes.endFrame();
    }
}
//?}
