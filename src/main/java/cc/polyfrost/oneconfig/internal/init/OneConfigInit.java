package cc.polyfrost.oneconfig.internal.init;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

@SuppressWarnings("unused")
public class OneConfigInit {

    public static void initialize(String[] args) {
        Mixins.addConfiguration("mixins.oneconfig.json");
    }
}
