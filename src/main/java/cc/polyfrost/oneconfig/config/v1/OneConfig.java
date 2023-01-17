/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.config.v1;

import cc.polyfrost.oneconfig.config.v1.categories.Category;
import cc.polyfrost.oneconfig.config.v1.options.OptionHolder;
import cc.polyfrost.oneconfig.config.v1.processor.OptionProcessor;
import cc.polyfrost.oneconfig.config.v1.properties.Properties;
import cc.polyfrost.oneconfig.config.v1.properties.Property;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

/**
 * The base class for all OneConfig configs.
 * <p>
 * This class is responsible for holding everything that is needed to create a config,
 * including the {@link Properties}, {@link OptionProcessor}, and a list of {@link OptionHolder}s.
 * </p>
 * <p>
 * Register all your options like so:
 * <pre>
 *         {@code
 *         public class MyConfig extends OneConfig {
 *
 *             @Switch(
 *                 name = "Test"
 *             )
 *             public boolean test = true;
 *
 *             public MyConfig() {
 *                 super("Config", new File("./config.json"), Category.UTIL_QOL);
 *                 init();
 *             }
 *         }
 *         }
 *    </pre>
 */
public class OneConfig {
    private final String name;
    private final File file;
    private final Category[] categories;
    private final Properties properties;
    private final OptionProcessor optionProcessor = new OptionProcessor();
    private List<OptionHolder> optionHolders;

    public OneConfig(String name, File file, Properties properties, Category... categories) {
        this.name = name;
        this.file = file;
        this.properties = properties;
        this.categories = categories;
        if (!properties.containsProperty(Property.SERIALIZE_BASED_ON_NAME) && !properties.containsProperty(Property.SERIALIZE_BASED_ON_FIELD)) {
            properties.addProperty(Property.SERIALIZE_BASED_ON_FIELD);
        }
        if (!properties.containsProperty(Property.GSON_SERIALIZATION)) {
            properties.addProperty(Property.GSON_SERIALIZATION);
        }
    }

    public OneConfig(String name, File file, Category... categories) {
        this(name, file, new Properties(), categories);
    }

    /**
     * Initializes the config by collecting all the options and serializing them via {@link OneConfig#save()}.
     */
    public void init() {
        optionHolders = optionProcessor.collect(this);
        save();
    }

    /**
     * Serializes the config to the file specified in the constructor.
     * @see OptionProcessor#serialize(OneConfig, List) on how the serialization works.
     */
    public void save() {
        optionProcessor.serialize(this, optionHolders);
    }

    /**
     * Deserializes the config from the file specified in the constructor.
     * @see OptionProcessor#deserialize(OneConfig, List) on how the deserialization works.
     */
    public void load() {
        optionProcessor.deserialize(this, optionHolders);
    }

    /**
     * Opens the GUI of the config.
     */
    public void openScreen() {

    }

    /**
     * Manually registers an option holder.
     * @param option The option holder to register.
     * @see OptionHolder
     */
    protected final void registerOption(OptionHolder option) {
        optionHolders.add(option);
    }

    /**
     * Make an option depends on the provided condition.
     * @param option The option to make depend on the condition.
     * @param conditionName The name of the condition. Used in the GUI.
     * @param condition The condition to check.
     */
    protected final void addDependency(String option, String conditionName, Supplier<Boolean> condition) {

    }

    /**
     * Make an option depends on the provided dependency.
     * @param option The option to make depend on the dependency.
     * @param dependentOption The option to depend on.
     */
    protected final void addDependency(String option, String dependentOption) {

    }

    /**
     * Hide an option from the GUI if the provided condition is true.
     * @param option The option to hide.
     * @param condition The condition to check.
     */
    protected final void hideIf(String option, Supplier<Boolean> condition) {

    }

    /**
     * Hide an option from the GUI if the provided dependency is true.
     * @param option The option to hide.
     * @param dependentOption The option to depend on.
     */
    protected final void hideIf(String option, String dependentOption) {

    }

    /**
     * Runs the provided runnable if the provided variable is changed via the GUI.
     * @param name The name of the variable.
     * @param callback The runnable to run.
     */
    protected final void listen(String name, Runnable callback) {

    }

    /**
     * Returns the file of the config.
     * @return The file of the config.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns all the properties of the config.
     * @return All the properties of the config.
     * @see Properties
     */
    public Properties getProperties() {
        return properties;
    }
}
