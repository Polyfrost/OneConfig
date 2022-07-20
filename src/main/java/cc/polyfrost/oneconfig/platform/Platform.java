package cc.polyfrost.oneconfig.platform;

import java.util.ServiceLoader;

/**
 * Contains various platform-specific utilities for OneConfig.
 *
 * This is meant for internal usage, however other mods may use these (unless otherwise stated).
 */
public interface Platform {

    static Platform getInstance() {
        return PlatformHolder.INSTANCE.platform;
    }

    static MousePlatform getMousePlatform() {
        return PlatformHolder.INSTANCE.mousePlatform;
    }

    static LoaderPlatform getLoaderPlatform() {
        return PlatformHolder.INSTANCE.loaderPlatform;
    }

    static ServerPlatform getServerPlatform() {
        return PlatformHolder.INSTANCE.serverPlatform;
    }

    static GLPlatform getGLPlatform() {
        return PlatformHolder.INSTANCE.glPlatform;
    }

    static GuiPlatform getGuiPlatform() {
        return PlatformHolder.INSTANCE.guiPlatform;
    }

    static I18nPlatform getI18nPlatform() {
        return PlatformHolder.INSTANCE.i18nPlatform;
    }

    static NanoVGPlatform getNanoVGPlatform() {
        return PlatformHolder.INSTANCE.nvgPlatform;
    }

    boolean isCallingFromMinecraftThread();

    int getMinecraftVersion();

    Loader getLoader();

    class PlatformHolder {
        private PlatformHolder() {

        }
        static PlatformHolder INSTANCE = new PlatformHolder();
        Platform platform = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).iterator().next();
        MousePlatform mousePlatform = ServiceLoader.load(MousePlatform.class, MousePlatform.class.getClassLoader()).iterator().next();
        LoaderPlatform loaderPlatform = ServiceLoader.load(LoaderPlatform.class, LoaderPlatform.class.getClassLoader()).iterator().next();
        ServerPlatform serverPlatform = ServiceLoader.load(ServerPlatform.class, ServerPlatform.class.getClassLoader()).iterator().next();
        GLPlatform glPlatform = ServiceLoader.load(GLPlatform.class, GLPlatform.class.getClassLoader()).iterator().next();
        GuiPlatform guiPlatform = ServiceLoader.load(GuiPlatform.class, GuiPlatform.class.getClassLoader()).iterator().next();
        I18nPlatform i18nPlatform = ServiceLoader.load(I18nPlatform.class, I18nPlatform.class.getClassLoader()).iterator().next();

        NanoVGPlatform nvgPlatform = ServiceLoader.load(NanoVGPlatform.class, NanoVGPlatform.class.getClassLoader()).iterator().next();
    }

    enum Loader {
        FORGE,
        FABRIC
    }
}
