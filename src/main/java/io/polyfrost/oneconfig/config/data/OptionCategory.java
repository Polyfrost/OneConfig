package io.polyfrost.oneconfig.config.data;

import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class OptionCategory {
    public final LinkedHashMap<String, ArrayList<BasicOption>> subcategories = new LinkedHashMap<>();
    public final ArrayList<ConfigPageButton> topPages = new ArrayList<>();
    public final ArrayList<ConfigPageButton> bottomPages = new ArrayList<>();
}
