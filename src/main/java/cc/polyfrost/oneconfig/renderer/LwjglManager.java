package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.renderer.asset.AssetHelper;
import cc.polyfrost.oneconfig.renderer.font.FontHelper;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;

import java.util.ServiceLoader;

/**
 * Abstraction over the LWJGL3 implementation & loading.
 *
 * @deprecated Use the direct interface instances instead.
 */
@SuppressWarnings("DeprecatedIsStillUsed"/*, reason = "Methods are still used internally in their respective interfaces" */)
@Deprecated
public interface LwjglManager {
    LwjglManager INSTANCE = ServiceLoader.load(
            LwjglManager.class,
            LwjglManager.class.getClassLoader()
    ).iterator().next();

    /**
     * @return the {@link NanoVGHelper} platform implementation.
     * @deprecated Use {@link NanoVGHelper#INSTANCE} instead.
     */
    @Deprecated
    NanoVGHelper getNanoVGHelper();

    /**
     * @return the {@link ScissorHelper} platform implementation.
     * @deprecated Use {@link ScissorHelper#INSTANCE} instead.
     */
    @Deprecated
    ScissorHelper getScissorHelper();

    /**
     * @return the {@link AssetHelper} platform implementation.
     * @deprecated Use {@link AssetHelper#INSTANCE} instead.
     */
    @Deprecated
    AssetHelper getAssetHelper();

    @Deprecated
    FontHelper getFontHelper();

    TinyFD getTinyFD();
}
