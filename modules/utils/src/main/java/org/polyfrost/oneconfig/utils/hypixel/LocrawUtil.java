package org.polyfrost.oneconfig.utils.hypixel;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class LocrawUtil {
    // LocrawUtils.h -- version specific workaround
    // see versions/src/main/java/org/polyfrost/oneconfig/utils/hypixel/LocrawUtil.java for the real implementation
    private LocrawUtil() {}

    public static final LocrawUtil INSTANCE = new LocrawUtil();

    void initialize() {}

    public boolean isInGame() {
        return false;
    }

    @Nullable
    public LocrawInfo getLocrawInfo() {
        return null;
    }

    @Nullable
    public LocrawInfo getLastLocrawInfo() {
        return null;
    }


}
