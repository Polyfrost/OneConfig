package io.polyfrost.oneconfig.interfaces;

import java.lang.reflect.Field;

@SuppressWarnings({"unused"})
public abstract class Option {
    protected final Field field;
    protected final String name;
    protected final String description;

    /**
     * Initialize option
     *
     * @param field       variable attached to option (null for category)
     * @param name        name of option
     * @param description description of option
     */
    public Option(Field field, String name, String description) {
        this.field = field;
        this.name = name;
        this.description = description;
        if (field != null)
            field.setAccessible(true);
    }

    /**
     * @param object Java object to set the variable to
     */
    protected void set(Object object) throws IllegalAccessException {
        if (field == null) return;
        field.set(null, object);
    }

    /**
     * @return value of variable as Java object
     */
    protected Object get() throws IllegalAccessException {
        if (field == null) return null;
        return field.get(null);
    }

    /**
     * @return height of option to align other options accordingly
     */
    public abstract int getHeight();

    /**
     * Function that gets called when drawing option
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of menu
     * @param mouseX x position of mouse
     * @param mouseY y position of mouse
     */
    public abstract void draw(int x, int y, int width, int mouseX, int mouseY);

    /**
     * Function that gets called when mouse is clicked
     *
     * @param mouseX      x position of mouse
     * @param mouseY      y position of mouse
     * @param mouseButton button that got pressed
     */
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    /**
     * Function that gets called when a key is typed
     *
     * @param typedChar char that has been typed
     * @param keyCode   code of key
     */
    protected void keyTyped(char typedChar, int keyCode) {
    }
}
