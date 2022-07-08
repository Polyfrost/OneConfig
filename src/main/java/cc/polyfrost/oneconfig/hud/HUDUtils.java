package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.internal.hud.HudCore;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class HUDUtils {
    public static void addHudOptions(OptionPage page, Field field, Object instance, Config config) {
        HUD hudAnnotation = field.getAnnotation(HUD.class);
        Hud hud = (Hud) ConfigUtils.getField(field, instance);
        if (hud == null) return;
        hud.setConfig(config);
        HudCore.huds.add(hud);
        String category = hudAnnotation.category();
        String subcategory = hudAnnotation.subcategory();
        ArrayList<BasicOption> options = ConfigUtils.getSubCategory(page, hudAnnotation.category(), hudAnnotation.subcategory()).options;
        try {
            options.add(new ConfigHeader(field, hud, hudAnnotation.name(), category, subcategory, 2));
            options.add(new ConfigSwitch(hud.getClass().getField("enabled"), hud, "Enabled", category, subcategory, 2));
            options.addAll(ConfigUtils.getClassOptions(hud));
            if (hud instanceof BasicHud) {
                options.add(new ConfigCheckbox(hud.getClass().getField("rounded"), hud, "Rounded corners", category, subcategory, 1));
                options.get(options.size() - 1).addDependency(hud::isEnabled);
                options.add(new ConfigCheckbox(hud.getClass().getField("border"), hud, "Outline/border", category, subcategory, 1));
                options.get(options.size() - 1).addDependency(hud::isEnabled);
                options.add(new ConfigColorElement(hud.getClass().getField("bgColor"), hud, "Background color:", category, subcategory, 1, true));
                options.get(options.size() - 1).addDependency(hud::isEnabled);
                options.add(new ConfigColorElement(hud.getClass().getField("borderColor"), hud, "Border color:", category, subcategory, 1, true));
                options.get(options.size() - 1).addDependency(() -> hud.isEnabled() && ((BasicHud) hud).border);
                options.add(new ConfigSlider(hud.getClass().getField("cornerRadius"), hud, "Corner radius:", category, subcategory, 0, 10, 0));
                options.get(options.size() - 1).addDependency(() -> hud.isEnabled() && ((BasicHud) hud).rounded);
                options.add(new ConfigSlider(hud.getClass().getField("borderSize"), hud, "Border thickness:", category, subcategory, 0, 10, 0));
                options.get(options.size() - 1).addDependency(() -> hud.isEnabled() && ((BasicHud) hud).border);
            }
            options.add(new ConfigSlider(hud.getClass().getField("paddingX"), hud, "X-Padding", category, subcategory, 0, 50, 0));
            options.get(options.size() - 1).addDependency(hud::isEnabled);
            options.add(new ConfigSlider(hud.getClass().getField("paddingY"), hud, "Y-Padding", category, subcategory, 0, 50, 0));
            options.get(options.size() - 1).addDependency(hud::isEnabled);
        } catch (NoSuchFieldException ignored) {
        }
    }
}
