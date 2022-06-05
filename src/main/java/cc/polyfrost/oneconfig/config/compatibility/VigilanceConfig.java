package cc.polyfrost.oneconfig.config.compatibility;

import cc.polyfrost.oneconfig.config.core.ConfigCore;
import cc.polyfrost.oneconfig.config.core.OneColor;
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
import net.minecraft.client.resources.I18n;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This class is used to convert the Vigilance config to the new config system.
 * It is not meant to be used outside the config system.
 */
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
            if (attributes.getHidden()) continue;
            if (!page.categories.containsKey(getCategory(attributes)))
                page.categories.put(getCategory(attributes), new OptionCategory());
            OptionCategory category = page.categories.get(getCategory(attributes));
            if (category.subcategories.size() == 0 || !category.subcategories.get(category.subcategories.size() - 1).getName().equals(getSubcategory(attributes)))
                category.subcategories.add(new OptionSubcategory(getSubcategory(attributes)));
            ArrayList<BasicOption> options = category.subcategories.get(category.subcategories.size() - 1).options;
            switch (attributes.getType()) {
                case SWITCH:
                    options.add(new ConfigSwitch(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2));
                    break;
                case CHECKBOX:
                    options.add(new ConfigCheckbox(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2));
                    break;
                case PARAGRAPH:
                case TEXT:
                    options.add(new ConfigTextBox(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2, attributes.getPlaceholder(), attributes.getProtected(), attributes.getType() == PropertyType.PARAGRAPH));
                    break;
                case SELECTOR:
                    options.add(new ConfigDropdown(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2, attributes.getOptions().toArray(new String[0])));
                    break;
                case PERCENT_SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2, 0, 1, 0));
                    break;
                case DECIMAL_SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2, attributes.getMinF(), attributes.getMaxF(), 0));
                    break;
                case SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2, attributes.getMin(), attributes.getMax(), 0));
                    break;
                case COLOR:
                    options.add(new CompatConfigColorElement(getFieldOfProperty(option), option.getInstance(), getName(attributes), 2));
                    break;
                case BUTTON:
                    options.add(new ConfigButton(() -> ((CallablePropertyValue) option.getValue()).invoke(option.getInstance()), option.getInstance(), getName(attributes), 2, attributes.getPlaceholder().isEmpty() ? "Activate" : attributes.getPlaceholder()));
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

    private String getName(PropertyAttributesExt ext) {
        try {
            PropertyAttributesExt.class.getDeclaredField("i18nName").setAccessible(true);
            return I18n.format((String) PropertyAttributesExt.class.getDeclaredField("i18nName").get(ext));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return ext.getName();
        }
    }

    private String getCategory(PropertyAttributesExt ext) {
        try {
            PropertyAttributesExt.class.getDeclaredField("i18nCategory").setAccessible(true);
            return I18n.format((String) PropertyAttributesExt.class.getDeclaredField("i18nCategory").get(ext));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return ext.getCategory();
        }
    }

    private String getSubcategory(PropertyAttributesExt ext) {
        try {
            PropertyAttributesExt.class.getDeclaredField("i18nSubcategory").setAccessible(true);
            return I18n.format((String) PropertyAttributesExt.class.getDeclaredField("i18nSubcategory").get(ext));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return ext.getSubcategory();
        }
    }

    @SuppressWarnings("unused")
    public void addDependency(PropertyData property, PropertyData dependency) {
        BasicOption option = optionNames.get(PropertyKt.fullPropertyPath(property.getAttributesExt()));
        if (option != null) {
            option.setDependency(() -> Objects.equals(dependency.getValue().getValue(vigilant), true));
        }
    }

    private static class CompatConfigColorElement extends ConfigColorElement {
        private final Field color;
        private Color prevColor = null;
        private OneColor cachedColor = null;

        public CompatConfigColorElement(Field color, Vigilant parent, String name, int size) {
            super(null, parent, name, size);
            this.color = color;
        }

        @Override
        protected Object get() throws IllegalAccessException {
            Color currentColor = (Color) color.get(parent);
            if (cachedColor == null || prevColor != color.get(parent)) {
                cachedColor = new OneColor(currentColor);
            }
            prevColor = currentColor;
            return cachedColor;
        }

        @Override
        protected void setColor(OneColor color) {
            if (cachedColor != color) {
                Color newColor = new Color(color.getRGB(), true);
                try {
                    this.color.set(parent, newColor);
                } catch (IllegalAccessException ignored) {

                }
            }
        }
    }
}
