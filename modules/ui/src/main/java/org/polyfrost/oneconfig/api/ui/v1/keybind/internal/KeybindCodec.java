package org.polyfrost.oneconfig.api.ui.v1.keybind.internal;

import org.jetbrains.annotations.Nullable;

public interface KeybindCodec {
    @Nullable String keyName(int code);

    @Nullable Integer keyCode(String name);

    @Nullable String mouseName(int button);

    @Nullable Integer mouseButton(String name);

    @Nullable String legacyKeyName(int glfwCode);

    @Nullable String legacyMouseName(int glfwButton);
}
