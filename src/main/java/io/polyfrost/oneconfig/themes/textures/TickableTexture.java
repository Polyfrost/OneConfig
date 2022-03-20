package io.polyfrost.oneconfig.themes.textures;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.polyfrost.oneconfig.themes.textures.ThemeElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static io.polyfrost.oneconfig.themes.Themes.getActiveTheme;
import static io.polyfrost.oneconfig.themes.Themes.themeLog;

@SuppressWarnings("unused")
public class TickableTexture {
    private final int framesToSkip;
    private final BufferedImage image;
    private final int sizeX, sizeY, frames;
    private int tick;
    private int tick2;
    private final ThemeElement thisElement;
    private final ResourceLocation location;

    public TickableTexture(ThemeElement element) throws IOException {
        thisElement = element;
        InputStream inputStream = getActiveTheme().getResource(element.location);
        image = ImageIO.read(inputStream);
        location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(element.location, new DynamicTexture(image));
        sizeX = image.getWidth();
        sizeY = image.getHeight();
        frames = sizeY / sizeX;
        int frametime;
        try {
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(getActiveTheme().getResource(element.location + ".json"))).getAsJsonObject();
            frametime = jsonObject.get("frametime").getAsInt();
            if (frametime == 0) {
                frametime = 1;
                themeLog.warn("You cannot have a frame tick time of 0. This will mean there is no animation as it will happen impossibly fast. Defaulting to 1, as we assume you wanted it fast.");
            }
        } catch (Exception e) {
            themeLog.error("failed to load metadata for tickable texture (" + element.location + "). Setting default (5)");
            frametime = 5;
        }
        framesToSkip = frametime;
    }

    public void draw(int x, int y) {
        GlStateManager.enableBlend();
        GlStateManager.color(1f,1f,1f,1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        if (tick < frames) {
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, (tick * sizeX), sizeX, sizeX, sizeX, sizeX);
            tick2++;
            if (tick2 == framesToSkip) {
                tick2 = 0;
                tick++;
            }
        }
        if (tick == frames) {
            tick = 0;
        }
        GlStateManager.disableBlend();
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getFrameTime() {
        return framesToSkip;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public ThemeElement getElement() {
        return thisElement;
    }
}
