package cc.polyfrost.oneconfig.internal.assets;

import cc.polyfrost.oneconfig.renderer.AssetLoader;
import cc.polyfrost.oneconfig.renderer.Image;

/**
 * An enum of images used in OneConfig.
 *
 * @see cc.polyfrost.oneconfig.renderer.RenderManager#drawImage(long, String, float, float, float, float, int)
 * @see AssetLoader
 */
public enum Images {
    HUE_GRADIENT("/assets/oneconfig/options/HueGradient.png"),
    COLOR_WHEEL("/assets/oneconfig/options/ColorWheel.png"),
    ALPHA_GRID("/assets/oneconfig/options/AlphaGrid.png");

    public final String filePath;
    public final Image image;

    Images(String filePath) {
        this.filePath = filePath;
        this.image = new Image(filePath);
    }
}