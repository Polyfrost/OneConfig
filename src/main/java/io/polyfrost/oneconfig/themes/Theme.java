package io.polyfrost.oneconfig.themes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FileResourcePack;

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
     * Get the accent color for the window, used on separators, lines, etc.
     */
    public Color getAccentColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("accent_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the base color for the window, used for the main background.
     */
    public Color getBaseColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("base_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the base color for the buttons, items, and most elements.
     */
    public Color getElementColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("element_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the accent color for elements when they are hovered.
     */
    public Color getHoverColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("hover_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the accent color for elements when they are clicked.
     */
    public Color getClickColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("click_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the color for the close/destroy buttons.
     */
    public Color getCloseColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("base_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the color for the main text, like titles, config element items, etc.
     */
    public Color getTextColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("title_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Get the accent color for the text, used for subtitles, etc.
     */
    public Color getAccentTextColor() {
        JsonObject colors = packConfig.getAsJsonObject("colors");
        int accentColor = colors.get("subtitle_color").getAsInt();
        return Renderer.getColorFromInt(accentColor);
    }
    /**
     * Weather or not to round off the corners of pretty much every element.
     */
    public boolean shouldRoundCorners() {
        return packConfig.get("round_corners").getAsBoolean();
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
        try {
            return packMetadata.get("description").getAsString();
        } catch (Exception e) {
            Themes.themeLog.error("Failed to get pack name. Defaulting to 'error occurred', is pack invalid??");
        }
        return "Couldn't get description!";
    }

    /**
     * Get the friendly name of the pack.
     */
    public String getName() {
        try {
            return packMetadata.get("name").getAsString();
        } catch (Exception e) {
            Themes.themeLog.error("Failed to get pack name. Defaulting to 'null', is pack invalid??");
        }
        return "null";

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
        try {
            return packMetadata.get("version").getAsInt();
        } catch (Exception e) {
            Themes.themeLog.error("Failed to get pack version. Is pack invalid?");
        }
        return 0;
    }

    /**
     * Get the source file of this theme.
     */
    public File getThemeFile() {
        return themeFile;
    }


}
