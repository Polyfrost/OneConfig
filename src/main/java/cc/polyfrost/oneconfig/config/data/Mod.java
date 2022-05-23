package cc.polyfrost.oneconfig.config.data;

import cc.polyfrost.oneconfig.config.interfaces.Config;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class Mod {
    public final String name;
    public final ModType modType;
    public final String modIcon;
    public Config config;
    public final OptionPage defaultPage;
    public boolean isShortCut = false;

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     * @param modIcon path to icon of the mod (png or svg format)
     */
    public Mod(String name, ModType modType, @Nullable String modIcon) {
        this.name = name;
        this.modType = modType;
        this.modIcon = modIcon;
        this.defaultPage = new OptionPage(name, this);
    }

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     */
    public Mod(String name, ModType modType) {
        this(name, modType, null);
    }
}
