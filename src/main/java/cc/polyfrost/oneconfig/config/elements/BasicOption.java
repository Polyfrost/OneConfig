package cc.polyfrost.oneconfig.config.elements;

import java.lang.reflect.Field;
import java.util.function.Supplier;

@SuppressWarnings({"unused"})
public abstract class BasicOption {
    public final int size;
    protected final Field field;
    protected final String name;
    protected final Object parent;
    private Supplier<Boolean> dependency;

    /**
     * Initialize option
     *
     * @param field  variable attached to option (null for category)
     * @param parent the parent object of the field, used for getting and setting the variable
     * @param name   name of option
     * @param size   size of option, 0 for single column, 1 for double.
     */
    public BasicOption(Field field, Object parent, String name, int size) {
        this.field = field;
        this.parent = parent;
        this.name = name;
        this.size = size;
        if (field != null) field.setAccessible(true);
    }

    /**
     * @param object Java object to set the variable to
     */
    protected void set(Object object) throws IllegalAccessException {
        if (field == null) return;
        field.set(parent, object);
    }

    /**
     * @return value of variable as Java object
     */
    protected Object get() throws IllegalAccessException {
        if (field == null) return null;
        return field.get(parent);
    }

    /**
     * @return height of option to align other options accordingly
     */
    public abstract int getHeight();

    /**
     * Function that gets called when drawing option
     *
     * @param vg NanoVG context
     * @param x  x position
     * @param y  y position
     */
    public abstract void draw(long vg, int x, int y);

    /**
     * Function that gets called last drawing option,
     * should be used for things that draw above other options
     *
     * @param vg NanoVG context
     * @param x  x position
     * @param y  y position
     */
    public void drawLast(long vg, int x, int y) {
    }

    /**
     * Function that gets called when a key is typed
     *
     * @param key     char that has been typed
     * @param keyCode code of key
     */
    public void keyTyped(char key, int keyCode) {
    }

    /**
     * @return If the component has an option to render at half size
     */
    public boolean hasHalfSize() {
        return true;
    }

    public String getName() {
        return name;
    }

    public void setDependency(Supplier<Boolean> supplier) {
        this.dependency = supplier;
    }

    protected boolean isEnabled() {
        return dependency == null || dependency.get();
    }
}
