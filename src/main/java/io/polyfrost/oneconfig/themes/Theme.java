package io.polyfrost.oneconfig.themes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Theme extends FileResourcePack {
    private final File themeFile;
    private final File themeConfigFile;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final JsonObject packMetadata;
    private final JsonObject packConfig;
    private final Color accentColor;
    private final Color elementColor;
    private final Color titleBarColor;
    private final Color baseColor;
    private final Color hoverColor;
    private final Color clickColor;
    private final Color closeColor;
    private final Color titleColor;
    private final Color subTitleColor;
    private final boolean roundCorners;
    private final String description;
    private final String title;
    private final int version;
    private final ResourceLocation iconsLoc;

    /**
     * Create a new theme instance for the window.
     * @param themePack file of the pack
     * @throws IOException if an error occurs reading metadata or unpacking, etc.
     */
    protected Theme(File themePack) throws IOException {
        super(themePack);
        themeFile = themePack;
        themeConfigFile = new File(themeFile.getPath() + ".json");
        packMetadata = new JsonParser().parse(new InputStreamReader(getInputStreamByName("pack.json"))).getAsJsonObject();
        try {
            unpackConfig();
        } catch (Exception e) {
            Themes.themeLog.error("failed to unpack config!", e);
            unpackConfig();
        }
        packConfig = new JsonParser().parse(new FileReader(themeConfigFile)).getAsJsonObject();
        JsonObject colors = packConfig.getAsJsonObject("colors");
        accentColor = Renderer.getColorFromInt(colors.get("accent_color").getAsInt());
        baseColor = Renderer.getColorFromInt(colors.get("base_color").getAsInt());
        titleBarColor = Renderer.getColorFromInt(colors.get("title_bar").getAsInt());
        elementColor = Renderer.getColorFromInt(colors.get("element_color").getAsInt());
        clickColor = Renderer.getColorFromInt(colors.get("click_color").getAsInt());
        closeColor = Renderer.getColorFromInt(colors.get("close_color").getAsInt());
        titleColor = Renderer.getColorFromInt(colors.get("title_color").getAsInt());
        hoverColor = Renderer.getColorFromInt(colors.get("hover_color").getAsInt());
        subTitleColor = Renderer.getColorFromInt(colors.get("subtitle_color").getAsInt());
        roundCorners = packConfig.get("round_corners").getAsBoolean();
        String title, description;
        int version;
        try {
            title = packMetadata.get("name").getAsString();
            description = packMetadata.get("description").getAsString();
            version = packMetadata.get("version").getAsInt();
        } catch (Exception e) {
            title = "null";
            description = "no valid pack.json found!";
            version = -1;
            Themes.themeLog.error("pack has invalid metadata! Using default values.");
        }
        this.title = title;
        this.description = description;
        this.version = version;

        iconsLoc = createIconAtlas();
    }

    /**
     * Attempt to unpack the theme default config if it doesn't already exist.
     */
    private void unpackConfig() throws IOException {
        if (themeConfigFile.createNewFile()) {
            Themes.themeLog.warn("Creating config file for theme " + themeFile.getName() + ", assuming it has never been opened before.");
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(getInputStreamByName("default_config.json")));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            FileWriter fileWriter = new FileWriter(themeConfigFile);
            fileWriter.write(responseStrBuilder.toString());
            fileWriter.close();
        }

    }

    // TIME CALC
    // long start = System.nanoTime();
    // System.out.println(((float) (System.nanoTime() - start)) / 1000000f + "ms");

    /**
     * Create the large icon atlas from this theme.
     */
    private ResourceLocation createIconAtlas() {        // TODO finish for all atlases
        try {
            List<BufferedImage> icons = new ArrayList<>();
            BufferedImage out = new BufferedImage(128, 1152, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = out.createGraphics();
            int i = 0;
            icons.add(ImageIO.read(getResource("textures/icons/discord.png")));
            icons.add(ImageIO.read(getResource("textures/icons/docs.png")));
            icons.add(ImageIO.read(getResource("textures/icons/feedback.png")));
            icons.add(ImageIO.read(getResource("textures/icons/guide.png")));
            icons.add(ImageIO.read(getResource("textures/icons/hudsettings.png")));
            icons.add(ImageIO.read(getResource("textures/icons/modsettings.png")));
            icons.add(ImageIO.read(getResource("textures/icons/store.png")));
            icons.add(ImageIO.read(getResource("textures/icons/themes.png")));
            icons.add(ImageIO.read(getResource("textures/icons/update.png")));
            for (BufferedImage img : icons) {
                graphics2D.drawImage(img, null, 0, i);
                i += 128;
            }
            graphics2D.dispose();
            return mc.getTextureManager().getDynamicTextureLocation("OneConfigIconAtlas", new DynamicTexture(out));
        } catch (Exception e) {
            Themes.themeLog.error("Failed to create large icon atlas", e);
            return null;
        }
    }



    /**
     * Get the accent color for the window, used on separators, lines, etc.
     */
    public Color getAccentColor() {
        return accentColor;
    }
    /**
     * Get the base color for the window, used for the main background.
     */
    public Color getBaseColor() {
        return baseColor;
    }
    /**
     * Get the color used for the title bar.
     */
    public Color getTitleBarColor() {
        return titleBarColor;
    }
    /**
     * Get the base color for the buttons, items, and most elements.
     */
    public Color getElementColor() {
        return elementColor;
    }
    /**
     * Get the accent color for elements when they are hovered.
     */
    public Color getHoverColor() {
        return hoverColor;
    }
    /**
     * Get the accent color for elements when they are clicked.
     */
    public Color getClickColor() {
        return clickColor;
    }
    /**
     * Get the color for the close/destroy buttons.
     */
    public Color getCloseColor() {
        return closeColor;
    }
    /**
     * Get the color for the main text, like titles, config element items, etc.
     */
    public Color getTextColor() {
        return titleColor;
    }
    /**
     * Get the accent color for the text, used for subtitles, etc.
     */
    public Color getAccentTextColor() {
        return subTitleColor;
    }
    /**
     * Weather or not to round off the corners of pretty much every element.
     */
    public boolean shouldRoundCorners() {
        return roundCorners;
    }


    /**
     * Get the InputStream of a resource in the pack.
     * @param name name of the resource
     * @throws IOException an error occurs reading
     */
    public InputStream getResource(String name) throws IOException {
        return getInputStreamByName(name);
    }

    /**
     * Get this pack's metadata json.
     */
    public JsonObject getPackMetaData() {
        return packMetadata;
    }

    /**
     * Get the pack's config file.
     */
    public JsonObject getPackConfig() {
        return packConfig;
    }

    /**
     * Get the pack's description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the friendly name of the pack.
     */
    public String getName() {
        return title;

    }

    /**
     * Get the pack's title image.
     */
    public BufferedImage getImage() {
        try {
            return getPackImage();
        } catch (IOException e) {
            Themes.themeLog.error("Failed to parse pack image. Is pack invalid??");
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the large icon atlas.
     */
    public ResourceLocation getIcons() {
        return iconsLoc;
    }

    /**
     * Get the pack's version. Not used yet, but will be when more features are added for theme compatability.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get the source file of this theme.
     */
    public File getThemeFile() {
        return themeFile;
    }


}
