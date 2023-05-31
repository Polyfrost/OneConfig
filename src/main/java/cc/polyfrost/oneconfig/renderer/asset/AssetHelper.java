/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.renderer.asset;

import cc.polyfrost.oneconfig.renderer.LwjglManager;

public interface AssetHelper {
    AssetHelper INSTANCE = LwjglManager.INSTANCE.getAssetHelper();

    int DEFAULT_FLAGS = 2 | 4 | 1; // NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS

    /**
     * Loads an assets from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param flags    The image flags
     * @param clazz    The class to use for loading the resource.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, String fileName, int flags, Class<?> clazz);

    /**
     * Loads an assets from resources.
     *
     * @param vg    The NanoVG context.
     * @param image The Image
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, Image image);

    /**
     * Loads an assets from resources.
     *
     * @param vg    The NanoVG context.
     * @param image The Image
     * @param clazz The class to use for loading the resource.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, Image image, Class<?> clazz);

    /**
     * Loads an assets from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param clazz    The class to load the resource from.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, String fileName, Class<?> clazz);

    /**
     * Loads an SVG from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param width    The width of the SVG.
     * @param height   The height of the SVG.
     * @param flags    The image flags
     * @param clazz    The class to use for loading the resource.
     * @return Whether the SVG was loaded successfully.
     */
    boolean loadSVG(long vg, String fileName, float width, float height, int flags, Class<?> clazz);

    /**
     * Loads an assets from resources.
     *
     * @param vg     The NanoVG context.
     * @param svg    The SVG
     * @param width  The width of the SVG.
     * @param height The height of the SVG.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadSVG(long vg, SVG svg, float width, float height);

    /**
     * Loads an assets from resources.
     *
     * @param vg     The NanoVG context.
     * @param svg    The SVG
     * @param width  The width of the SVG.
     * @param height The height of the SVG.
     * @param clazz  The class to use for loading the resource.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadSVG(long vg, SVG svg, float width, float height, Class<?> clazz);

    /**
     * Loads an SVG from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param width    The width of the SVG.
     * @param height   The height of the SVG.
     * @param clazz    The class to load the resource from.
     * @return Whether the SVG was loaded successfully.
     */
    boolean loadSVG(long vg, String fileName, float width, float height, Class<?> clazz);

    /**
     * Get a loaded assets from the cache.
     * <p><b>Requires the assets to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The assets
     * @see AssetHelper#loadImage(long, String, Class)
     */
    int getImage(String fileName);

    NVGAsset getNVGImage(String fileName);

    /**
     * Remove an assets from the cache, allowing the assets to be garbage collected.
     * Should be used when the GUI rendering the assets is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see AssetHelper#loadImage(long, String, Class)
     */
    void removeImage(long vg, String fileName);

    /**
     * Clears all images from the cache, allowing the images cleared to be garbage collected.
     * Should be used when the GUI rendering loaded images are closed.
     *
     * @param vg The NanoVG context.
     */
    void clearImages(long vg);

    /**
     * Get a loaded SVG from the cache.
     * <p><b>Requires the SVG to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The SVG
     * @see AssetHelper#loadSVG(long, String, float, float, Class)
     */
    int getSVG(String fileName, float width, float height);

    NVGAsset getNVGSVG(String fileName);

    /**
     * Remove an SVG from the cache, allowing the SVG to be garbage collected.
     * Should be used when the GUI rendering the SVG is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see AssetHelper#loadSVG(long, String, float, float, Class)
     */
    void removeSVG(long vg, String fileName, float width, float height);

    /**
     * Clears all SVGs from the cache, allowing the SVGs cleared to be garbage collected.
     * Should be used when the GUI rendering loaded SVGs are closed.
     *
     * @param vg The NanoVG context.
     */
    void clearSVGs(long vg);
}
