package cc.polyfrost.oneconfig.internal.plugin;

import cc.polyfrost.oneconfig.internal.init.OneConfigInit;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class OneConfigPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        OneConfigInit.initialize(new String[]{});
    }
}
