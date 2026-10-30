package org.polyfrost.oneconfig.internal.mixin.skia;

//? if > 1.8.9 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 1.21.8 {
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.gui.render.GuiRenderer;
//?}

//? if < 1.21.8 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

//~ if < 1.21.8 'GuiRenderer' -> 'GuiGraphics'
@Mixin(GuiRenderer.class)
public class Mixin_ItemAtlasScissor {
    //? if >= 1.21.8 {
    @Expression("16 * ?")
    @ModifyExpressionValue(method = "prepareItemElements", at = @At("MIXINEXTRAS:EXPRESSION"))
    private int oneconfig$itemAtlasSlotSize(int original) {
        int requested = GuiTargetRedirect.itemRenderSizePx;
        return requested > 0 ? requested : original;
    }
    //?}

    @WrapOperation(
        //? if >= 1.21.8 {
        method = "enableScissor",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;enableScissor(IIII)V")
        //?} else {
        /*method = "applyScissor",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableScissor(IIII)V")
        *///?}
    )
    //~ if < 1.21.8 '(RenderPass pass, int x' -> '(int x'
    private void oneconfig$targetScissor(RenderPass pass, int x, int y, int width, int height, Operation<Void> original, ScreenRectangle rectangle) {
        GuiTargetRedirect.ScissorTransform transform = GuiTargetRedirect.scissorTransform;
        if (transform == null) {
            //~ if < 1.21.8 '(pass, x' -> '(x'
            original.call(pass, x, y, width, height);
            return;
        }
        int[] mapped = transform.map(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
        RenderTarget target = GuiTargetRedirect.target;
        if (target != null) {
            int x0 = clamp(mapped[0], 0, target.width - 1);
            int y0 = clamp(mapped[1], 0, target.height - 1);
            int x1 = clamp(mapped[0] + mapped[2], x0 + 1, target.width);
            int y1 = clamp(mapped[1] + mapped[3], y0 + 1, target.height);
            mapped = new int[]{x0, y0, x1 - x0, y1 - y0};
        }
        //~ if < 1.21.8 '(pass, mapped[0]' -> '(mapped[0]'
        original.call(pass, mapped[0], mapped[1], mapped[2], mapped[3]);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
//?}
