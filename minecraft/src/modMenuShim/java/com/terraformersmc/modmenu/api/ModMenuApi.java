package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Consumer;

public interface ModMenuApi {
    static Screen createModsScreen(Screen previous) {
        try {
            Class<?> screenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            Constructor<?> constructor = screenClass.getConstructor(Screen.class);
            Object screen = constructor.newInstance(previous);
            if (screen instanceof Screen) {
                return (Screen) screen;
            }
        } catch (Throwable ignored) {
        }
        return previous;
    }

    static Component createModsButtonText() {
        return Component.literal("Mods");
    }

    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> null;
    }

    default UpdateChecker getUpdateChecker() {
        return null;
    }

    default Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return Map.of();
    }

    default Map<String, UpdateChecker> getProvidedUpdateCheckers() {
        return Map.of();
    }

    default void attachModpackBadges(Consumer<String> consumer) {
    }
}
