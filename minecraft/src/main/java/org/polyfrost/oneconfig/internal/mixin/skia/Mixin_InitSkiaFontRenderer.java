package org.polyfrost.oneconfig.internal.mixin.skia;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaFontRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_InitSkiaFontRenderer {
    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;createReload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/server/packs/resources/ReloadInstance;"))
    private void impl$registerFontReloadListener(CallbackInfo ci) {
        this.resourceManager.registerReloadListener(SkiaFontRenderer.INSTANCE);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    void impl$__init__(CallbackInfo ci) {
        SkiaFontRenderer.INSTANCE.init();
    }
}
