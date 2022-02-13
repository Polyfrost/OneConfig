package io.polyfrost.oneconfig.themes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.polyfrost.oneconfig.themes.Themes.themeLog;

public class TextureManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final List<ResourceLocation> resources = new ArrayList<>();
    private final List<ThemeElement> tickableTextureLocations = new ArrayList<>();
    private final HashMap<Integer, Integer> tickableTextures = new HashMap<>();
    private int tick = 0;

    /**
     * Create a new texture manager for this theme, used for drawing of icons, etc.
     */
    public TextureManager(Theme theme) {
        for(ThemeElement element : ThemeElement.values()) {
            BufferedImage img;
            try {
                img = ImageIO.read(theme.getResource(element.location));
            } catch (Exception e) {
                themeLog.error("failed to get themed texture: " + element.location + ", having to fallback to default one. Is pack invalid?");
                img = new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
                // TODO add fallback
            }
            ResourceLocation location = mc.getTextureManager().getDynamicTextureLocation(element.location, new DynamicTexture(img));
            resources.add(location);
            if(img.getWidth() != element.size) {
                themeLog.warn("Theme element " + element.name() + " with size " + img.getWidth() + "px is not recommended, expected " + element.size + "px. Continuing anyway.");
            }
            if(img.getWidth() != img.getHeight()) {
                themeLog.info("found tickable animated texture (" + element.name() + "). Loading texture");
                tickableTextureLocations.add(element);
                tickableTextures.put(img.getWidth(), img.getHeight());
            }
        }
    }

    /**
     * Draw the specified icon at the coordinates, scaled to the width and height.
     * @param element element to draw
     * @param x x coordinate (top left)
     * @param y y coordinate (top left)
     * @param width width of the image
     * @param height height of the image
     */
    public void draw(ThemeElement element, int x, int y, int width, int height) {
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        ResourceLocation location = resources.get(element.ordinal());
        mc.getTextureManager().bindTexture(location);
        try {
            if(tickableTextureLocations.contains(element)) {
                int texWidth = tickableTextures.keySet().stream().findFirst().get();       // TODO unsure if this works safe
                int texHeight = tickableTextures.values().stream().findFirst().get();
                int frames = texHeight / texWidth;
                while(tick < frames) {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, 0, (tick * texWidth), texWidth, texWidth, texWidth, texWidth);
                    tick++;
                    if(tick == frames) {
                        tick = 0;
                    }
                }
            } else {
                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, width, height, width, height, width, height);
            }
        } catch (Exception e) {
            themeLog.error("Error occurred drawing texture " + element.name() + ", is theme invalid?", e);
        }
    }
}
