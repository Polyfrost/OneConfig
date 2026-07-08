package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 26.1 {
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("renderState")
    GuiRenderState oneconfig$getRenderState();

    @Mutable
    @Accessor("renderState")
    void oneconfig$setRenderState(GuiRenderState state);
}
//? }
