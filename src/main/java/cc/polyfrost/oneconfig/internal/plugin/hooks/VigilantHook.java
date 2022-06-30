package cc.polyfrost.oneconfig.internal.plugin.hooks;

import cc.polyfrost.oneconfig.config.compatibility.vigilance.VigilanceConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import gg.essential.vigilance.Vigilant;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.io.File;

@SuppressWarnings("unused")
public class VigilantHook {
    public static VigilanceConfig returnNewConfig(Vigilant vigilant, File file) {
        if (vigilant != null && Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            String name = !vigilant.getGuiTitle().equals("Settings") ? vigilant.getGuiTitle() : Loader.instance().activeModContainer() == null ? "Unknown" : Loader.instance().activeModContainer().getName();
            if (name.equals("OneConfig")) name = "Essential";
            String finalName = name;
            // duplicate fix
            if (ConfigCore.mods.stream().anyMatch(mod -> mod.name.equals(finalName))) return null;
            return new VigilanceConfig(new Mod(name, ModType.THIRD_PARTY), file.getAbsolutePath(), vigilant);
        } else {
            return null;
        }
    }
}
