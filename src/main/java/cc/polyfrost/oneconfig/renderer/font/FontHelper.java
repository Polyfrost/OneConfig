package cc.polyfrost.oneconfig.renderer.font;

public interface FontHelper {
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
