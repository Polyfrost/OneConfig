package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.renderer.Renderer;
import io.polyfrost.oneconfig.themes.textures.ThemeElement;
import net.minecraft.client.renderer.GlStateManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class OCButton extends OCBlock {
    private final Color elementColor = theme.getElementColor();
    private final Color hoverColor = theme.getHoverColor();
    private float percentHoveredRed = 0f;
    private float percentHoveredGreen = 0f;
    private float percentHoveredBlue = 0f;
    private float percentHoveredAlpha = 0f;
    private float percentDescription = 0f;
    private ThemeElement element;
    private boolean alwaysShowDesc = true;
    private String title, description;

    /**
     * Create an empty button.
     */
    public OCButton(int width, int height) {
        super(width, height);
    }

    /**
     * Create a new button with the specified texture.
     */
    public OCButton(ThemeElement element) {
        super(element.size + 2, element.size + 2);
        this.element = element;
    }

    public OCButton(@NotNull String title, @NotNull String description, ThemeElement icon, boolean alwaysShowDesc) {
        super(icon.size + theme.getBoldFont().getWidth(title) + 20, icon.size + 10);
        this.element = icon;
        this.title = title;
        this.description = description;
        this.alwaysShowDesc = alwaysShowDesc;
    }


    public OCButton(@NotNull String title, @NotNull String description, ThemeElement icon, boolean alwaysShowDesc, int width, int height) {
        super(width, height);
        this.element = icon;
        this.title = title;
        this.description = description;
        this.alwaysShowDesc = alwaysShowDesc;
    }

    public void draw(int x, int y) {
        super.update(x, y);

        percentHoveredRed = smooth(percentHoveredRed, elementColor.getRed() / 255f, hoverColor.getRed() / 255f);
        percentHoveredGreen = smooth(percentHoveredGreen, elementColor.getGreen() / 255f, hoverColor.getGreen() / 255f);
        percentHoveredBlue = smooth(percentHoveredBlue, elementColor.getBlue() / 255f, hoverColor.getBlue() / 255f);
        percentHoveredAlpha = smooth(percentHoveredAlpha, elementColor.getAlpha() / 255f, hoverColor.getAlpha() / 255f);
        if (!alwaysShowDesc) {
            percentDescription = Renderer.clamp(Renderer.easeOut(percentDescription, isHovered() ? 1f : 0f));
        }
        GlStateManager.color(percentHoveredRed, percentHoveredGreen, percentHoveredBlue, percentHoveredAlpha);
        if (isClicked()) {
            //Renderer.setGlColor(theme.getClickColor());
        }

        theme.getTextureManager().draw(ThemeElement.BUTTON, x, y, width, height);
        if (element != null) {
            GlStateManager.color(1f, 1f, 1f, isClicked() ? 0.6f : 1f);
            theme.getTextureManager().draw(element, x + 19, y + 8, element.size, element.size);
            if (title != null) {
                if (alwaysShowDesc) {
                    theme.getBoldFont().drawString(title, x + element.size + 25, y + 30, 1.2f, 1.2f, isClicked() ? theme.getTextColor().darker().getRGB() : theme.getTextColor().getRGB());
                    theme.getFont().drawString(description, x + element.size + 25, y + theme.getBoldFont().getHeight() + 37, 1.2f, 1.2f, isClicked() ? theme.getAccentTextColor().darker().getRGB() : theme.getAccentTextColor().getRGB());
                } else {
                    int titleY = y + 48;
                    titleY -= (int) (percentDescription * 18);
                    Color targetColor = theme.getAccentTextColor();
                    Color currentColor = isClicked() ? targetColor.darker() : new Color(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), (int) (targetColor.getAlpha() * percentDescription));
                    theme.getFont().drawString(description, x + element.size + 25, y + theme.getBoldFont().getHeight() + 37, 1.2f, 1.2f, currentColor.getRGB());
                    theme.getBoldFont().drawString(title, x + element.size + 25, titleY, 1.2f, 1.2f, isClicked() ? theme.getTextColor().darker().getRGB() : theme.getTextColor().getRGB());
                }
            }
        }
    }


    private float smooth(float current, float min, float max) {
        current = Renderer.easeOut(current, isHovered() ? 1f : 0f);
        if (current <= min) {
            current = min;
        }

        if (current >= max) {
            current = max;
        }
        return current;
    }

    public void onHover() {

    }
}
