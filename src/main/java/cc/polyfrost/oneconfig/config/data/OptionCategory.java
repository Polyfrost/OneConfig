package cc.polyfrost.oneconfig.config.data;

import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class OptionCategory {
    public final LinkedHashMap<String, ArrayList<BasicOption>> subcategories = new LinkedHashMap<>();
    public final ArrayList<ConfigPageButton> topPages = new ArrayList<>();
    public final ArrayList<ConfigPageButton> bottomPages = new ArrayList<>();
}
