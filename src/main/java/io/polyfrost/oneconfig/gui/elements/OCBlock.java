package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.renderer.Renderer;
import io.polyfrost.oneconfig.themes.Theme;
import io.polyfrost.oneconfig.themes.Themes;
import io.polyfrost.oneconfig.themes.textures.ThemeElement;
import net.minecraft.client.Minecraft;
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
    private float percentHoveredRed = 0f;
    private float percentHoveredGreen = 0f;
    private float percentHoveredBlue = 0f;
    private float percentHoveredAlpha = 0f;
    private final Color elementColor = theme.getElementColor();
    private final Color hoverColor = theme.getHoverColor();
    private final Runnable draw;

    /**
     * Create a basic element.
     */
    public OCBlock(int width, int height) {
        this(null, false, theme.getElementColor().getRGB(), width, height);
    }

    /**
     * Create a new basic element.
     * @param color color of the element
     * @param width width of the element
     * @param height height of the element
     * @deprecated
     * This method DOES NOT respect the theme colors for the element. Use of {@link #OCBlock(int, int)} is recommended instead.
     */
    @Deprecated()
    public OCBlock(int color, int width, int height) {
        this(null, false, color, width, height);
    }

    /**
     * Create a new element with the specified text, and automatic width/height + padding.
     * @param text text to use
     * @param bold weather or not to use bold text
     * @param color color for the text
     */
    public OCBlock(@NotNull String text, boolean bold, int color) {
        this(text, bold, color, theme.getFont().getWidth(text) + 6, theme.getFont().getHeight() + 4);
    }

    /**
     * Create a new element with the specified text, and custom width/height.
     * @param text text to use
     * @param bold weather or not to use bold text
     * @param color color for the text (use {@link Theme#getTextColor()} or {@link Theme#getAccentTextColor()} for default colors)
     */
    public OCBlock(String text, boolean bold, int color, int width, int height) {
        this.draw = null;
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
        this.draw = null;
        this.element = element;
        this.color = Renderer.getColorFromInt(colorMask);
        this.width = width;
        this.height = height;
        this.bold = false;
    }

    /**
     * Create a new Element with a custom render script. The {@link Runnable} should ONLY contain #draw() calls or equivalent.
     * @param whatToDraw a {@link Runnable}, containing draw scripts for elements. You will need to instantiate the objects first, if they are sub-elements.
     */
    public OCBlock(Runnable whatToDraw, int width, int height) {
        this.draw = whatToDraw;
        this.bold = false;
        this.width = width;
        this.height = height;
    }

    /**
     * Draw the element at the specified coordinates.
     */
    public void draw(int x, int y) {
        GlStateManager.enableBlend();
        percentHoveredRed = smooth(percentHoveredRed, elementColor.getRed() / 255f, hoverColor.getRed() / 255f);
        percentHoveredGreen = smooth(percentHoveredGreen, elementColor.getGreen() / 255f, hoverColor.getGreen() / 255f);
        percentHoveredBlue = smooth(percentHoveredBlue, elementColor.getBlue() / 255f, hoverColor.getBlue() / 255f);
        percentHoveredAlpha = smooth(percentHoveredAlpha, elementColor.getAlpha() / 255f, hoverColor.getAlpha() / 255f);
        GlStateManager.color(percentHoveredRed, percentHoveredGreen, percentHoveredBlue, percentHoveredAlpha);
        update(x, y);
        if(draw != null) {
            draw.run();
        }
        if(element != null) {
            theme.getTextureManager().draw(ThemeElement.BUTTON, x, y, width, height);
            GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            theme.getTextureManager().draw(element, x, y, width, height);
        }
        if(text == null) {
            theme.getTextureManager().draw(ThemeElement.BUTTON, x, y, width, height);
        }
        else {
            theme.getTextureManager().draw(ThemeElement.BUTTON, x, y, width, height);
            if(bold) {
                theme.getBoldFont().drawString(text, x + 3, y + 2, 1f, 1f, color.getRGB());
            } else {
                theme.getFont().drawString(text, x + 3, y + 2, 1.1f, 1f, color.getRGB());
            }
        }
        GlStateManager.disableBlend();


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
        if(clicked) {
            Renderer.color(theme.getClickColor());
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


    private float smooth(float current, float min, float max) {
        current = Renderer.easeOut(current, isHovered() ? 1f : 0f);
        if(current <= min) {
            current = min;
        }

        if(current >= max) {
            current = max;
        }
        return current;
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
