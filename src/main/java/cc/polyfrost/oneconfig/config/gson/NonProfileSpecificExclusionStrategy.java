package cc.polyfrost.oneconfig.config.gson;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.NonProfileSpecific;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class NonProfileSpecificExclusionStrategy implements ExclusionStrategy {
    /**
     * @param f the field object that is under test
     * @return true if the field should be ignored; otherwise false
     */
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(NonProfileSpecific.class) == null || f.getAnnotation(Exclude.class) != null;
    }

    /**
     * @param clazz the class object that is under test
     * @return true if the class should be ignored; otherwise false
     */
    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return clazz.getAnnotation(Exclude.class) != null;
    }
}
