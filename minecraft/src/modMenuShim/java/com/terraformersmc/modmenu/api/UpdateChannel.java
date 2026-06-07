package com.terraformersmc.modmenu.api;

public enum UpdateChannel {
    ALPHA,
    BETA,
    RELEASE;

    public static UpdateChannel getUserPreference() {
        try {
            Class<?> configClass = Class.forName("com.terraformersmc.modmenu.config.ModMenuConfig");
            Object option = configClass.getField("UPDATE_CHANNEL").get(null);
            Object value = option.getClass().getMethod("getValue").invoke(option);
            if (value instanceof UpdateChannel) {
                return (UpdateChannel) value;
            }
            if (value instanceof Enum<?>) {
                return valueOf(((Enum<?>) value).name());
            }
        } catch (Throwable ignored) {
        }
        return RELEASE;
    }
}
