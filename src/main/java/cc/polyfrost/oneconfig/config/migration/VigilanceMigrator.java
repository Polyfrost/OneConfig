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

import cc.polyfrost.oneconfig.config.core.OneColor;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VigilanceMigrator implements Migrator {
    private static final Pattern categoryPattern = Pattern.compile("\\[\"?(?<category>[^.\\[\\]\"]+)\"?(\\.\"?(?<subcategory>[^.\\[\\]\"]+)\"?)?]");
    private static final Pattern booleanPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = (?<value>true|false)");
    private static final Pattern numberPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = (?<value>[\\d.]+)");
    private static final Pattern stringPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = \"(?<value>.+)\"");
    private static final Pattern colorPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = \"(?<value>(\\d{1,3},){3}\\d{1,3})\"");
    protected final String filePath;
    protected HashMap<String, HashMap<String, HashMap<String, Object>>> values = null;
    protected final boolean fileExists;

    public VigilanceMigrator(String filePath) {
        this.filePath = filePath;
        this.fileExists = new File(filePath).exists();
    }

    @Override
    public Object getValue(Field field, @NotNull String name, @NotNull String category, @NotNull String subcategory) {
        if (!fileExists) return null;
        if (values == null) generateValues();
        if (field.isAnnotationPresent(VigilanceName.class)) {
            VigilanceName annotation = field.getAnnotation(VigilanceName.class);
            name = annotation.name();
            category = annotation.category();
            subcategory = annotation.subcategory();
        }
        name = parse(name);
        category = parse(category);
        subcategory = parse(subcategory);
        return values.getOrDefault(category, new HashMap<>()).getOrDefault(subcategory, new HashMap<>()).getOrDefault(name, null);
    }

    protected @NotNull String parse(@NotNull String value) {
        return value.toLowerCase().replace(" ", "_");
    }

    protected void generateValues() {
        if (values == null) values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentCategory = null;
            String currentSubcategory = null;
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher categoryMatcher = categoryPattern.matcher(line);
                if (categoryMatcher.find()) {
                    currentCategory = categoryMatcher.group("category");
                    currentSubcategory = categoryMatcher.group("subcategory");
                    if (!values.containsKey(currentCategory)) values.put(currentCategory, new HashMap<>());
                    if (!values.get(currentCategory).containsKey(currentSubcategory))
                        values.get(currentCategory).put(currentSubcategory, new HashMap<>());
                    continue;
                }
                if (currentCategory == null) continue;
                HashMap<String, Object> options = values.get(currentCategory).get(currentSubcategory);
                Matcher booleanMatcher = booleanPattern.matcher(line);
                if (booleanMatcher.find()) {
                    options.put(booleanMatcher.group("name"), Boolean.parseBoolean(booleanMatcher.group("value")));
                    continue;
                }
                Matcher numberMatcher = numberPattern.matcher(line);
                if (numberMatcher.find()) {
                    String value = numberMatcher.group("value");
                    if (value.contains(".")) options.put(numberMatcher.group("name"), Float.parseFloat(value));
                    else options.put(numberMatcher.group("name"), Integer.parseInt(value));
                    continue;
                }
                Matcher colorMatcher = colorPattern.matcher(line);
                if (colorMatcher.find()) {
                    String[] strings = colorMatcher.group("value").split(",");
                    int[] values = new int[4];
                    for (int i = 0; i < 4; i++) {
                        values[i] = Integer.parseInt(strings[i]);
                    }
                    options.put(colorMatcher.group("name"), new OneColor(values[0], values[1], values[2], values[3]));
                }
                Matcher stringMatcher = stringPattern.matcher(line);
                if (stringMatcher.find()) {
                    options.put(stringMatcher.group("name"), stringMatcher.group("value"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
