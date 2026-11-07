package org.polyfrost.oneconfig.internal.ui;

import org.polyfrost.oneconfig.api.platform.v1.I18nPlatform;

/** No language is loaded in tests, so every text falls back to its English default */
public final class TestI18nPlatform implements I18nPlatform {
    @Override
    public Object translate(String key, String fallback, Object... args) {
        return fallback;
    }

    @Override
    public String getUnformattedText(Object component) {
        return String.valueOf(component);
    }
}
