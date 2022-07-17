package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import org.jetbrains.annotations.NotNull;

public class BasicElement extends Element {
    protected final float radius;
    protected final boolean hoverFx;
    /**
     * The color animation used by this element.
     */
    protected ColorAnimation colorAnimation;


    public BasicElement(int width, int height, @NotNull ColorPalette colorPalette, boolean hoverFx) {
        this(width, height, colorPalette, hoverFx, 12f);
    }

    public BasicElement(int width, int height, @NotNull ColorPalette colorPalette, boolean hoverFx, float radius) {
        super(width, height, colorPalette);
        this.hoverFx = hoverFx;
        this.radius = radius;
        this.colorAnimation = new ColorAnimation(colorPalette);
    }

    public BasicElement(int width, int height, boolean hoverFx) {
        this(width, height, ColorPalette.TRANSPARENT, hoverFx, 12f);
    }



    @Override
    public void draw(long vg, float x, float y) {
        this.update(x, y);
        RenderManager.drawRoundedRect(vg, x, y, width, height, currentColor, radius);
    }


    @Override
    public void update(float x, float y) {
        if (disabled) {
            hovered = false;
            pressed = false;
            clicked = false;
        } else {
            hovered = InputUtils.isAreaHovered(x - hitBoxX, y - hitBoxY, width + hitBoxX, height + hitBoxY);
            pressed = hovered && Platform.getMousePlatform().isButtonDown(0);
            clicked = InputUtils.isClicked(block) && hovered;

            if (clicked) {
                toggled = !toggled;
                onClick();
            }
        }

        if (hoverFx) currentColor = colorAnimation.getColor(hovered, pressed);
        else currentColor = colorAnimation.getColor(false, false);
    }
    @Override
    public void setColorPalette(ColorPalette colorPalette) {
        if (this.colorPalette.equals(ColorPalette.TERTIARY) || this.colorPalette.equals(ColorPalette.TERTIARY_DESTRUCTIVE))
            this.colorAnimation.setColors(colorPalette.getNormalColorf());
        this.colorPalette = colorPalette;
        this.colorAnimation.setPalette(colorPalette);
    }


}
