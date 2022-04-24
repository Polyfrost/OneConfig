package io.polyfrost.oneconfig;

import io.polyfrost.oneconfig.command.OneConfigCommand;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.hud.HudCore;
import io.polyfrost.oneconfig.test.TestConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Mod(modid = "@ID@", name = "@NAME@", version = "@VER@")
public class OneConfig {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static File jarFile;
    public static File oneConfigDir = new File(mc.mcDataDir, "OneConfig/");
    public static File themesDir = new File(oneConfigDir, "themes/");
    public static OneConfigConfig config;
    public static TestConfig testConfig;
    public static List<ModData> loadedMods = new ArrayList<>();
    public static List<ModMetadata> loadedOtherMods = new ArrayList<>();

    @Mod.EventHandler
    public void onPreFMLInit(FMLPreInitializationEvent event) {
        jarFile = event.getSourceFile();
        oneConfigDir.mkdirs();
        themesDir.mkdirs();
        config = new OneConfigConfig();
    }

    @Mod.EventHandler
    public void onFMLInitialization(FMLInitializationEvent event) {
        testConfig = new TestConfig();
        ClientCommandHandler.instance.registerCommand(new OneConfigCommand());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HudCore());
    }

    @Mod.EventHandler
    public void onPostFMLInit(FMLPostInitializationEvent event) {
        loadedMods.addAll(ConfigCore.settings.keySet());
        LinkedHashSet<ModData> modData = new LinkedHashSet<>(ConfigCore.settings.keySet());
        for(ModContainer mod : Loader.instance().getActiveModList()) {
            ModMetadata metadata = mod.getMetadata();
            loadedOtherMods.add(metadata);
            String author = metadata.authorList.size() > 0 ? metadata.authorList.get(0) : "";
            ModData newMod = new ModData(metadata.name, ModType.OTHER, author, metadata.version);
            if(newMod.name.equals("OneConfig") || newMod.name.equals("Minecraft Coder Pack") || newMod.name.equals("Forge Mod Loader") || newMod.name.equals("Minecraft Forge")) {
                continue;
            }
            if(modData.add(newMod)) loadedMods.add(newMod);     // anti duplicate fix
        }
    }
}
