package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.renderer.font.FontHelper;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;

import java.util.ServiceLoader;

public interface LwjglManager {
    NanoVGHelper getNanoVGHelper();
    ScissorHelper getScissorHelper();
    AssetHelper getAssetHelper();
    FontHelper getFontHelper();

    TinyFD getTinyFD();

    LwjglManager INSTANCE = ServiceLoader.load(LwjglManager.class, LwjglManager.class.getClassLoader()).iterator().next();
}
