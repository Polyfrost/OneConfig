package io.polyfrost.oneconfig.themes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.polyfrost.oneconfig.renderer.Renderer;
import io.polyfrost.oneconfig.renderer.TrueTypeFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

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
    private final TextureManager manager;
    private final TrueTypeFont boldFont;
    private final TrueTypeFont normalFont;



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
        TrueTypeFont normalFontTemp;
        TrueTypeFont boldFontTemp;
        try {
            boldFontTemp = new TrueTypeFont(Font.createFont(Font.TRUETYPE_FONT, getResource("textures/fonts/font_bold.ttf")).deriveFont(30f), true);
            normalFontTemp = new TrueTypeFont(Font.createFont(Font.TRUETYPE_FONT, getResource("textures/fonts/font.ttf")).deriveFont(12f), true);
        } catch (FontFormatException e) {
            Themes.themeLog.error("failed to derive fonts, is theme invalid?",e);
            e.printStackTrace();
            try {
                normalFontTemp = new TrueTypeFont(Font.createFont(Font.TRUETYPE_FONT, mc.getResourceManager().getResource(new ResourceLocation("oneconfig", "textures/fonts/font.ttf")).getInputStream()).deriveFont(12f), true);
                boldFontTemp = new TrueTypeFont(Font.createFont(Font.TRUETYPE_FONT, mc.getResourceManager().getResource(new ResourceLocation("oneconfig", "textures/fonts/font_bold.ttf")).getInputStream()).deriveFont(30f), true);
            } catch (FontFormatException ex) {
                ex.printStackTrace();
                throw new ReportedException(new CrashReport("Failed to get fallback fonts! game will crash :(", ex));
            }
        }
        normalFont = normalFontTemp;
        boldFont = boldFontTemp;
        manager = new TextureManager(this);
        if(Themes.VERSION != version) {
            Themes.themeLog.warn("Theme was made for a different version of OneConfig! This may cause issues in the GUI.");
        }
        Themes.themeLog.info("Successfully loaded theme in " + ((float) (System.nanoTime() - start)) / 1000000f + "ms");

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
     * Get the texture manager for this theme, with all drawing utilities.
     */
    public TextureManager getTextureManager() {
        return manager;
    }

    /**
     * Get the font from this theme.
     */
    public TrueTypeFont getFont() {
        return normalFont;
    }

    /**
     * Get the bold, larger font from this theme.
     */
    public TrueTypeFont getBoldFont() {
        return boldFont;
    }

}
