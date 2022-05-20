package cc.polyfrost.oneconfig.init;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Initializes the OneConfig mod.
 * <p><b>MUST BE CALLED VIA AN ITWEAKER / FMLLOADINGPLUGIN FOR 1.12 AND BELOW, OR A PRELAUNCH TWEAKER FOR 1.14+ FABRIC.</b></p>
 */
@SuppressWarnings("unused")
public class OneConfigInit {
    public static void initialize(String[] args) {
        Launch.blackboard.put("oneconfig.initialized", true);
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.oneconfig.json");
    }
}
