package io.polyfrost.oneconfig.config.data;

import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;

import java.util.ArrayList;
import java.util.HashMap;

public class OptionCategory {
    public final HashMap<String, ArrayList<BasicOption>> subcategories = new HashMap<>();
    public final ArrayList<ConfigPageButton> topPages = new ArrayList<>();
    public final ArrayList<ConfigPageButton> bottomPages = new ArrayList<>();
}
