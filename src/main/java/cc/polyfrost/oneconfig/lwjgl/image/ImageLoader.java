package cc.polyfrost.oneconfig.lwjgl.image;

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

/**
 * Loads images and SVGs from resources into NanoVG.
 *
 * @see cc.polyfrost.oneconfig.lwjgl.RenderManager
 * @see Images
 * @see SVGs
 */
public final class ImageLoader {
    private ImageLoader() {

    }

    private final HashMap<String, Integer> imageHashMap = new HashMap<>();
    private final HashMap<String, Integer> svgHashMap = new HashMap<>();
    public static ImageLoader INSTANCE = new ImageLoader();

    /**
     * Loads an image from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @return Whether the image was loaded successfully.
     */
    public boolean loadImage(long vg, String fileName) {
        if (!imageHashMap.containsKey(fileName)) {
            int[] width = {0};
            int[] height = {0};
            int[] channels = {0};

            ByteBuffer image = IOUtils.resourceToByteBufferNullable(fileName);
            if (image == null) {
                return false;
            }

            ByteBuffer buffer = STBImage.stbi_load_from_memory(image, width, height, channels, 4);
            if (buffer == null) {
                return false;
            }

            imageHashMap.put(fileName, NanoVG.nvgCreateImageRGBA(vg, width[0], height[0], NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS, buffer));
            return true;
        }
        return true;
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
    public boolean loadSVG(long vg, String fileName, float width, float height) {
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

                svgHashMap.put(name, NanoVG.nvgCreateImageRGBA(vg, w, h, NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS, image));
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
     * Get a loaded image from the cache.
     * <p><b>Requires the image to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The image
     * @see ImageLoader#loadImage(long, String)
     */
    public int getImage(String fileName) {
        return imageHashMap.get(fileName);
    }

    /**
     * Remove an image from the cache, allowing the image to be garbage collected.
     * Should be used when the GUI rendering the image is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see ImageLoader#loadImage(long, String)
     */
    public void removeImage(long vg, String fileName) {
        NanoVG.nvgDeleteImage(vg, imageHashMap.get(fileName));
        imageHashMap.remove(fileName);
    }

    /**
     * Clears all images from the cache, allowing the images cleared to be garbage collected.
     * Should be used when the GUI rendering loaded images are closed.
     *
     * @param vg The NanoVG context.
     */
    public void clearImages(long vg) {
        HashMap<String, Integer> temp = new HashMap<>(imageHashMap);
        for (String image : temp.keySet()) {
            NanoVG.nvgDeleteImage(vg, imageHashMap.get(image));
            imageHashMap.remove(image);
        }
    }

    /**
     * Get a loaded SVG from the cache.
     * <p><b>Requires the SVG to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The SVG
     * @see ImageLoader#loadSVG(long, String, float, float)
     */
    public int getSVG(String fileName, float width, float height) {
        String name = fileName + "-" + width + "-" + height;
        return svgHashMap.get(name);
    }

    /**
     * Remove a SVG from the cache, allowing the SVG to be garbage collected.
     * Should be used when the GUI rendering the SVG is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see ImageLoader#loadSVG(long, String, float, float)
     */
    public void removeSVG(long vg, String fileName, float width, float height) {
        String name = fileName + "-" + width + "-" + height;
        NanoVG.nvgDeleteImage(vg, imageHashMap.get(name));
        svgHashMap.remove(name);
    }

    /**
     * Clears all SVGs from the cache, allowing the SVGs cleared to be garbage collected.
     * Should be used when the GUI rendering loaded SVGs are closed.
     *
     * @param vg The NanoVG context.
     */
    public void clearSVGs(long vg) {
        HashMap<String, Integer> temp = new HashMap<>(svgHashMap);
        for (String image : temp.keySet()) {
            NanoVG.nvgDeleteImage(vg, svgHashMap.get(image));
            svgHashMap.remove(image);
        }
    }
}
