package cc.polyfrost.oneconfig.mixin;

import cc.polyfrost.oneconfig.api.events.EventManager;
import cc.polyfrost.oneconfig.api.events.event.HudRenderEvent;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class, remap = false)
public class GuiIngameForgeMixin {
    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;post(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)V", shift = At.Shift.AFTER, remap = false))
    private void onRenderGameOverlay(float partialTicks, CallbackInfo ci) {
        EventManager.INSTANCE.post(new HudRenderEvent(partialTicks));
    }
}
