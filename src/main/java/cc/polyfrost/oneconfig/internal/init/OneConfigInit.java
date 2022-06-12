package cc.polyfrost.oneconfig.internal.init;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@SuppressWarnings("unused")
public class OneConfigInit {

    public static void initialize(String[] args) {
        Launch.blackboard.put("oneconfig.initialized", true);
        MixinBootstrap.init();
        if (Launch.blackboard.containsKey("oneconfig.wrapper.modFile")) {
            try {
                File modFile = (File) Launch.blackboard.get("oneconfig.wrapper.modFile");
                try (JarFile jarFile = new JarFile(modFile)) {
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null) {
                        String mixinConfigs = manifest.getMainAttributes().getValue("MixinConfigs");
                        if (mixinConfigs != null) {
                            Mixins.addConfiguration(mixinConfigs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Mixins.addConfiguration("mixins.oneconfig.json");
    }
}
