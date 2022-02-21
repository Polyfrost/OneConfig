package io.polyfrost.oneconfig.themes.textures;

import io.polyfrost.oneconfig.themes.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.polyfrost.oneconfig.themes.Themes.activeTheme;
import static io.polyfrost.oneconfig.themes.Themes.themeLog;

public class TextureManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final List<ResourceLocation> resources = new ArrayList<>();
    private final List<TickableTexture> tickableTextures = new ArrayList<>();

    /**
     * Create a new texture manager for this theme, used for drawing of icons, etc.
     */
    public TextureManager(Theme theme) {
        for (ThemeElement element : ThemeElement.values()) {
            BufferedImage img;
            try {
                img = ImageIO.read(theme.getResource(element.location));
            } catch (Exception e) {
                themeLog.error("failed to get themed texture: " + element.location + ", having to fallback to default one. Is pack invalid?");
                try {
                    img = ImageIO.read(mc.getResourceManager().getResource(new ResourceLocation("oneconfig", element.location)).getInputStream());
                } catch (IOException ex) {
                    themeLog.fatal("failed to get fallback texture: " + element.location + ", game will crash :(");
                    throw new ReportedException(new CrashReport("TextureManager failure: FALLBACK_ERROR_OR_MISSING", ex));
                }
            }
            ResourceLocation location = mc.getTextureManager().getDynamicTextureLocation(element.location, new DynamicTexture(img));
            resources.add(location);
            if (img.getWidth() != element.size) {
                themeLog.warn("Theme element " + element.name() + " with size " + img.getWidth() + "px is not recommended, expected " + element.size + "px. Continuing anyway.");
            }
            if(element.ordinal() < 26) {
                if (img.getWidth() != img.getHeight()) {
                    themeLog.info("found tickable animated texture (" + element.name() + "). Loading texture");
                    try {
                        tickableTextures.add(new TickableTexture(element));
                    } catch (IOException e) {
                        themeLog.error("failed to create TickableTexture " + element.location + ". Just going to load it as a normal texture, this may break things!");
                        e.printStackTrace();
                    }
                }
            } else {
                if(element.ordinal() < 29) {
                    if(img.getHeight() != 144 || img.getWidth() != 758) {
                        themeLog.warn("found badly sized button texture " + element.location);
                    }
                }
            }
        }
    }

    /**
     * Draw the specified icon at the coordinates, scaled to the width and height.
     *
     * @param element element to draw
     * @param x       x coordinate (top left)
     * @param y       y coordinate (top left)
     * @param width   width of the image
     * @param height  height of the image
     */
    public void draw(@NotNull ThemeElement element, int x, int y, int width, int height) {
        if (activeTheme.isReady()) {
            ResourceLocation location = resources.get(element.ordinal());
            mc.getTextureManager().bindTexture(location);
            GlStateManager.enableBlend();
            try {
                if (!tickableTextures.isEmpty()) {
                    for (TickableTexture texture : tickableTextures) {
                        if (texture.getElement().equals(element)) {
                            texture.draw(x, y);
                        } else {
                            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, width, height, width, height, width, height);
                        }
                    }
                } else {
                    Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, width, height, width, height, width, height);
                }
                GlStateManager.disableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
            } catch (Exception e) {
                themeLog.error("Error occurred drawing texture " + element.name() + ", is theme invalid?", e);
            }
        }
    }
}
