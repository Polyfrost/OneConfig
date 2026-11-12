package org.polyfrost.oneconfig.internal.mixin.compat;

//? if >= 1.21.11 {
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState;
//?} else {
/*import net.minecraft.client.gui.render.state.GuiRenderState;
*///?}
import org.polyfrost.oneconfig.internal.compat.GnetumCompat;
import org.polyfrost.oneconfig.internal.compat.GnetumOffscreenState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiRenderState.class, priority = 900)
public abstract class Mixin_GnetumOffscreenState implements GnetumOffscreenState {
    @Unique private boolean oneconfig$offscreen;

    @Override
    public void oneconfig$markOffscreen() {
        oneconfig$offscreen = true;
    }

    //? if >= 26.1 {
    @Inject(method = {"addGuiElement", "addText", "addItem", "addPicturesInPictureState"}, at = @At("HEAD"))
    //?} else
    //@Inject(method = {"submitGuiElement", "submitText", "submitItem", "submitPicturesInPictureState"}, at = @At("HEAD"))
    private void oneconfig$bypassGnetum(CallbackInfo ci) {
        if (oneconfig$offscreen) GnetumCompat.suppress();
    }

    //? if >= 26.1 {
    @Inject(method = {"addGuiElement", "addText", "addItem", "addPicturesInPictureState"}, at = @At("RETURN"))
    //?} else
    //@Inject(method = {"submitGuiElement", "submitText", "submitItem", "submitPicturesInPictureState"}, at = @At("RETURN"))
    private void oneconfig$restoreGnetum(CallbackInfo ci) {
        if (oneconfig$offscreen) GnetumCompat.restore();
    }
}
//?}
