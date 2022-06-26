package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.elements.BasicOption;

import java.util.ArrayList;

public interface Cacheable {
    void addCacheOptions(String category, String subcategory, ArrayList<BasicOption> options);
}
