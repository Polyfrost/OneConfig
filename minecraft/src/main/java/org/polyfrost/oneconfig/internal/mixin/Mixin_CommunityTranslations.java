package org.polyfrost.oneconfig.internal.mixin;

//? if > 1.8.9 {
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.polyfrost.oneconfig.internal.l10n.CommunityTranslations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class Mixin_CommunityTranslations {
    @Inject(
        method = "loadFrom",
        //? if >= 1.21.4 {
        at = @At(value = "INVOKE", target = "Ljava/util/Map;copyOf(Ljava/util/Map;)Ljava/util/Map;")
        //?} else
        //at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;")
    )
    private static void oneconfig$applyCommunityTranslations(ResourceManager resourceManager, List<String> languages, boolean defaultRightToLeft, CallbackInfoReturnable<ClientLanguage> cir, @Local Map<String, String> translations) {
        CommunityTranslations.apply(languages, resourceManager.getNamespaces(), translations::put);
    }
}
//?}
