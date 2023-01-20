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

package cc.polyfrost.oneconfig.config.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CfgMigrator implements Migrator {
    final String filePath;
    final boolean fileExists;
    HashMap<String, HashMap<String, Object>> values;
    final Pattern stringOrFloatPattern = Pattern.compile("S:(?<name>\\S+)=(?<value>\\S+)");
    final Pattern intPattern = Pattern.compile("I:(?<name>\\S+)=(?<value>\\S+)");
    final Pattern doublePattern = Pattern.compile("D:(?<name>\\S+)=(?<value>\\S+)");
    final Pattern booleanPattern = Pattern.compile("B:(?<name>\\S+)=(?<value>\\S+)");
    final Pattern listPattern = Pattern.compile("S:(?<name>\\S+)\\s<");
    final Pattern categoryPattern = Pattern.compile("(?<name>\\S+)\\s\\Q{");

    public CfgMigrator(String filePath) {
        this.filePath = filePath;
        this.fileExists = new File(filePath).exists();
    }


    /**
     * Get the value from its name, category, and subcategory. The target field is also supplied, which can be used to check for {@link VigilanceName}.
     * The returned Object is intended to be a "Duck" object, and should be cast to the correct type. The Migrator should never return ClassCastExceptions.
     * <br><b>NOTE: .cfg files DO NOT support subcategories! The implementation of this is:</b> <br>
     * <pre>{@code // if a category and a subcategory is supplied, only the category is used. else:
     * if(category == null && subcategory != null) category = subcategory;}</pre>
     *
     * @param subcategory The subcategory of the option (not supported!)
     */
    @Nullable
    @Override
    public Object getValue(Field field, @NotNull String name, @Nullable String category, @Nullable String subcategory) {
        if (!fileExists) return null;
        if (values == null) generateValues();
        if (field.isAnnotationPresent(CfgName.class)) {
            CfgName annotation = field.getAnnotation(CfgName.class);
            name = annotation.name();
            category = annotation.category();
        }
        name = parse(name);
        if (category == null) {
            if (subcategory != null) {
                category = parse(subcategory);
            }
        } else category = parse(category);
        return values.getOrDefault(category, new HashMap<>()).getOrDefault(name, null);
    }

    protected void generateValues() {
        if (values == null) values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentCategory = null;
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher categoryMatcher = categoryPattern.matcher(line);
                if (categoryMatcher.find()) {
                    currentCategory = categoryMatcher.group("name");
                    if (!values.containsKey(currentCategory)) values.put(currentCategory, new HashMap<>());
                }
                if (currentCategory == null) continue;
                if (line.contains("\"")) {
                    line = line.replaceAll("\"", "").replaceAll(" ", "");
                }
                Matcher booleanMatcher = booleanPattern.matcher(line);
                if (booleanMatcher.find()) {
                    values.get(currentCategory).put(booleanMatcher.group("name"), Boolean.parseBoolean(booleanMatcher.group("value")));
                    continue;
                }
                Matcher intMatcher = intPattern.matcher(line);
                if (intMatcher.find()) {
                    values.get(currentCategory).put(intMatcher.group("name"), Integer.parseInt(intMatcher.group("value")));
                    continue;
                }
                Matcher doubleMatcher = doublePattern.matcher(line);
                if (doubleMatcher.find()) {
                    values.get(currentCategory).put(doubleMatcher.group("name"), Double.parseDouble(doubleMatcher.group("value")));
                    continue;
                }
                Matcher stringOrFloatMatcher = stringOrFloatPattern.matcher(line.trim());
                if (stringOrFloatMatcher.matches()) {
                    if (line.contains(".")) {
                        try {
                            values.get(currentCategory).put(stringOrFloatMatcher.group("name"), Float.parseFloat(stringOrFloatMatcher.group("value")));
                        } catch (Exception ignored) {
                            values.get(currentCategory).put(stringOrFloatMatcher.group("name"), stringOrFloatMatcher.group("value"));
                        }
                    }
                    values.get(currentCategory).put(stringOrFloatMatcher.group("name"), stringOrFloatMatcher.group("value"));
                    continue;
                }
                Matcher listMatcher = listPattern.matcher(line.trim());
                if (listMatcher.matches()) {
                    String name = listMatcher.group("name");
                    ArrayList<String> list = new ArrayList<>();
                    while ((line = reader.readLine()) != null && !line.contains(">")) {
                        list.add(line.trim());
                    }
                    String[] array = new String[list.size()];
                    // type casting doesn't work, so you have to do this
                    list.toArray(array);
                    values.get(currentCategory).put(name, array);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    protected String parse(@NotNull String value) {
        if (value.contains("\"")) {
            return value.replaceAll("\"", "").replaceAll(" ", "");
        } else return value;
    }
}
