package org.polyfrost.oneconfig.internal.mixin.compat.yacl;
//#if MC != 1.20.4 || FABRIC
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.impl.YetAnotherConfigLibImpl;
import org.polyfrost.oneconfig.internal.compat.yacl.YaclV1Compat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(value = YetAnotherConfigLibImpl.BuilderImpl.class, remap = false)
public class Mixin_YetAnotherConfigLib_Builder {

    @Shadow private
    //#if FABRIC
    net.minecraft.text.Text
    //#else
    //$$ net.minecraft.network.chat.Component
    //#endif
    title;

    @Shadow @Final private List<ConfigCategory> categories;

    @Shadow private Runnable saveFunction;

    @Inject(method = "build", at = @At("TAIL"))
    public void build(CallbackInfoReturnable<YetAnotherConfigLib> cir) {
        YaclV1Compat.build(title, categories, saveFunction);
    }

}
//#endif