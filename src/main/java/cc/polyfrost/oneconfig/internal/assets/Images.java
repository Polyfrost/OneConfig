package cc.polyfrost.oneconfig.internal.assets;

import cc.polyfrost.oneconfig.renderer.AssetLoader;
import cc.polyfrost.oneconfig.renderer.Image;

/**
 * An enum of images used in OneConfig.
 *
 * @see cc.polyfrost.oneconfig.renderer.RenderManager#drawImage(long, String, float, float, float, float, int)
 * @see AssetLoader
 */
public class Images {
    public static final Image HUE_GRADIENT = new Image("/assets/oneconfig/options/HueGradient.png");
    public static final Image COLOR_WHEEL = new Image("/assets/oneconfig/options/ColorWheel.png");
    public static final Image ALPHA_GRID = new Image("/assets/oneconfig/options/AlphaGrid.png");
}