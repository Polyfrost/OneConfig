/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package cc.polyfrost.oneconfig.api.v1.config.processor.serializers;

import cc.polyfrost.oneconfig.api.v1.OneCollection;
import cc.polyfrost.oneconfig.api.v1.config.OneConfig;
import cc.polyfrost.oneconfig.api.v1.config.option.OptionHolder;
import cc.polyfrost.oneconfig.api.v1.config.property.Property;
import cc.polyfrost.oneconfig.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GsonSerializer implements Serializer {
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .setPrettyPrinting()
            .create();

    @Override
    public void serialize(OneConfig config, List<OptionHolder> options) {
        JsonObject json = parseJson(config);
        OneCollection<Property> properties = config.getProperties();
        boolean categoryBased = !properties.contains(Property.NO_CATEGORY_SERIALIZATION);
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
                if (properties.contains(Property.SERIALIZE_BASED_ON_NAME)) {
                    name = formatString(option.getDisplayName());
                }
                if (properties.contains(Property.SERIALIZE_BASED_ON_FIELD)) {
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
            Files.write(config.getPath(), GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deserialize(OneConfig config, List<OptionHolder> options) {
        JsonObject json = parseJson(config);
        OneCollection<Property> properties = config.getProperties();
        boolean categoryBased = !properties.contains(Property.NO_CATEGORY_SERIALIZATION);
        for (OptionHolder option : options) {
            if (option.getOptionType().serializable()) {
                JsonObject category = getCategoryJson(categoryBased, json, option.getDisplayCategory(), option.getDisplaySubcategory());
                String name = "";
                if (properties.contains(Property.SERIALIZE_BASED_ON_NAME)) {
                    name = formatString(option.getDisplayName());
                }
                if (properties.contains(Property.SERIALIZE_BASED_ON_FIELD)) {
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
        Path path = config.getPath();
        if (Files.exists(path)) {
            try {
                byte[] allBytes = Files.readAllBytes(path);
                String contents = new String(allBytes, StandardCharsets.UTF_8);
                JsonElement json = JsonUtils.parseString(contents, true);
                return Objects.requireNonNull(json).getAsJsonObject();
            } catch (Exception e) {
                e.printStackTrace();

                Path backupPath = path.resolveSibling(path.getFileName() + ".bak");

                try {
                    Files.move(path, backupPath);
                    Files.createFile(path);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return new JsonObject();
            }
        } else {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new JsonObject();
        }
    }
}
