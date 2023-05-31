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

package cc.polyfrost.oneconfig.internal.renderer;

import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.asset.AssetHelper;
import cc.polyfrost.oneconfig.renderer.asset.Image;
import cc.polyfrost.oneconfig.renderer.asset.NVGAsset;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.utils.IOUtils;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads images and SVGs from resources into NanoVG.
 *
 * @see NanoVGHelperImpl
 * @see Images
 * @see SVGs
 */
public final class AssetHelperImpl implements AssetHelper {
    public static final int DEFAULT_FLAGS = NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS;
    private final Map<String, NVGAsset> imageHashMap = new HashMap<>();
    private final Map<String, NVGAsset> svgHashMap = new HashMap<>();

    /**
     * Loads an assets from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param flags    The image flags
     * @return Whether the asset was loaded successfully.
     */
    @Override
    public boolean loadImage(long vg, String fileName, int flags, Class<?> clazz) {
        if (!imageHashMap.containsKey(fileName)) {
            int[] width = {0};
            int[] height = {0};
            int[] channels = {0};

            ByteBuffer image = IOUtils.resourceToByteBufferNullable(fileName, clazz);
            if (image == null) {
                return false;
            }

            ByteBuffer buffer = STBImage.stbi_load_from_memory(image, width, height, channels, 4);
            if (buffer == null) {
                return false;
            }

            imageHashMap.put(fileName, new NVGAsset(NanoVG.nvgCreateImageRGBA(vg, width[0], height[0], flags, buffer), width[0], height[0]));
            return true;
        }
        return true;
    }

    /**
     * Loads an assets from resources.
     *
     * @param vg    The NanoVG context.
     * @param image The Image
     * @return Whether the asset was loaded successfully.
     */
    @Override
    public boolean loadImage(long vg, Image image) {
        return loadImage(vg, image.filePath, image.flags, image.getClass());
    }

    @Override
    public boolean loadImage(long vg, Image image, Class<?> clazz) {
        return loadImage(vg, image.filePath, image.flags, clazz);
    }

    /**
     * Loads an assets from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @return Whether the asset was loaded successfully.
     */
    @Override
    public boolean loadImage(long vg, String fileName, Class<?> clazz) {
        return loadImage(vg, fileName, DEFAULT_FLAGS, clazz);
    }

    /**
     * Loads an SVG from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param width    The width of the SVG.
     * @param height   The height of the SVG.
     * @param flags    The image flags
     * @return Whether the SVG was loaded successfully.
     */
    @Override
    public boolean loadSVG(long vg, String fileName, float width, float height, int flags, Class<?> clazz) {
        String name = fileName + "-" + width + "-" + height;
        if (!svgHashMap.containsKey(name)) {
            try {
                InputStream inputStream = this.getClass().getResourceAsStream(fileName);
                if (inputStream == null) return false;
                StringBuilder resultStringBuilder = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        resultStringBuilder.append(line);
                    }
                }
                CharSequence s = resultStringBuilder.toString();
                NSVGImage svg = NanoSVG.nsvgParse(s, "px", 96f);
                if (svg == null) return false;
                long rasterizer = NanoSVG.nsvgCreateRasterizer();

                int w = (int) svg.width();
                int h = (int) svg.height();
                float scale = Math.max(width / w, height / h);
                w = (int) (w * scale);
                h = (int) (h * scale);

                ByteBuffer image = MemoryUtil.memAlloc(w * h * 4);
                NanoSVG.nsvgRasterize(rasterizer, svg, 0, 0, scale, image, w, h, w * 4);

                NanoSVG.nsvgDeleteRasterizer(rasterizer);
                NanoSVG.nsvgDelete(svg);

                svgHashMap.put(name, new NVGAsset(NanoVG.nvgCreateImageRGBA(vg, w, h, flags, image), w, h));
                return true;
            } catch (Exception e) {
                System.err.println("Failed to parse SVG file");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Loads an assets from resources.
     *
     * @param vg     The NanoVG context.
     * @param svg    The SVG
     * @param width  The width of the SVG.
     * @param height The height of the SVG.
     * @return Whether the asset was loaded successfully.
     */
    @Override
    public boolean loadSVG(long vg, SVG svg, float width, float height) {
        return loadSVG(vg, svg.filePath, width, height, svg.flags, svg.getClass());
    }

    @Override
    public boolean loadSVG(long vg, SVG svg, float width, float height, Class<?> clazz) {
        return loadSVG(vg, svg.filePath, width, height, svg.flags, clazz);
    }

    /**
     * Loads an SVG from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param width    The width of the SVG.
     * @param height   The height of the SVG.
     * @return Whether the SVG was loaded successfully.
     */
    @Override
    public boolean loadSVG(long vg, String fileName, float width, float height, Class<?> clazz) {
        return loadSVG(vg, fileName, width, height, DEFAULT_FLAGS, clazz);
    }

    /**
     * Get a loaded assets from the cache.
     * <p><b>Requires the assets to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The assets
     * @see AssetHelperImpl#loadImage(long, String, Class)
     */
    @Override
    public int getImage(String fileName) {
        return imageHashMap.get(fileName).getImage();
    }

    /**
     * Get a loaded assets from the cache.
     * <p><b>Requires the assets to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The image and its data
     * @see AssetHelperImpl#loadImage(long, String, Class)
     */
    @Override
    public NVGAsset getNVGImage(String fileName) {
        return imageHashMap.get(fileName);
    }

    /**
     * Remove an assets from the cache, allowing the assets to be garbage collected.
     * Should be used when the GUI rendering the assets is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see AssetHelperImpl#loadImage(long, String, Class)
     */
    @Override
    public void removeImage(long vg, String fileName) {
        NanoVG.nvgDeleteImage(vg, imageHashMap.get(fileName).getImage());
        imageHashMap.remove(fileName);
    }

    /**
     * Clears all images from the cache, allowing the images cleared to be garbage collected.
     * Should be used when the GUI rendering loaded images are closed.
     *
     * @param vg The NanoVG context.
     */
    @Override
    public void clearImages(long vg) {
        Map<String, NVGAsset> temp = new HashMap<>(imageHashMap);
        for (String image : temp.keySet()) {
            NanoVG.nvgDeleteImage(vg, imageHashMap.get(image).getImage());
            imageHashMap.remove(image);
        }
    }

    /**
     * Get a loaded SVG from the cache.
     * <p><b>Requires the SVG to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The SVG
     * @see AssetHelperImpl#loadSVG(long, String, float, float, Class)
     */
    @Override
    public int getSVG(String fileName, float width, float height) {
        String name = fileName + "-" + width + "-" + height;
        return svgHashMap.get(name).getImage();
    }

    /**
     * Get a loaded assets from the cache.
     * <p><b>Requires the assets to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The SVG and its data
     * @see AssetHelperImpl#loadImage(long, String, Class)
     */
    @Override
    public NVGAsset getNVGSVG(String fileName) {
        return svgHashMap.get(fileName);
    }

    /**
     * Remove an SVG from the cache, allowing the SVG to be garbage collected.
     * Should be used when the GUI rendering the SVG is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see AssetHelperImpl#loadSVG(long, String, float, float, Class)
     */
    @Override
    public void removeSVG(long vg, String fileName, float width, float height) {
        String name = fileName + "-" + width + "-" + height;
        NanoVG.nvgDeleteImage(vg, imageHashMap.get(name).getImage());
        svgHashMap.remove(name);
    }

    /**
     * Clears all SVGs from the cache, allowing the SVGs cleared to be garbage collected.
     * Should be used when the GUI rendering loaded SVGs are closed.
     *
     * @param vg The NanoVG context.
     */
    @Override
    public void clearSVGs(long vg) {
        Map<String, NVGAsset> temp = new HashMap<>(svgHashMap);
        for (String image : temp.keySet()) {
            NanoVG.nvgDeleteImage(vg, svgHashMap.get(image).getImage());
            svgHashMap.remove(image);
        }
    }
}
