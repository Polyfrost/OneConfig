package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;

public class BasicElement {
    protected int width, height;
    protected ColorPalette colorPalette;
    protected int hitBoxX, hitBoxY;
    protected boolean hoverFx;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean clicked = false;
    protected boolean toggled = false;
    protected boolean disabled = false;
    public int currentColor;
    protected final float radius;
    private boolean block = false;
    protected ColorAnimation colorAnimation;

    public BasicElement(int width, int height, @NotNull ColorPalette colorPalette, boolean hoverFx) {
        this(width, height, colorPalette, hoverFx, 12f);
    }

    public BasicElement(int width, int height, @NotNull ColorPalette colorPalette, boolean hoverFx, float radius) {
        this.height = height;
        this.width = width;
        this.colorPalette = colorPalette;
        this.hoverFx = hoverFx;
        this.radius = radius;
        this.colorAnimation = new ColorAnimation(colorPalette);
    }

    public BasicElement(int width, int height, boolean hoverFx) {
        this(width, height, ColorPalette.TRANSPARENT, hoverFx, 12f);
    }


    public void draw(long vg, int x, int y) {
        this.update(x, y);
        RenderManager.drawRoundedRect(vg, x, y, width, height, currentColor, radius);
    }

    public void update(int x, int y) {
        if (disabled) {
            hovered = false;
            pressed = false;
            clicked = false;
        } else {
            hovered = InputUtils.isAreaHovered(x - hitBoxX, y - hitBoxY, width + hitBoxX, height + hitBoxY);
            pressed = hovered && Mouse.isButtonDown(0);
            clicked = InputUtils.isClicked(block) && hovered;

            if (clicked) {
                toggled = !toggled;
                onClick();
            }
        }

        if (hoverFx) currentColor = colorAnimation.getColor(hovered, pressed);
        else currentColor = colorAnimation.getColor(false, false);
    }

    public void ignoreBlockedTouches(boolean state) {
        block = state;
    }


    public void onClick() {

    }

    public void setCustomHitbox(int x, int y) {
        hitBoxX = x;
        hitBoxY = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setColorPalette(ColorPalette colorPalette) {
        if (this.colorPalette.equals(ColorPalette.TERTIARY) || this.colorPalette.equals(ColorPalette.TERTIARY_DESTRUCTIVE))
            this.colorAnimation.setColors(colorPalette.getNormalColorf());
        this.colorPalette = colorPalette;
        this.colorAnimation.setPalette(colorPalette);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isClicked() {
        return clicked;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void disable(boolean state) {
        disabled = state;
    }
}
