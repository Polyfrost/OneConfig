package cc.polyfrost.oneconfig.config.data;

import cc.polyfrost.oneconfig.config.interfaces.Config;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import org.jetbrains.annotations.Nullable;

public class Mod {
    public final String name;
    public final ModType modType;
    public final String modIcon;
    public final Migrator migrator;
    public final OptionPage defaultPage;
    public Config config;
    public boolean isShortCut = false;

    /**
     * @param name     Friendly name of the mod
     * @param modType  Type of the mod (for example ModType.QOL)
     * @param modIcon  Path to icon of the mod (png or svg format)
     * @param migrator Migrator class to port the old config
     */
    public Mod(String name, ModType modType, @Nullable String modIcon, @Nullable Migrator migrator) {
        this.name = name;
        this.modType = modType;
        this.modIcon = modIcon;
        this.migrator = migrator;
        this.defaultPage = new OptionPage(name, this);
    }

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     * @param modIcon path to icon of the mod (png or svg format)
     */
    public Mod(String name, ModType modType, @Nullable String modIcon) {
        this(name, modType, modIcon, null);
    }

    /**
     * @param name     Friendly name of the mod
     * @param modType  Type of the mod (for example ModType.QOL)
     * @param migrator Migrator class to port the old config
     */
    public Mod(String name, ModType modType, @Nullable Migrator migrator) {
        this(name, modType, null, migrator);
    }

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     */
    public Mod(String name, ModType modType) {
        this(name, modType, null, null);
    }
}
