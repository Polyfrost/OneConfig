package cc.polyfrost.oneconfig.internal.init;

import org.spongepowered.asm.mixin.Mixins;

@SuppressWarnings("unused")
public class OneConfigInit {

    public static void initialize(String[] args) {
        Mixins.addConfiguration("mixins.oneconfig.json");
    }
}
