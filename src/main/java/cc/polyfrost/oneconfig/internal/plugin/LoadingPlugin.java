package cc.polyfrost.oneconfig.internal.plugin;

import cc.polyfrost.oneconfig.internal.init.OneConfigInit;
import cc.polyfrost.oneconfig.internal.plugin.asm.ClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

public class LoadingPlugin {

    /**
     * Taken from LWJGLTwoPointFive under The Unlicense
     * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
     */
    public LoadingPlugin() {
        try {
            Field f_exceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            f_exceptions.setAccessible(true);
            Set<String> exceptions = (Set<String>) f_exceptions.get(Launch.classLoader);
            exceptions.remove("org.lwjgl.");
            OneConfigInit.initialize(new String[]{});
            Launch.blackboard.put("oneconfig.init.initialized", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getASMTransformerClass() {
        Launch.blackboard.put("oneconfig.init.registered_transformer", true);
        return new String[]{ClassTransformer.class.getName()};
    }

    public String getModContainerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> data) {

    }

    public String getAccessTransformerClass() {
        return null;
    }
}