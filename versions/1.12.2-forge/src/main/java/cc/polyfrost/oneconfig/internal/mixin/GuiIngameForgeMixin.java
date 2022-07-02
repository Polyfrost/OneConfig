package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class, remap = false)
public class GuiIngameForgeMixin {
    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;post(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onRenderGameOverlay(float partialTicks, CallbackInfo ci) {
        EventManager.INSTANCE.post(new HudRenderEvent(new UMatrixStack(), partialTicks));
    }
}