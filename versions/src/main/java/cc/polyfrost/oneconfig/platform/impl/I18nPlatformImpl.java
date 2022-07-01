package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.I18nPlatform;
import net.minecraft.client.resources.I18n;

@SuppressWarnings("unused")
public class I18nPlatformImpl implements I18nPlatform {

    @Override
    public String format(String key, Object... args) {
        return I18n.format(key, args);
    }
}
