package io.polyfrost.oneconfig.config.data;

import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.config.ConfigPage;

import java.util.ArrayList;
import java.util.HashMap;

public class OptionCategory {
    public final HashMap<String, ArrayList<BasicOption>> options;
    public final ArrayList<ConfigPage> pages;

    public OptionCategory(HashMap<String, ArrayList<BasicOption>> options, ArrayList<ConfigPage> pages) {
        this.options = options;
        this.pages = pages;
    }
}
