package io.polyfrost.oneconfig.config.data;

import io.polyfrost.oneconfig.config.interfaces.Config;

/**
 * The Mod data Object.
 * Used for Configuration
 */
public class Mod {
    public final String name;
    public final ModType modType;
    public final String creator;
    public final String version;
    public Config config;
    public OptionPage defaultPage = new OptionPage("", this);

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     * @param creator Creator of the mod
     * @param version Version of the mod
     */
    public Mod(String name, ModType modType, String creator, String version) {
        this.name = name;
        this.modType = modType;
        this.creator = creator;
        this.version = version;
    }
}
