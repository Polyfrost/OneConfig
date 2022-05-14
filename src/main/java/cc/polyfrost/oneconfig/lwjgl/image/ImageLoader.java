package cc.polyfrost.oneconfig.lwjgl.image;

import cc.polyfrost.oneconfig.utils.IOUtils;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.stb.STBImage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ImageLoader {
    private final HashMap<String, Image> imageHashMap = new HashMap<>();
    private final HashMap<String, NSVGImage> NSVGImageHashMap = new HashMap<>();
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

            imageHashMap.put(fileName, new Image(NanoVG.nvgCreateImageRGBA(vg, width[0], height[0], NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS, buffer), buffer));
            return true;
        }
        return true;
    }

    public boolean loadSVGImage(String fileName) {
        if(!NSVGImageHashMap.containsKey(fileName)) {
            try {
                InputStream inputStream = this.getClass().getResourceAsStream(fileName);
                StringBuilder resultStringBuilder = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        resultStringBuilder.append(line);
                    }
                }
                CharSequence s = resultStringBuilder.toString();
                System.out.println(s);
                NSVGImage image = NanoSVG.nsvgParse(s, "px", 96f);
                NSVGImageHashMap.put(fileName, image);
                System.out.println("Loaded SVG: " + fileName);
            } catch (Exception e) {             // just in case
                System.err.println("Failed to parse SVG file");
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return true;
    }

    public void removeImage(String fileName) {
        imageHashMap.remove(fileName);
    }

    public Image getImage(String fileName) {
        return imageHashMap.get(fileName);
    }

    public NSVGImage getSVG(String fileName) {
        return NSVGImageHashMap.get(fileName);
    }
}
