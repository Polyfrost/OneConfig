package io.polyfrost.oneconfig.lwjgl.image;

import io.polyfrost.oneconfig.lwjgl.IOUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
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

            ByteBuffer image = IOUtil.resourceToByteBufferNullable(fileName);
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
                InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("oneconfig", fileName)).getInputStream();
                StringBuilder resultStringBuilder = new StringBuilder();
                try (BufferedReader br
                             = new BufferedReader(new InputStreamReader(inputStream))) {
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

    public Image getImage(String fileName) {
        return imageHashMap.get(fileName);
    }

    public NSVGImage getSVG(String fileName) {
        return NSVGImageHashMap.get(fileName);
    }
}
