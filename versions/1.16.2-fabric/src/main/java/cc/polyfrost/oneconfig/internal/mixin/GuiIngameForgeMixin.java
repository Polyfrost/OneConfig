package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class GuiIngameForgeMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderGameOverlay(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        EventManager.INSTANCE.post(new HudRenderEvent(new UMatrixStack(matrices), tickDelta));
    }
}