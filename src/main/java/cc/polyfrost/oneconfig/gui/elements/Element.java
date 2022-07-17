package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

public abstract class Element {
    protected int width, height;
    /**
     * The color palette used for this element.
     */
    protected ColorPalette colorPalette;
    /**
     * hitBoxX and hitBoxY are integer variables to determine (in pixels) how far past the boundaries of this button it is still able to be interacted with.
     */
    protected int hitBoxX, hitBoxY;
    /**
     * Whether the element is currently being hovered over
     */
    protected boolean hovered = false;
    /**
     * Whether the mouse is actively being held down on the element.
     */
    protected boolean pressed = false;
    /**
     * Whether the element is clicked.
     */
    protected boolean clicked = false;
    /**
     * The toggle state of the button. Its false, then if it is clicked, it becomes true, and if clicked again, it becomes false.
     */
    protected boolean toggled = false;
    /**
     * Whether the element is currently disabled.
     */
    protected boolean disabled = false;
    /**
     * The ARGB color of this element.
     */
    public int currentColor;
    /**
     * Boolean to determine if this element is allowed to be clicked when {@link InputUtils#isBlockingInput()} is true.
     */
    protected boolean block = false;

    public Element(int width, int height, ColorPalette palette) {
        this.width = width;
        this.height = height;
        this.colorPalette = palette;
    }

    public Element(int width, int height) {
        this(width, height, ColorPalette.TRANSPARENT);
    }

    /**
     * Draw script for the element.
     * <br> <b>Make sure to call {@link #update(float, float)} to update the elements states!</b>
     *
     * @param vg NanoVG context (see {@link RenderManager})
     * @param x  x position of the element
     * @param y  y position of the element
     */
    public abstract void draw(long vg, float x, float y);

    /**
     * Update this element's clicked, hovered, toggled, and pressed states, invoke any necessary methods, and update the color animation.
     */
    public abstract void update(float x, float y);


    public void onClick() {

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

    public void ignoreBlockedTouches(boolean state) {
        block = state;
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
        this.colorPalette = colorPalette;
    }

}
