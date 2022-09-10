package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.utils.LogScanner;
import net.minecraft.crash.CrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CrashReport.class)
public class CrashReportMixin {
    @Shadow
    @Final
    private Throwable cause;

    // I have checked that this does not need preprocessing
    @ModifyVariable(method = "getCompleteReport", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private StringBuilder stringBuilder(StringBuilder stringBuilder) {
        if (Preferences.scanCrashes) {
            LogScanner.onCrash(stringBuilder, cause);
        }
        return stringBuilder;
    }
}
