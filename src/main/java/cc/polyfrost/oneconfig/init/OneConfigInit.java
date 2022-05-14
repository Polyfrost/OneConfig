package cc.polyfrost.oneconfig.init;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Initializes the OneConfig mod.
 * <p>MUST BE CALLED VIA AN ITWEAKER / FMLLOADINGPLUGIN FOR 1.12 AND BELOW, OR A PRELAUNCH TWEAKER FOR 1.14+ FABRIC.</p>
 */
@SuppressWarnings("unused")
public class OneConfigInit {
    public static void initialize(String[] args) {
        if (!Launch.blackboard.containsKey("oneconfig.initialized")) {
            Launch.blackboard.put("oneconfig.initialized", true);
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.oneconfig.json");
        }
    }
}
