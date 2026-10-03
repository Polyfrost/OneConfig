package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;
import java.util.function.Supplier;

public interface GameOptionsCompat {
    default float getSoundSourceVolume(SoundCategory source) {
        return ((Options) (Object) this).getSoundCategoryVolume(source);
    }

    default Supplier<Boolean> forceUnicodeFont() {
        return () -> ((Options) (Object) this).forceUnicodeFont;
    }

    default Supplier<Boolean> japaneseGlyphVariants() {
        return () -> Locale.getDefault().getLanguage().equalsIgnoreCase("ja");
    }
}
*///?}
