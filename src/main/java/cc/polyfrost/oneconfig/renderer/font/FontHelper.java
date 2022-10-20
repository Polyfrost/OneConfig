package cc.polyfrost.oneconfig.renderer.font;

import cc.polyfrost.oneconfig.renderer.LwjglManager;

public interface FontHelper {
    FontHelper INSTANCE = LwjglManager.INSTANCE.getFontHelper();

    /**
     * Load all fonts in the Fonts class
     *
     * @param vg NanoVG context
     */
    void initialize(long vg);

    /**
     * Load a font into NanoVG
     *
     * @param vg   NanoVG context
     * @param font The font to be loaded
     */
    void loadFont(long vg, Font font);
}
