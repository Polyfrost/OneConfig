package cc.polyfrost.oneconfig.internal.init;

import org.spongepowered.asm.mixin.Mixins;

import java.io.File;

@SuppressWarnings("unused")
public class OneConfigInit {

    public static void initialize(String[] args) {
        Mixins.addConfiguration("mixins.oneconfig.json");
        final File oneConfigDir = new File("./OneConfig");
        oneConfigDir.mkdirs();
        new File(oneConfigDir, "profiles").mkdirs();
    }
}
