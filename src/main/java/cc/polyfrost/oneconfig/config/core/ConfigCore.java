package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.config.annotations.Option;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.hud.HudCore;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ConfigCore {
    public static ArrayList<Mod> oneConfigMods = new ArrayList<>();

    public static void saveAll() {
        for (Mod modData : oneConfigMods) {
            modData.config.save();
        }
    }

    public static void reInitAll() {
        ArrayList<Mod> data = new ArrayList<>(oneConfigMods);
        oneConfigMods.clear();
        HudCore.huds.clear();
        for (Mod modData : data) {
            modData.config.init(modData);
        }
    }

    public static BasicOption getOption(Option option, Field field, Object instance) {
        switch (option.type()) {
            case SWITCH:
                return new ConfigSwitch(field, instance, option.name(), option.size());
            case CHECKBOX:
                return new ConfigCheckbox(field, instance, option.name(), option.size());
            case TEXT:
                return new ConfigTextBox(field, instance, option.name(), option.size(), option.placeholder(), option.secure(), option.multiLine());
            case DUAL_OPTION:
                return new ConfigDualOption(field, instance, option.name(), option.size(), option.options());
            case DROPDOWN:
                return new ConfigDropdown(field, instance, option.name(), option.size(), option.options());
            case SLIDER:
                return new ConfigSlider(field, instance, option.name(), option.size(), option.min(), option.max(), option.step());
            case INFO:
                return new ConfigInfo(field, instance, option.name(), option.size(), option.infoType());
            case COLOR:
                return new ConfigColorElement(field, instance, option.name(), option.size());
            case HEADER:
                return new ConfigHeader(field, instance, option.name(), option.size());
            case BUTTON:
                return new ConfigButton(field, instance, option.name(), option.size(), option.buttonText());
            case KEYBIND:
                return new ConfigKeyBind(field, instance, option.name(), option.size());
        }
        return null;
    }

    public static ArrayList<BasicOption> getClassOptions(Object hud) {
        ArrayList<BasicOption> options = new ArrayList<>();
        for (Field field : hud.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Option.class)) continue;
            Option option = field.getAnnotation(Option.class);
            options.add(getOption(option, field, hud));
        }
        return options;
    }
}
