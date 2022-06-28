package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.elements.BasicOption;

import java.util.ArrayList;

/**
 * Marks that the HUD element has options to add programmatically.
 */
public interface Conditional {
    void addNewOptions(String category, String subcategory, ArrayList<BasicOption> options);
}
