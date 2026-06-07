package com.terraformersmc.modmenu.api;

import org.jetbrains.annotations.Nullable;

public interface UpdateChecker {
    @Nullable
    UpdateInfo checkForUpdates();
}
