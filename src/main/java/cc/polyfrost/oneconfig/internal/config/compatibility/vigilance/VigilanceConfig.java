package cc.polyfrost.oneconfig.internal.config.compatibility.vigilance;

import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.platform.Platform;
import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.*;
import kotlin.reflect.KMutableProperty0;
import kotlin.reflect.jvm.ReflectJvmMapping;

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
        initialize();
    }

    @Override
    public void initialize() {
        if (vigilant != null) {
            mod.config = this;
            generateOptionsList(mod.defaultPage);
            ConfigCore.mods.add(mod);
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
            ArrayList<BasicOption> options = ConfigUtils.getSubCategory(page, getCategory(attributes), getSubcategory(attributes)).options;
            switch (attributes.getType()) {
                case SWITCH:
                    options.add(new ConfigSwitch(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), 2));
                    break;
                case CHECKBOX:
                    options.add(new ConfigCheckbox(getFieldOfProperty(option), option.getInstance(), getName(attributes),getCategory(attributes), getSubcategory(attributes), 2));
                    break;
                case PARAGRAPH:
                case TEXT:
                    options.add(new ConfigTextBox(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), 2, attributes.getPlaceholder(), attributes.getProtected(), attributes.getType() == PropertyType.PARAGRAPH));
                    break;
                case SELECTOR:
                    options.add(new ConfigDropdown(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), 2, attributes.getOptions().toArray(new String[0])));
                    break;
                case PERCENT_SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), 0, 1, 0));
                    break;
                case DECIMAL_SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), attributes.getMinF(), attributes.getMaxF(), 0));
                    break;
                case NUMBER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), attributes.getMin(), attributes.getMax(), 1));
                    break;
                case SLIDER:
                    options.add(new ConfigSlider(getFieldOfProperty(option), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), attributes.getMin(), attributes.getMax(), 0));
                    break;
                case COLOR:
                    options.add(new CompatConfigColorElement(getFieldOfProperty(option), option.getInstance(), getCategory(attributes), getSubcategory(attributes), getName(attributes), 2));
                    break;
                case BUTTON:
                    options.add(new ConfigButton(() -> ((CallablePropertyValue) option.getValue()).invoke(option.getInstance()), option.getInstance(), getName(attributes), getCategory(attributes), getSubcategory(attributes), 2, attributes.getPlaceholder().isEmpty() ? "Activate" : attributes.getPlaceholder()));
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
                return field;
            } catch (NoSuchFieldException e) {
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
            return Platform.getI18nPlatform().format((String) PropertyAttributesExt.class.getDeclaredField("i18nName").get(ext));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return ext.getName();
        }
    }

    private String getCategory(PropertyAttributesExt ext) {
        try {
            PropertyAttributesExt.class.getDeclaredField("i18nCategory").setAccessible(true);
            return Platform.getI18nPlatform().format((String) PropertyAttributesExt.class.getDeclaredField("i18nCategory").get(ext));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return ext.getCategory();
        }
    }

    private String getSubcategory(PropertyAttributesExt ext) {
        try {
            PropertyAttributesExt.class.getDeclaredField("i18nSubcategory").setAccessible(true);
            return Platform.getI18nPlatform().format((String) PropertyAttributesExt.class.getDeclaredField("i18nSubcategory").get(ext));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return ext.getSubcategory();
        }
    }

    @SuppressWarnings("unused")
    public void addDependency(PropertyData property, PropertyData dependency) {
        BasicOption option = optionNames.get(PropertyKt.fullPropertyPath(property.getAttributesExt()));
        if (option != null) {
            option.addDependency(() -> Objects.equals(dependency.getValue().getValue(vigilant), true));
        }
    }

    private static class CompatConfigColorElement extends ConfigColorElement {
        private final Field color;
        private Color prevColor = null;
        private OneColor cachedColor = null;

        public CompatConfigColorElement(Field color, Vigilant parent, String name, String category, String subcategory, int size) {
            super(null, parent, name, category, subcategory, size, true);
            this.color = color;
        }

        @Override
        public Object get() throws IllegalAccessException {
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
