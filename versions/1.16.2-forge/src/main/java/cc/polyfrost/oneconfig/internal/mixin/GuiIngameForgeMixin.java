package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeIngameGui.class, remap = false)
public class GuiIngameForgeMixin {
    @Inject(method = "renderIngameGui", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/ForgeIngameGui;post(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;Lcom/mojang/blaze3d/matrix/MatrixStack;)V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onRenderGameOverlay(MatrixStack matrixStack, float partialTicks, CallbackInfo ci) {
        EventManager.INSTANCE.post(new HudRenderEvent(new UMatrixStack(), partialTicks));
    }
}