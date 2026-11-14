package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.8 {
import net.minecraft.client.gui.navigation.ScreenRectangle;
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
//?} else {
/*import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.ScreenArea;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
*///?}
import org.polyfrost.oneconfig.internal.ui.hud.VanillaHudFootprint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class Mixin_VanillaHudFootprint {
    @Unique
    private static void oneconfig$measure(ScreenArea area) {
        if (area == null) return;
        ScreenRectangle bounds = area.bounds();
        if (bounds == null) return;
        VanillaHudFootprint.add(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    //? if >= 26.1 {
    @Inject(method = "addGuiElement", at = @At("HEAD"))
    //?} else
    //@Inject(method = "submitGuiElement", at = @At("HEAD"))
    private void oneconfig$element(GuiElementRenderState state, CallbackInfo ci) {
        oneconfig$measure(state);
    }

    //? if >= 26.1 {
    @Inject(method = "addBlitToCurrentLayer", at = @At("HEAD"))
    //?} else
    //@Inject(method = "submitBlitToCurrentLayer", at = @At("HEAD"))
    private void oneconfig$blit(BlitRenderState state, CallbackInfo ci) {
        oneconfig$measure(state);
    }

    //? if >= 26.1 {
    @Inject(method = "addText", at = @At("HEAD"))
    //?} else
    //@Inject(method = "submitText", at = @At("HEAD"))
    private void oneconfig$text(GuiTextRenderState state, CallbackInfo ci) {
        oneconfig$measure(state);
    }

    //? if >= 26.1 {
    @Inject(method = "addItem", at = @At("HEAD"))
    //?} else
    //@Inject(method = "submitItem", at = @At("HEAD"))
    private void oneconfig$item(GuiItemRenderState state, CallbackInfo ci) {
        oneconfig$measure(state);
    }

    //? if >= 26.1 {
    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"))
    //?} else
    //@Inject(method = "submitPicturesInPictureState", at = @At("HEAD"))
    private void oneconfig$picture(PictureInPictureRenderState state, CallbackInfo ci) {
        oneconfig$measure(state);
    }
}
//?}
