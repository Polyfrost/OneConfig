package cc.polyfrost.oneconfig.internal.plugin.asm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Lwjgl3Loader {
    private static File tempJar = null;

    public static void load() {
        //#if FORGE==1 && MC<=11202
        if (net.minecraft.launchwrapper.Launch.blackboard.containsKey("oneconfig.lwjgl")) return; // dont add stuff twice in case of essential relaunch
        net.minecraft.launchwrapper.Launch.blackboard.put("oneconfig.lwjgl", true);
        ClassLoader root = net.minecraft.launchwrapper.Launch.classLoader.getClass().getClassLoader();
        //#else
        //$$ ClassLoader root = Lwjgl3Loader.class.getClassLoader();
        //#endif
        Method addURL;
        try {
            addURL = root.getClass().getMethod("addURL", URL.class);
        } catch (Exception e) {
            try {
                addURL = root.getClass().getDeclaredMethod("addUrlFwd", URL.class);
            } catch (Exception ex) {
                ex.printStackTrace();
                try {
                    //#if FORGE==1 && MC<=11202
                    net.minecraft.launchwrapper.Launch.classLoader.addURL(getTempJar().toURI().toURL());
                    //#endif
                } catch (Exception exception) {
                    e.printStackTrace();
                }
                return;
            }
        }
        addURL.setAccessible(true);
        try {
            addURL.invoke(root, getTempJar().toURI().toURL());
            //#if FORGE==1 && MC<=11202
            net.minecraft.launchwrapper.Launch.classLoader.addURL(getTempJar().toURI().toURL());
            //#endif
        } catch (IllegalAccessException | InvocationTargetException | IOException e) {
            e.printStackTrace();
        }
    }

    private static File getTempJar() throws IOException {
        if (tempJar == null) {
            tempJar = new File("./OneConfig/temp/oneconfig-lwjgl3.jar");
            tempJar.mkdirs();
            tempJar.createNewFile();
            tempJar.deleteOnExit();
            try (InputStream in = Lwjgl3Loader.class.getResourceAsStream("/lwjgl.jar")) {
                assert in != null;
                Files.copy(in, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return tempJar;
    }
}
