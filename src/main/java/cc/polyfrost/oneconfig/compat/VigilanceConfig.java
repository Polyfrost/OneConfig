package cc.polyfrost.oneconfig.compat;

import cc.polyfrost.oneconfig.config.core.ConfigCore;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.OptionCategory;
import cc.polyfrost.oneconfig.config.data.OptionPage;
import cc.polyfrost.oneconfig.config.data.OptionSubcategory;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.config.interfaces.Config;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.*;
import kotlin.reflect.KMutableProperty0;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;

public class VigilanceConfig extends Config {
    public final Vigilant vigilant;

    public VigilanceConfig(Mod modData, String configFile, Vigilant vigilant) {
        super(modData, configFile);
        this.vigilant = vigilant;
        init(modData);
    }

    @Override
    public void init(Mod mod) {
        if (vigilant != null) {
            mod.config = this;
            generateOptionsList(mod.defaultPage);
            ConfigCore.oneConfigMods.add(mod);
            this.mod = mod;
        }
    }

    @Override
    public void save() {
        vigilant.markDirty();
        vigilant.writeData();
    }

    @Override
    public void load() {
        //no-op
    }

    private void generateOptionsList(OptionPage page) {
        for (PropertyData option : ((VigilantAccessor) vigilant).getPropertyCollector().getProperties()) {
            PropertyAttributesExt attributes = option.getAttributesExt();
            if (!page.categories.containsKey(attributes.getCategory()))
                page.categories.put(attributes.getCategory(), new OptionCategory());
            OptionCategory category = page.categories.get(attributes.getCategory());
            if (category.subcategories.size() == 0 || !category.subcategories.get(category.subcategories.size() - 1).getName().equals(attributes.getSubcategory()))
                category.subcategories.add(new OptionSubcategory(attributes.getSubcategory()));
            ArrayList<BasicOption> options = category.subcategories.get(category.subcategories.size() - 1).options;
            switch (attributes.getType()) {
                case SWITCH:
                    options.add(new ConfigSwitch(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1));
                    break;
                case CHECKBOX:
                    options.add(new ConfigCheckbox(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1));
                    break;
                case PARAGRAPH:
                case TEXT:
                    options.add(new ConfigTextBox(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1, attributes.getPlaceholder(), attributes.getProtected(), attributes.getType() == PropertyType.PARAGRAPH));
                    break;
                case SELECTOR:
                    options.add(new ConfigUniSelector(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1, attributes.getOptions().toArray(new String[0])));
                    break;
                case PERCENT_SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1, 0, 1, 0));
                    break;
                case DECIMAL_SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1, attributes.getMinF(), attributes.getMaxF(), 0));
                    break;
                case SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1, attributes.getMin(), attributes.getMax(), 0));
                    break;
                case COLOR:
                    options.add(new ConfigColorElement(getFieldOfProperty(option), option.getInstance(), attributes.getName(), 1));
                    break;
                case BUTTON:
                    options.add(new ConfigButton(() -> ((CallablePropertyValue) option.getValue()).invoke(option.getInstance()), option.getInstance(), attributes.getName(), 1, attributes.getPlaceholder()));
                    break;
            }
            if (attributes.getType() == PropertyType.SWITCH || attributes.getType() == PropertyType.CHECKBOX) {
                optionNames.put(PropertyKt.fullPropertyPath(option.getAttributesExt()), options.get(options.size() - 1));
            }
        }
    }

    private Field getFieldOfProperty(PropertyData data) {
        if (data.getValue() instanceof FieldBackedPropertyValue) {
            FieldBackedPropertyValue fieldBackedPropertyValue = (FieldBackedPropertyValue) data.getValue();
            try {
                Field field = fieldBackedPropertyValue.getClass().getDeclaredField("field");
                field.setAccessible(true);
                return (Field) field.get(fieldBackedPropertyValue);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        } else if (data.getValue() instanceof ValueBackedPropertyValue) {
            ValueBackedPropertyValue valueBackedPropertyValue = (ValueBackedPropertyValue) data.getValue();
            try {
                Field field = valueBackedPropertyValue.getClass().getDeclaredField("obj");
                field.setAccessible(true);
                return (Field) field.get(valueBackedPropertyValue);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        } else if (data.getValue() instanceof KPropertyBackedPropertyValue) {
            KPropertyBackedPropertyValue kPropertyBackedPropertyValue = (KPropertyBackedPropertyValue) data.getValue();
            try {
                Field field = kPropertyBackedPropertyValue.getClass().getDeclaredField("property");
                field.setAccessible(true);
                KMutableProperty0 property = (KMutableProperty0) field.get(kPropertyBackedPropertyValue);
                return ReflectJvmMapping.getJavaField(property);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        } else throw new RuntimeException("Unknown property value type: " + data.getValue().getClass());
    }

    @SuppressWarnings("unused")
    public void addDependency(PropertyData property, PropertyData dependency) {
        BasicOption option = optionNames.get(PropertyKt.fullPropertyPath(property.getAttributesExt()));
        if (option != null) {
            option.setDependency(() -> Objects.equals(dependency.getValue().getValue(vigilant), true));
        }
    }
}
