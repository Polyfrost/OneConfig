package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.11 {
import net.minecraft.client.gui.navigation.ScreenRectangle;
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
//?} else {
/*import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ScreenArea;
*///?}
import org.jspecify.annotations.Nullable;
import org.polyfrost.oneconfig.internal.ui.compose.StackingGuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiRenderState.class)
public abstract class Mixin_GuiRenderStateStacking implements StackingGuiRenderState {
    @Shadow private @Nullable ScreenRectangle lastElementBounds;

    @Shadow public abstract void up();

    @Unique private int oneconfig$stacked = -1;

    @Override
    public void oneconfig$setStacking(boolean stacking) {
        oneconfig$stacked = stacking ? 0 : -1;
    }

    @Inject(method = "findAppropriateNode", at = @At("HEAD"), cancellable = true)
    private void oneconfig$stack(ScreenArea area, CallbackInfoReturnable<Boolean> cir) {
        if (oneconfig$stacked < 0) return;
        ScreenRectangle bounds = area.bounds();
        if (bounds == null || oneconfig$stacked++ == 0) return;
        up();
        lastElementBounds = bounds;
        cir.setReturnValue(true);
    }
}
//?}
