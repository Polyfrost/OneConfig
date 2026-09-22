package org.polyfrost.oneconfig.internal.mixin.fixes;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "dev.terminalmc.effecttimerplus.config.Config", remap = false)
public class Mixin_EffectTimerPlusLocation {

    @Inject(method = "validate", at = @At("HEAD"), require = 0)
    private void oneconfig$resetInvalidLocations(CallbackInfo ci) {
        oneconfig$reset("potencyLocation", "defaultPotencyLocation");
        oneconfig$reset("timerLocation", "defaultTimerLocation");
    }

    private void oneconfig$reset(String fieldName, String defaultFieldName) {
        try {
            Class<?> cls = getClass();
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            int value = field.getInt(this);
            if (value >= 0 && value <= 7) return;

            Field defaultField = cls.getDeclaredField(defaultFieldName);
            defaultField.setAccessible(true);
            int defaultValue = defaultField.getInt(null);
            field.setInt(this, defaultValue);
            LogManager.getLogger("OneConfig/Compat").warn(
                    "Reset out of range EffectTimerPlus option {} ({}) to its default ({})",
                    fieldName, value, defaultValue
            );
        } catch (Throwable ignored) {
        }
    }
}
