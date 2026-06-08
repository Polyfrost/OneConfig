package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

//? moul_compat {

/*import org.polyfrost.oneconfig.internal.compat.MoulConfigCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Collection;

@Pseudo
@Mixin(targets = "moe.nea.firmament.compat.moulconfig.MCConfigEditorIntegration", remap = false)
public class Mixin_MCConfigEditorIntegration_Firmament {
    @Inject(method = "<init>()V", at = @At("TAIL"), require = 0)
    private void oneconfig$onInit(CallbackInfo ci) {
        try {
            Field categoriesField = this.getClass().getDeclaredField("categories");
            Field configField = this.getClass().getDeclaredField("configObject");
            categoriesField.setAccessible(true);
            configField.setAccessible(true);

            Object categoriesValue = categoriesField.get(this);
            Object configValue = configField.get(this);

            if (categoriesValue instanceof Collection<?> && configValue != null) {
                MoulConfigCompat.parseMoulconfigFromUnknownEditor((Collection<?>) categoriesValue, configValue);
            }
        } catch (Throwable ignored) {}
    }
}
*///? }