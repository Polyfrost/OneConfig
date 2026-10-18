package org.polyfrost.oneconfig.internal.mixin;

//? if = 1.8.9 {
/*import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.resource.language.Locale;
import net.minecraft.client.resource.manager.ResourceManager;
import org.polyfrost.oneconfig.internal.l10n.CommunityTranslations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Locale.class)
public class Mixin_LegacyCommunityTranslations {
    @Unique
    private static final Pattern oneconfig$NUMERIC_FORMAT = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");

    @Shadow
    Map<String, String> translations;

    @Inject(
        method = "reload",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/language/Locale;checkUnicodeUsage()V")
    )
    private void oneconfig$applyCommunityTranslations(ResourceManager resourceManager, List<String> languages, CallbackInfo ci) {
        CommunityTranslations.apply(languages, resourceManager.getNamespaces(), (key, value) -> translations.put(key, oneconfig$NUMERIC_FORMAT.matcher(value).replaceAll("%$1s")));
    }
}
*///?}
