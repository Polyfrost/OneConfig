package org.polyfrost.oneconfig.internal;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class SkikoDataPathPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        SkikoDataPath.redirect();
    }
}
