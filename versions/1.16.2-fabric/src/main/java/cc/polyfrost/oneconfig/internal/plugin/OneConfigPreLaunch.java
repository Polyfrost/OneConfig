package cc.polyfrost.oneconfig.internal.plugin;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import cc.polyfrost.oneconfig.internal.OneConfig;

public class OneConfigPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        OneConfig.preLaunch();
    }
}
