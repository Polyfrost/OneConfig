package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.internal.hud.HudCore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class HUDUtils {
    public static void addHudOptions(OptionPage page, Field field, Object instance, Config config) {
        HUD hudAnnotation = field.getAnnotation(HUD.class);
        field.setAccessible(true);
        Hud hud = (Hud) ConfigUtils.getField(field, instance);
        if (hud == null) return;
        hud.setConfig(config);
        HudCore.huds.add(hud);
        String category = hudAnnotation.category();
        String subcategory = hudAnnotation.subcategory();
        ArrayList<BasicOption> options = new ArrayList<>();
        try {
            ArrayList<Field> fieldArrayList = ConfigUtils.getClassFields(hud.getClass());
            HashMap<String, Field> fields = new HashMap<>();
            for (Field f : fieldArrayList) fields.put(f.getName(), f);
            options.add(new ConfigHeader(field, hud, hudAnnotation.name(), category, subcategory, 2));
            options.add(new ConfigSwitch(fields.get("enabled"), hud, "Enabled", category, subcategory, 2));
            options.addAll(ConfigUtils.getClassOptions(hud));
            if (hud instanceof BasicHud) {
                options.add(new ConfigCheckbox(fields.get("rounded"), hud, "Rounded corners", category, subcategory, 1));
                options.add(new ConfigCheckbox(fields.get("border"), hud, "Outline/border", category, subcategory, 1));
                options.add(new ConfigColorElement(fields.get("bgColor"), hud, "Background color:", category, subcategory, 1, true));
                options.add(new ConfigColorElement(fields.get("borderColor"), hud, "Border color:", category, subcategory, 1, true));
                options.get(options.size() - 1).addDependency(() -> ((BasicHud) hud).border);
                options.add(new ConfigSlider(fields.get("cornerRadius"), hud, "Corner radius:", category, subcategory, 0, 10, 0));
                options.get(options.size() - 1).addDependency(() -> ((BasicHud) hud).rounded);
                options.add(new ConfigSlider(fields.get("borderSize"), hud, "Border thickness:", category, subcategory, 0, 10, 0));
                options.get(options.size() - 1).addDependency(() -> ((BasicHud) hud).border);
                options.add(new ConfigSlider(fields.get("paddingX"), hud, "X-Padding", category, subcategory, 0, 50, 0));
                options.add(new ConfigSlider(fields.get("paddingY"), hud, "Y-Padding", category, subcategory, 0, 50, 0));
            }
            for (BasicOption option : options) {
                if (option.name.equals("Enabled")) continue;
                option.addDependency(hud::isEnabled);
            }
        } catch (Exception ignored) {
        }
        ConfigUtils.getSubCategory(page, hudAnnotation.category(), hudAnnotation.subcategory()).options.addAll(options);
    }
}
