package org.polyfrost.oneconfig.internal.mixin.compat;

//? dandelion_compat {
/*import java.util.List;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.ConfigManager;
import net.azureaaron.dandelion.impl.DandelionConfigScreenImpl;
import net.minecraft.network.chat.Component;
import org.polyfrost.oneconfig.internal.compat.DandelionCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DandelionConfigScreenImpl.class)
public class DandelionScreenImplMixin {
    @Shadow
    @Final
    private List<ConfigCategory> categories;

    @Shadow
    @Final
    private Component title;

    @Shadow
    @Final
    private ConfigManager<?> manager;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        DandelionCompat.initialize(this.title, this.categories, this.manager::save);
    }

}
*///? }