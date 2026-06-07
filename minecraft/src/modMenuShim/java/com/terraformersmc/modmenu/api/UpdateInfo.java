package com.terraformersmc.modmenu.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface UpdateInfo {
    boolean isUpdateAvailable();

    @Nullable
    default Component getUpdateMessage() {
        return null;
    }

    String getDownloadLink();

    UpdateChannel getUpdateChannel();
}
