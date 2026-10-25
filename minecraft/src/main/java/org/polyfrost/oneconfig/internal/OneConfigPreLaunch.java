package org.polyfrost.oneconfig.internal;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.polyfrost.oneconfig.internal.ui.compose.McFontService;

public final class OneConfigPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        McFontService.installEngines();
    }
}
