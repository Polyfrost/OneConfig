package org.polyfrost.oneconfig.internal.mixin.keybind;

//? if >=1.21.10 {
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.Category.class)
public class Mixin_KeybindCategoryLabel {
    @Inject(method = "label", at = @At("RETURN"), cancellable = true)
    private void oneconfig$label(CallbackInfoReturnable<Component> cir) {
        String label = MinecraftKeybindBridgeImpl.categoryLabel(((KeyMapping.Category) (Object) this).id());
        if (label != null) cir.setReturnValue(Component.literal(label));
    }
}
//?}
