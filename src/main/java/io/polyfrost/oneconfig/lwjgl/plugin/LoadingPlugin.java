package io.polyfrost.oneconfig.lwjgl.plugin;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/
 */
public class LoadingPlugin implements IFMLLoadingPlugin {

    public LoadingPlugin() {
        try {
            Field f_exceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            f_exceptions.setAccessible(true);
            Set<String> exceptions = (Set<String>) f_exceptions.get(Launch.classLoader);
            exceptions.remove("org.lwjgl.");
        } catch (Exception e) {
            throw new RuntimeException("e");
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"io.polyfrost.oneconfig.lwjgl.plugin.ClassTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}