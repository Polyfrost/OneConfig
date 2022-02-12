package io.polyfrost.oneconfig.themes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

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
    private final ResourceLocation modIconsLoc;
    private final ResourceLocation smallIconsLoc;
    private final ResourceLocation logoLoc;
    private final ResourceLocation logoLocSmall;



    /**
     * Create a new theme instance for the window.
     * @param themePack file of the pack
     * @throws IOException if an error occurs reading metadata or unpacking, etc.
     */
    protected Theme(File themePack) throws IOException {
        super(themePack);
        long start = System.nanoTime();
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

        iconsLoc = createLargeIconAtlas();
        smallIconsLoc = createSmallIconAtlas();
        modIconsLoc = createModIconsAtlas();
        logoLoc = getLocationFromName("textures/logos/logo.png");
        logoLocSmall = getLocationFromName("textures/logos/logo_small.png");
        Themes.themeLog.info("Successfully loaded theme and created atlases in " + ((float) (System.nanoTime() - start)) / 1000000f + "ms");

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

    /**
     * Create the large icon atlas from this theme.
     */
    private ResourceLocation createLargeIconAtlas() {
        try {
            List<BufferedImage> icons = new ArrayList<>();
            icons.add(ImageIO.read(getResource("textures/icons/discord.png")));
            icons.add(ImageIO.read(getResource("textures/icons/docs.png")));
            icons.add(ImageIO.read(getResource("textures/icons/feedback.png")));
            icons.add(ImageIO.read(getResource("textures/icons/guide.png")));
            icons.add(ImageIO.read(getResource("textures/icons/hudsettings.png")));
            icons.add(ImageIO.read(getResource("textures/icons/modsettings.png")));
            icons.add(ImageIO.read(getResource("textures/icons/store.png")));
            icons.add(ImageIO.read(getResource("textures/icons/themes.png")));
            icons.add(ImageIO.read(getResource("textures/icons/update.png")));
            return createAtlasFromList(icons, 128);
        } catch (Exception e) {
            Themes.themeLog.error("Failed to create large icon atlas, is pack invalid?", e);
            return null;
        }
    }

    /**
     * Create the small icon atlas for this theme.
     */
    private ResourceLocation createSmallIconAtlas() {
        try {
            List<BufferedImage> icons = new ArrayList<>();
            icons.add(ImageIO.read(getResource("textures/smallicons/backarrow.png")));
            icons.add(ImageIO.read(getResource("textures/smallicons/close.png")));
            icons.add(ImageIO.read(getResource("textures/smallicons/forward.png")));
            icons.add(ImageIO.read(getResource("textures/smallicons/home.png")));
            icons.add(ImageIO.read(getResource("textures/smallicons/magnify.png")));
            icons.add(ImageIO.read(getResource("textures/smallicons/minimize.png")));
            icons.add(ImageIO.read(getResource("textures/smallicons/search.png")));
            return createAtlasFromList(icons, 32);
        } catch (Exception e) {
            Themes.themeLog.error("Failed to create small icon atlas, is pack invalid?", e);
            return null;
        }
    }

    /**
     * Create mod icon atlas for this theme.
     */
    private ResourceLocation createModIconsAtlas() {
        try {
            List<BufferedImage> icons = new ArrayList<>();
            icons.add(ImageIO.read(getResource("textures/mod/allmods.png")));
            icons.add(ImageIO.read(getResource("textures/mod/hudmods.png")));
            icons.add(ImageIO.read(getResource("textures/mod/hypixel.png")));
            icons.add(ImageIO.read(getResource("textures/mod/performance.png")));
            icons.add(ImageIO.read(getResource("textures/mod/pvp.png")));
            icons.add(ImageIO.read(getResource("textures/mod/qolmods.png")));
            icons.add(ImageIO.read(getResource("textures/mod/skyblock.png")));
            icons.add(ImageIO.read(getResource("textures/mod/utilities.png")));
            return createAtlasFromList(icons, 32);
        } catch (Exception e) {
            Themes.themeLog.error("Failed to create mod icon atlas, is pack invalid?", e);
            return null;
        }
    }


    /**
     * Create a texture atlas from the given list of images, vertically stacked.
     * @param images List of BufferedImages to use
     * @param imageSize image size in pixels (note: images must be square)
     * @return ResourceLocation of the atlas
     */
    public static ResourceLocation createAtlasFromList(@NotNull List<BufferedImage> images, int imageSize) {
        BufferedImage out = new BufferedImage(imageSize, (images.size() * imageSize), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = out.createGraphics();
        int i = 0;
        for (BufferedImage img : images) {
            graphics2D.drawImage(img, null, 0, i);
            i += imageSize;
        }
        graphics2D.dispose();
        return mc.getTextureManager().getDynamicTextureLocation(String.valueOf(i), new DynamicTexture(out));
    }


    /**
     * get a ResourceLocation of an image in the current theme.
     * @param name path of the resource (e.g. textures/logos/logo.png)
     * @throws IOException if the item cannot be located or pack is corrupt.
     */
    public ResourceLocation getLocationFromName(String name) throws IOException {
        return mc.getTextureManager().getDynamicTextureLocation(name, new DynamicTexture(ImageIO.read(getInputStreamByName(name))));
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

    /**
     * Get the large icon atlas.
     */
    public ResourceLocation getLargeIconAtlas() {
        return iconsLoc;
    }

    /**
     * Get the small icon atlas.
     */
    public ResourceLocation getSmallIconAtlas() {
        return smallIconsLoc;
    }

    /**
     * Get the mod icon atlas.
     */
    public ResourceLocation getModIconsAtlas() {
        return modIconsLoc;
    }

    /**
     * Get the logo for OneConfig as specified by this theme.
     */
    public ResourceLocation getOneConfigLogo() {
        return logoLoc;
    }

    /**
     * Get the small logo for OneConfig as specified by this theme.
     */
    public ResourceLocation getSmallOneConfigLogo() {
        return logoLocSmall;
    }

}
