//~ gui_graphics
package org.polyfrost.oneconfig.internal.mixin.skia;

//? if >= 1.21.11 {
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.polyfrost.oneconfig.internal.ui.compose.BlazeToasts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.gui.Gui;
//?} else {
/*import net.minecraft.client.renderer.GameRenderer;
*///?}

//? if >= 26.2 {
@Mixin(Gui.class)
//?} else {
/*@Mixin(GameRenderer.class)
*///?}
public class Mixin_BlazeToasts {
    //? if >= 26.2 {
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor"))
    //?} elif >= 26.1 {
    /*@Inject(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor"))
    *///?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    *///?}
    private void oneconfig$blazeToasts(CallbackInfo ci, @Local GuiGraphicsExtractor graphics) {
        BlazeToasts.render(graphics);
    }
}
//?} else {
/*public class Mixin_BlazeToasts {}
*///?}
