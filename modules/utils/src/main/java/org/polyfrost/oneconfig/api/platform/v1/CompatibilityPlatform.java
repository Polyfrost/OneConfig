package org.polyfrost.oneconfig.api.platform.v1;

import dev.deftu.omnicore.api.loader.ModInfo;

import java.util.Set;

public interface CompatibilityPlatform {
    Set<ModInfo> getValidTrees();

    void executeTreeAction(String action);
}

