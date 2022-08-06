package cc.polyfrost.oneconfig.internal.config.compatibility.forge;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ForgeCompat {
    public static final HashMap<Mod, Runnable> compatMods = new HashMap<>();

    public static class ForgeCompatMod extends Mod {

        public ForgeCompatMod(String name, ModType modType, @Nullable String modIcon, @Nullable Migrator migrator) {
            super(name, modType, modIcon, migrator);
        }

        public ForgeCompatMod(String name, ModType modType, @Nullable String modIcon) {
            super(name, modType, modIcon);
        }

        public ForgeCompatMod(String name, ModType modType, @Nullable Migrator migrator) {
            super(name, modType, migrator);
        }

        public ForgeCompatMod(String name, ModType modType) {
            super(name, modType);
        }
    }
}
