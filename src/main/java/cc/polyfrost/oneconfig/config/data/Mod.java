package cc.polyfrost.oneconfig.config.data;

import cc.polyfrost.oneconfig.config.interfaces.Config;

import java.util.ArrayList;

public class Mod {
    public final String name;
    public final ModType modType;
    public final String creator;
    public final String version;
    public Config config;
    public final OptionPage defaultPage;
    public boolean isShortCut = false;
    private static final ArrayList<Mod> mods = new ArrayList<>();

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     * @param creator Creator of the mod
     * @param version Version of the mod
     */
    public Mod(String name, ModType modType, String creator, String version) {
        int i = 1;
        for (Mod mod : mods) {
            if (mod.name.startsWith(name)) {
                ++i;
                name = name + " " + i;
            }
        }
        this.name = name;
        this.modType = modType;
        this.creator = creator;
        this.version = version;
        this.defaultPage = new OptionPage(name, this);
        mods.add(this);
    }
}
