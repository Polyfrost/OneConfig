/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.config.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Supplier;

@SuppressWarnings({"unused"})
public abstract class BasicOption {
    public final int size;
    protected final Field field;
    protected Object parent;
    public final String name;
    public final String category;
    public final String subcategory;
    private final ArrayList<Supplier<Boolean>> dependencies = new ArrayList<>();
    private final ArrayList<Runnable> listeners = new ArrayList<>();
    private final ArrayList<Supplier<Boolean>> hideConditions = new ArrayList<>();

    /**
     * Initialize option
     *
     * @param field  variable attached to option (null for category)
     * @param parent the parent object of the field, used for getting and setting the variable
     * @param name   name of option
     * @param size   size of option, 0 for single column, 1 for double.
     */
    public BasicOption(Field field, Object parent, String name, String category, String subcategory, int size) {
        this.field = field;
        this.parent = parent;
        this.name = name;
        this.size = size;
        this.category = category;
        this.subcategory = subcategory;
        if (field != null) field.setAccessible(true);
    }

    /**
     * @param object Java object to set the variable to
     */
    protected void set(Object object) throws IllegalAccessException {
        if (field == null) return;
        field.set(parent, object);
        for (Runnable listener : listeners) listener.run();
    }

    /**
     * @return value of variable as Java object
     */
    public Object get() throws IllegalAccessException {
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
     * Reset the field to its default value
     *
     * @param config The config the field is in
     */
    public void reset(Config config) {
        Object object = config.getDefault(field);
        if (object == null) return;
        try {
            set(object);
        } catch (IllegalAccessException ignored) {
        }
    }

    /**
     * @return If the option is enabled, based on the dependencies
     */
    public boolean isEnabled() {
        for (Supplier<Boolean> dependency : dependencies) {
            if (!dependency.get()) return false;
        }
        return true;
    }

    public boolean isHidden() {
        for (Supplier<Boolean> condition : hideConditions) {
            if (condition.get()) return true;
        }
        return false;
    }

    /**
     * Add a condition to this option
     *
     * @param supplier The dependency
     */
    public void addDependency(Supplier<Boolean> supplier) {
        this.dependencies.add(supplier);
    }

    /**
     * Add a listener to this option
     *
     * @param runnable The listener
     */
    public void addListener(Runnable runnable) {
        this.listeners.add(runnable);
    }

    /**
     * Hide an option if a condition is met
     *
     * @param supplier The condition
     */
    public void addHideCondition(Supplier<Boolean> supplier) {
        this.hideConditions.add(supplier);
    }

    /**
     * @return The field
     */
    public Field getField() {
        return field;
    }

    /**
     * @return The parent of the field
     */
    public Object getParent() {
        return parent;
    }

    /**
     * @param parent The new parent object
     */
    public void setParent(Object parent) {
        this.parent = parent;
    }
}
