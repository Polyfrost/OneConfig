package cc.polyfrost.oneconfig.lwjgl.image;

/**
 * An enum of images used in OneConfig.
 *
 * @see cc.polyfrost.oneconfig.lwjgl.RenderManager#drawImage(long, String, float, float, float, float, int)
 * @see ImageLoader
 */
public enum Images {
    HUE_GRADIENT("/assets/oneconfig/options/HueGradient.png"),
    COLOR_WHEEL("/assets/oneconfig/options/ColorWheel.png"),
    ALPHA_GRID("/assets/oneconfig/options/AlphaGrid.png");

    public final String filePath;

    Images(String filePath) {
        this.filePath = filePath;
    }
}