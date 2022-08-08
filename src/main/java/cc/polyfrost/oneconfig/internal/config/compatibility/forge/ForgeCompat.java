package cc.polyfrost.oneconfig.internal.config.compatibility.forge;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;

public class ForgeCompat {
    public static final HashMap<Mod, Runnable> compatMods = new HashMap<>();

    public static class ForgeCompatMod extends Mod {

        public ForgeCompatMod(String name, ModType modType, @Nullable String modIcon, @Nullable Migrator migrator) {
            super(name, modType, modIcon, migrator);
            config = new Config(this, "") {
                @Override
                public void initialize() {

                }

                @Override
                public void save() {

                }

                @Override
                public void load() {

                }

                @Override
                public void openGui() {
                    compatMods.get(mod).run();
                }

                @Override
                public Object getDefault(Field field) {
                    return null;
                }

                @Override
                public void reset() {

                }
            };
        }

        public ForgeCompatMod(String name, ModType modType, @Nullable String modIcon) {
            this(name, modType, modIcon, null);
        }

        public ForgeCompatMod(String name, ModType modType, @Nullable Migrator migrator) {
            this(name, modType, null, migrator);
        }

        public ForgeCompatMod(String name, ModType modType) {
            this(name, modType, null, null);
        }
    }
}
