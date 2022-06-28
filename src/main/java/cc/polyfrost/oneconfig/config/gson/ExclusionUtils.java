package cc.polyfrost.oneconfig.config.gson;

public class ExclusionUtils {
    protected static boolean isSuperClassOf(Class<?> clazz, Class<?> parentClass) {
        Class<?> tempClass = clazz;
        Class<?> lastClass;
        if (tempClass == parentClass) return true;
        while (true) {
            lastClass = tempClass;
            tempClass = tempClass.getSuperclass();
            if (tempClass == lastClass) return false;
            if (tempClass == parentClass) return true;
        }
    }
}
