package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.renderer.Renderer;
import io.polyfrost.oneconfig.themes.Theme;
import io.polyfrost.oneconfig.themes.Themes;
import io.polyfrost.oneconfig.themes.textures.ThemeElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;

import static io.polyfrost.oneconfig.gui.Window.resolution;

/**
 * Default simple block for all the GUI elements. If you are making custom ones, your class should extend this one.
 */
@SuppressWarnings("unused")
public class OCBlock {
    public static final Theme theme = Themes.getActiveTheme();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private Color color;
    private String text;
    private final boolean bold;
    /**
     * Width of the element in pixels.
     */
    public int width;
    /**
     * Height of the element in pixels.
     */
    public int height;
    private ThemeElement element;
    private boolean clicked = false;
    private boolean rightClicked = false;
    private int mouseX, mouseY;
    private boolean hovered;

    /**
     * Create a basic element with nothing. Used for extended classes.
     */
    public OCBlock(int width, int height) {
        this(null, false, -1, width, height);
    }

    /**
     * Create a new basic element.
     * @param color color of the element
     * @param width width of the element
     * @param height height of the element
     */
    public OCBlock(int color, int width, int height) {
        this(null, false, color, width, height);
    }

    /**
     * Create a new element with the specified text, and automatic width/height + padding.
     * @param text text to use
     * @param bold weather or not to use bold text
     * @param color color of the background to use
     */
    public OCBlock(@NotNull String text, boolean bold, int color) {
        this(text, bold, color, theme.getFont().getWidth(text) + 6, theme.getFont().getHeight() + 4);
    }

    /**
     * Create a new element with the specified text, and custom width/height.
     * @param text text to use
     * @param bold weather or not to use bold text
     * @param color color of the background to use
     */
    public OCBlock(String text, boolean bold, int color, int width, int height) {
        this.text = text;
        this.bold = bold;
        this.color = Renderer.getColorFromInt(color);
        this.width = width;
        this.height = height;
    }

    /**
     * Create a new Element with the specified image.
     * @param element element to use
     * @param colorMask color mast to use (-1 for default)
     */
    public OCBlock(ThemeElement element, int colorMask, int width, int height) {
        this.element = element;
        this.color = Renderer.getColorFromInt(colorMask);
        this.width = width;
        this.height = height;
        this.bold = false;
    }

    /**
     * Draw the element at the specified coordinates.
     */
    public void draw(int x, int y) {
        update(x, y);
        if(element != null) {
            Gui.drawRect(x, y, x + width, y + height, color.getRGB());
            GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            theme.getTextureManager().draw(element, x, y, width, height);
        }
        if(text == null) {
            Gui.drawRect(x, y, x + width, y + height, color.getRGB());
        }
        else {
            Gui.drawRect(x, y, x + width, y + height, color.getRGB());
            if(bold) {
                theme.getBoldFont().drawString(text, x + 3, y + 2, 1f, 1f, -1);
            } else {
                theme.getFont().drawString(text, x + 3, y + 2, 1.1f, 1f, -1);
            }
        }

    }

    /**
     * Update this elements click, key and hover status. Call this method at the end of your 'draw' function, if overridden.
     */
    public void update(int x, int y) {
        int mouseX = Mouse.getX() / resolution.getScaleFactor();
        int mouseY = Math.abs((Mouse.getY() / resolution.getScaleFactor()) - resolution.getScaledHeight());
        hovered = mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
        if(hovered) {
            onHover();
            if (Mouse.isButtonDown(0) && !clicked) {
                onClick(0);
            }
            clicked = Mouse.isButtonDown(0);

            if (Mouse.isButtonDown(1) && !rightClicked) {
                onClick(1);
            }
            rightClicked = Mouse.isButtonDown(1);
            onKeyPress(Keyboard.getEventKey());
        }
        if(!hovered && clicked) clicked = false;
    }

    /**
     * Draw the element with the specified coordinates, width and height.
     */
    public void draw(int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        draw(x, y);
    }


    /**
     * Override this method to set a function when a key is pressed while this element is hovered.
     * @param keyCode key code that was pressed (check org.lwjgl.Keyboard for keymap)
     */
    public void onKeyPress(int keyCode) {

    }

    /**
     * Override this method to set a function when the element is hovered.
     * @param button the button that was pressed (0 is left, 1 is right)
     */
    public void onClick(int button) {

    }


    /**
     * Override this method to set a function when the element is hovered.
     */
    public void onHover() {

    }

    public void setText(String text) {
        this.text = text;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public Color getColor() {
        return color;
    }

    public int getWidth() {
        return width;
    }

    public String getText() {
        return text;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isClicked() {
        return clicked;
    }
}
