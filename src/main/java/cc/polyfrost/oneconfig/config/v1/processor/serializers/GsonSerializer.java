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

package cc.polyfrost.oneconfig.config.v1.processor.serializers;

import cc.polyfrost.oneconfig.config.v1.OneConfig;
import cc.polyfrost.oneconfig.config.v1.options.OptionHolder;
import cc.polyfrost.oneconfig.config.v1.properties.Property;
import cc.polyfrost.oneconfig.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class GsonSerializer implements Serializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void serialize(OneConfig config, List<OptionHolder> options) {
        JsonObject json = parseJson(config);
        boolean categoryBased = !config.getProperties().containsProperty(Property.NO_CATEGORY_SERIALIZATION);
        for (OptionHolder option : options) {
            if (option.getOptionType().serializable()) {
                JsonObject category = getCategoryJson(categoryBased, json, option.getDisplayCategory(), option.getDisplaySubcategory());
                if (option.isAccordion()) {
                    if (category.has(formatString(option.getAccordion().name()))) {
                        category = category.getAsJsonObject(formatString(option.getAccordion().name()));
                    } else {
                        JsonObject accordion = new JsonObject();
                        category.add(formatString(option.getAccordion().name()), accordion);
                        category = accordion;
                    }
                }
                String name = "";
                if (config.getProperties().containsProperty(Property.SERIALIZE_BASED_ON_NAME)) {
                    name = formatString(option.getDisplayName());
                }
                if (config.getProperties().containsProperty(Property.SERIALIZE_BASED_ON_FIELD)) {
                    if (!name.isEmpty()) {
                        name += "-";
                    }
                    name += option.getJavaName();
                }
                category.add(name, JsonUtils.parseString(GSON.toJson(option.invoke())));
                if (categoryBased) {
                    json.add(formatString(option.getDisplayCategory()), json.get(formatString(option.getDisplayCategory())));
                }
            }
        }
        try {
            FileUtils.writeStringToFile(config.getFile(), GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deserialize(OneConfig config, List<OptionHolder> options) {
        JsonObject json = parseJson(config);
        boolean categoryBased = !config.getProperties().containsProperty(Property.NO_CATEGORY_SERIALIZATION);
        for (OptionHolder option : options) {
            if (option.getOptionType().serializable()) {
                JsonObject category = getCategoryJson(categoryBased, json, option.getDisplayCategory(), option.getDisplaySubcategory());
                String name = "";
                if (config.getProperties().containsProperty(Property.SERIALIZE_BASED_ON_NAME)) {
                    name = formatString(option.getDisplayName());
                }
                if (config.getProperties().containsProperty(Property.SERIALIZE_BASED_ON_FIELD)) {
                    name += option.getJavaName();
                }
                if (category.has(name)) {
                    option.set(GSON.fromJson(category.get(name), option.getJavaClass()));
                }
            }
        }
    }

    private String formatString(String string) {
        if (string == null) {
            return "";
        }
        return StringUtils.replace(string.toLowerCase(Locale.ENGLISH), " ", "_");
    }

    private JsonObject getCategoryJson(boolean categoryBased, JsonObject json, String category, String subCategory) {
        if (categoryBased) {
            try {
                if (json.has(formatString(category))) {
                    JsonObject categoryJson = json.getAsJsonObject(formatString(category));
                    if (subCategory.isEmpty()) {
                        return categoryJson;
                    } else {
                        System.out.println("Subcategory: " + subCategory);
                        try {
                            if (categoryJson.has(formatString(subCategory))) {
                                return categoryJson.getAsJsonObject(formatString(subCategory));
                            } else {
                                JsonObject subCategoryJson = new JsonObject();
                                categoryJson.add(formatString(subCategory), subCategoryJson);
                                return subCategoryJson;
                            }
                        } catch (Exception e) {
                            JsonObject subCategoryJson = new JsonObject();
                            categoryJson.add(formatString(subCategory), subCategoryJson);
                            return subCategoryJson;
                        }
                    }
                } else {
                    JsonObject newCategory = new JsonObject();
                    json.add(formatString(category), newCategory);
                    return getCategoryJson(true, json, category, subCategory);
                }
            } catch (Exception e) {
                JsonObject newCategory = new JsonObject();
                json.add(formatString(category), newCategory);
                return getCategoryJson(true, json, category, subCategory);
            }
        } else {
            return json;
        }
    }

    private JsonObject parseJson(OneConfig config) {
        if (config.getFile().exists()) {
            try {
                return JsonUtils.parseString(FileUtils.readFileToString(config.getFile(), StandardCharsets.UTF_8), false).getAsJsonObject();
            } catch (Exception e) {
                e.printStackTrace();
                config.getFile().renameTo(new File(config.getFile().getAbsolutePath() + ".backup"));
                try {
                    config.getFile().createNewFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return new JsonObject();
            }
        } else {
            try {
                config.getFile().createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new JsonObject();
        }
    }
}
