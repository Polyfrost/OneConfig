package org.polyfrost.oneconfig.api.platform.v1;

import net.kyori.adventure.text.Component;

import java.util.Set;

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
}

