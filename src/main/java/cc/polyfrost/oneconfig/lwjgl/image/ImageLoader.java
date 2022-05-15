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

public class ImageLoader {
    private final HashMap<String, Integer> imageHashMap = new HashMap<>();
    private final HashMap<String, Integer> SVGHashMap = new HashMap<>();
    public static ImageLoader INSTANCE = new ImageLoader();

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

    public boolean loadSVG(long vg, String fileName, float SVGWidth, float SVGHeight) {
        String name = fileName + "-" + SVGWidth + "-" + SVGHeight;
        if (!SVGHashMap.containsKey(name)) {
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
                float scale = Math.max(SVGWidth / w, SVGHeight / h);
                w = (int) (w * scale);
                h = (int) (h * scale);

                ByteBuffer image = MemoryUtil.memAlloc(w * h * 4);
                NanoSVG.nsvgRasterize(rasterizer, svg, 0, 0, scale, image, w, h, w * 4);

                NanoSVG.nsvgDeleteRasterizer(rasterizer);
                NanoSVG.nsvgDelete(svg);

                SVGHashMap.put(name, NanoVG.nvgCreateImageRGBA(vg, w, h, NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS, image));
                return true;
            } catch (Exception e) {
                System.err.println("Failed to parse SVG file");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public int getImage(String fileName) {
        return imageHashMap.get(fileName);
    }

    public void removeImage(long vg, String fileName) {
        NanoVG.nvgDeleteImage(vg, imageHashMap.get(fileName));
        imageHashMap.remove(fileName);
    }

    public void clearImages(long vg) {
        HashMap<String, Integer> temp = new HashMap<>(imageHashMap);
        for (String image : temp.keySet()) {
            NanoVG.nvgDeleteImage(vg, imageHashMap.get(image));
            imageHashMap.remove(image);
        }
     }

    public int getSVG( String fileName, float width, float height) {
        String name = fileName + "-" + width + "-" + height;
        return SVGHashMap.get(name);
    }

    public void removeSVG(long vg, String fileName, float width, float height) {
        String name = fileName + "-" + width + "-" + height;
        NanoVG.nvgDeleteImage(vg, imageHashMap.get(name));
        SVGHashMap.remove(name);
    }

    public void clearSVGs(long vg) {
        HashMap<String, Integer> temp = new HashMap<>(SVGHashMap);
        for (String image : temp.keySet()) {
            NanoVG.nvgDeleteImage(vg, SVGHashMap.get(image));
            SVGHashMap.remove(image);
        }
    }
}
