package org.polyfrost.oneconfig.api.platform.v1;

import net.kyori.adventure.text.Component;

import java.util.Set;
import net.kyori.adventure.text.TranslatableComponent;

public interface CompatibilityPlatform {
    void displayChatMessage(Component text);
    default void displayChatMessage(String text) {
        displayChatMessage(Component.text(text));
    }
    Set<ModInfo> getMods();
    boolean isDevelopment();
    String version();
    String loader();
    Options options();
    Keys keys();

    long windowHandle();

    int fps();

    String resolveComponent(Component component);
    Object wrapPlatformComponent(Object component);
}

