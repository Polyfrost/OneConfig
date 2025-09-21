package org.polyfrost.oneconfig.internal;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.minecraft.util.Util;
import org.lwjgl.system.linux.DynamicLinkLoader;

import java.util.Map;

public final class RenderDoc {

    private static final int VERSION = 1_06_00; // 1.6.0
    private static boolean loaded = false;

    public static void init() {
        if (RenderDoc.loaded || !System.getProperty("renderdoc.enabled", "false").equalsIgnoreCase("true")) return;

        RenderDoc.loaded = true;

        var apiPointer = new PointerByReference();
        RenderdocLibrary.RenderdocApi apiInstance;

        var os = Util.getOperatingSystem();

        if (os == Util.OperatingSystem.WINDOWS || os == Util.OperatingSystem.LINUX) {
            try {
                RenderdocLibrary renderdocLibrary;
                if (os == Util.OperatingSystem.WINDOWS) {
                    var path = System.getProperty("renderdoc.path");
                    if (path == null || path.isEmpty()) {
                        System.out.println("RenderDoc path not set, please set the 'renderdoc.path' system property to the path of the RenderDoc DLL.");
                        throw new UnsatisfiedLinkError();
                    }
                    System.load(path);
                    renderdocLibrary = Native.load("renderdoc", RenderdocLibrary.class);
                } else {
                    int flags = DynamicLinkLoader.RTLD_NOW | DynamicLinkLoader.RTLD_NOLOAD;
                    if (DynamicLinkLoader.dlopen("librenderdoc.so", flags) == 0) {
                        System.out.println("RenderDoc library not found, please ensure it is available in your LD_PRELOAD environment variable.");
                        throw new UnsatisfiedLinkError();
                    }

                    renderdocLibrary = Native.load("renderdoc", RenderdocLibrary.class, Map.of(Library.OPTION_OPEN_FLAGS, flags));
                }

                int initResult = renderdocLibrary.RENDERDOC_GetAPI(VERSION, apiPointer);
                if (initResult != 1) {
                    System.out.println("Could not connect to RenderDoc API, return code: " + initResult);
                } else {
                    apiInstance = new RenderdocLibrary.RenderdocApi(apiPointer.getValue());

                    var major = new IntByReference();
                    var minor = new IntByReference();
                    var patch = new IntByReference();
                    apiInstance.GetAPIVersion.call(major, minor, patch);
                    System.out.println("Connected to RenderDoc API v" + major.getValue() + "." + minor.getValue() + "." + patch.getValue());
                }
            } catch (UnsatisfiedLinkError ignored) {}
        }
    }

}
