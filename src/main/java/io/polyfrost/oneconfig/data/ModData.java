package io.polyfrost.oneconfig.data;

import io.polyfrost.oneconfig.interfaces.Config;

public class ModData {
    public final String name;
    public final ModType modType;
    public final String creator;
    public final String version;
    public Config config;

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     * @param creator Creator of the mod
     * @param version Version of the mod
     */
    public ModData(String name, ModType modType, String creator, String version) {
        this.name = name;
        this.modType = modType;
        this.creator = creator;
        this.version = version;
    }
}
