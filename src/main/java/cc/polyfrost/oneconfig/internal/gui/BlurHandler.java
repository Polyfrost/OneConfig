package cc.polyfrost.oneconfig.internal.gui;

import java.util.ServiceLoader;

public interface BlurHandler {
    BlurHandler INSTANCE = ServiceLoader.load(BlurHandler.class, BlurHandler.class.getClassLoader()).iterator().next();
    void reloadBlur(Object screen);
}
