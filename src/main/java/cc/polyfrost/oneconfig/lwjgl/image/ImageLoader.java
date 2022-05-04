package cc.polyfrost.oneconfig.lwjgl.image;

import cc.polyfrost.oneconfig.utils.IOUtils;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class ImageLoader {
    private final HashMap<String, Image> imageHashMap = new HashMap<>();
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


    public void removeImage(String fileName) {
        imageHashMap.remove(fileName);
    }

    public Image getImage(String fileName) {
        return imageHashMap.get(fileName);
    }

}
