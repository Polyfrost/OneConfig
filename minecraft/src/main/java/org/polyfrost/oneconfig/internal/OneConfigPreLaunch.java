package org.polyfrost.oneconfig.internal;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class OneConfigPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        org.polyfrost.oneconfig.internal.ui.compose.McFontService.installEngines();
    }
}
