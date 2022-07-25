package cc.polyfrost.oneconfig.config.gson;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.NonProfileSpecific;
import cc.polyfrost.oneconfig.gui.pages.Page;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class ProfileExclusionStrategy extends ExclusionUtils implements ExclusionStrategy {
    /**
     * @param f the field object that is under test
     * @return true if the field should be ignored; otherwise false
     */
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        if (isSuperClassOf(f.getDeclaredClass(), Config.class)) return true;
        if (isSuperClassOf(f.getDeclaredClass(), Page.class)) return true;
        if (f.getDeclaredClass().isAssignableFrom(Runnable.class)) return true;
        if (f.getAnnotation(NonProfileSpecific.class) != null) return true;
        Exclude exclude = f.getAnnotation(Exclude.class);
        return exclude != null;
    }

    /**
     * @param clazz the class object that is under test
     * @return true if the class should be ignored; otherwise false
     */
    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        Exclude exclude = clazz.getAnnotation(Exclude.class);
        return exclude != null;
    }
}
