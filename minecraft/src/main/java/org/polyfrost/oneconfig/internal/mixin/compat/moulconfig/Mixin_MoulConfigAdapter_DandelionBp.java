package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

//? moul_compat {

/*import org.polyfrost.oneconfig.internal.compat.MoulConfigCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

@Pseudo
@Mixin(targets = "net.azureaaron.dandelion_bp.impl.moulconfig.MoulConfigAdapter", remap = false)
public class Mixin_MoulConfigAdapter_DandelionBp {
    @Inject(method = "generateMoulConfigScreen", at = @At("RETURN"), require = 0)
    private void oneconfig$onGenerateMoulConfigScreen(List<?> categories, @Coerce Object parent, String search, CallbackInfoReturnable<?> cir) {
        try {
            Method generateProcessedCategories = this.getClass().getDeclaredMethod("generateProcessedCategories", List.class);
            generateProcessedCategories.setAccessible(true);
            Object processed = generateProcessedCategories.invoke(this, categories);

            Field configDefinitionField = this.getClass().getDeclaredField("configDefinition");
            configDefinitionField.setAccessible(true);
            Object configDefinition = configDefinitionField.get(this);

            if (processed instanceof Collection<?> && configDefinition != null) {
                MoulConfigCompat.parseMoulconfigFromUnknownEditor((Collection<?>) processed, configDefinition);
            }
        } catch (Throwable ignored) {}
    }
}
*///? }
