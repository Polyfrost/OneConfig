package cc.polyfrost.oneconfig;

import cc.polyfrost.oneconfig.command.OneConfigCommand;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.core.ConfigCore;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.hud.HudCore;
import cc.polyfrost.oneconfig.lwjgl.BlurHandler;
import cc.polyfrost.oneconfig.test.TestCommand;
import cc.polyfrost.oneconfig.test.TestConfig;
import cc.polyfrost.oneconfig.utils.GuiUtils;
import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The main class of OneConfig.
 */
@net.minecraftforge.fml.common.Mod(modid = "@ID@", name = "@NAME@", version = "@VER@")
public class OneConfig {
    public static final File oneConfigDir = new File("./OneConfig");
    public static final List<Mod> loadedMods = new ArrayList<>();
    public static final List<ModMetadata> loadedOtherMods = new ArrayList<>();
    public static OneConfigConfig config;
    public static TestConfig testConfig;
    private static boolean preLaunched = false;
    private static boolean initialized = false;

    /**
     * Called before mods are loaded.
     * <p><b>SHOULD NOT BE CALLED!</b></p>
     */
    public static void preLaunch() {
        if (preLaunched) return;
        if (!net.minecraft.launchwrapper.Launch.blackboard.containsKey("oneconfig.initialized")) {
            throw new RuntimeException("OneConfig has not been initialized! Please add the OneConfig tweaker or call OneConfigInit via an ITweaker or a FMLLoadingPlugin!");
        }
        oneConfigDir.mkdirs();
        new File(oneConfigDir, "profiles").mkdirs();
        config = new OneConfigConfig();
        preLaunched = true;
    }

    /**
     * Called after mods are loaded.
     * <p><b>SHOULD NOT BE CALLED!</b></p>
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init() {
        if (initialized) return;
        GuiUtils.getDeltaTime(); // called to make sure static initializer is called
        BlurHandler.INSTANCE.load();
        testConfig = new TestConfig();
        CommandManager.INSTANCE.registerCommand(OneConfigCommand.class);
        CommandManager.INSTANCE.registerCommand(TestCommand.class);
        EventManager.INSTANCE.register(new HudCore());
        EventManager.INSTANCE.register(HypixelUtils.INSTANCE);
        reloadModsList();
        initialized = true;
    }

    public static void reloadModsList() {
        loadedMods.addAll(ConfigCore.oneConfigMods);
        LinkedHashSet<Mod> modData = new LinkedHashSet<>(ConfigCore.oneConfigMods);
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            ModMetadata metadata = mod.getMetadata();
            loadedOtherMods.add(metadata);
            String imageName = Loader.instance().activeModContainer() == null || Loader.instance().activeModContainer().getMetadata().logoFile.trim().equals("") ? null : "/" + Loader.instance().activeModContainer().getMetadata().logoFile;
            Mod newMod = new Mod(metadata.name, ModType.THIRD_PARTY, imageName);
            newMod.isShortCut = true;
            if (mod instanceof DummyModContainer || newMod.name.equals("OneConfig")) continue;
            if (modData.add(newMod)) loadedMods.add(newMod);
        }
    }
}
