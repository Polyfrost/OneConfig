package io.polyfrost.oneconfig;

import io.polyfrost.oneconfig.command.OneConfigCommand;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.hud.HudCore;
import io.polyfrost.oneconfig.test.TestConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@net.minecraftforge.fml.common.Mod(modid = "@ID@", name = "@NAME@", version = "@VER@")
public class OneConfig {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static File jarFile;
    public static File oneConfigDir = new File(mc.mcDataDir, "OneConfig/");
    public static File themesDir = new File(oneConfigDir, "themes/");
    public static OneConfigConfig config;
    public static TestConfig testConfig;
    public static List<Mod> loadedMods = new ArrayList<>();
    public static List<ModMetadata> loadedOtherMods = new ArrayList<>();

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void onPreFMLInit(FMLPreInitializationEvent event) {
        jarFile = event.getSourceFile();
        oneConfigDir.mkdirs();
        themesDir.mkdirs();
        config = new OneConfigConfig();
    }

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void onFMLInitialization(FMLInitializationEvent event) {
        testConfig = new TestConfig();
        ClientCommandHandler.instance.registerCommand(new OneConfigCommand());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HudCore());
    }

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void onPostFMLInit(FMLPostInitializationEvent event) {
        reloadModsList();
    }

    public static void reloadModsList() {
        loadedMods.addAll(ConfigCore.oneConfigMods);
        LinkedHashSet<Mod> modData = new LinkedHashSet<>(ConfigCore.oneConfigMods);
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            ModMetadata metadata = mod.getMetadata();
            loadedOtherMods.add(metadata);
            String author = metadata.authorList.size() > 0 ? metadata.authorList.get(0) : "";
            Mod newMod = new Mod(metadata.name, ModType.OTHER, author, metadata.version);
            if (newMod.name.equals("Minecraft Coder Pack") || newMod.name.equals("Forge Mod Loader") || newMod.name.equals("Minecraft Forge")) {     // TODO add oneconfig
                continue;
            }
            if (modData.add(newMod)) loadedMods.add(newMod);     // anti duplicate fix
        }
    }
}
