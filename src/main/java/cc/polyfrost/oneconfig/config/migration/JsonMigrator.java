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

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <pre>{@code public class JsonMigrator implements Migrator}</pre>
 * <h2>JsonMigrator is a class that is used to migrate old configs using ANY .json system to the new OneConfig one.</h2>
 * It works using the {@link JsonName} annotation to specify a full, case sensitive, dot (or slash) path to the element; <br>or if the annotation isn't present, it will assume the target is in the 'master' object, and will use field.getName(). <br><br>
 * An easy way to get this path is to right-click on any json key in IntelliJ, and select "Copy JSON Pointer" or Copy Special > Copy Reference (Ctrl+Alt+Shift+C) <br><br>
 * <h2>Examples</h2>
 * Here is our example .json:
 * <pre>{@code
 * {
 *   "heartbeatTimeFactor": 100.321,
 *   "heartbeatVolume": 1.0,
 *   "hearts": {
 *     "disabled": false,
 *     "opacity": 1.0,
 *     "scale": 1.0,
 *     "animationSpeed": 0,
 *     "blur": {
 *       "disabled": false,
 *       "opacity": 0.97
 *     }
 *   }
 * }
 *  }</pre>
 * And here is some examples of how to fetch values, in a config that uses a JsonMigrator:
 * <pre>{@code
 * @JsonName("hearts.blur.disabled")                     // retrieve the value from the old JSON file
 * @Switch(name = "Disable Blur", category = "Hearts")   // initialize a field like a normal config
 * public static boolean isHeartsBlurDisabled = false;
 *
 * @JsonName("/hearts/blur/opacity")                     // retrieve the value from the old JSON file using slashes instead (either work!)
 * @Slider(name = "Hearts Opacity", category = "Hearts", min = 0f, max = 1f)   // initialize a field like a normal config
 * public static float isHeartsBlurDisabled = 1f;
 *
 * //@JsonName("heartbeatVolume")                   // This one does not need to have the annotation, as it is in the master object, and the variable name is the same.
 * @Slider(name = "Heartbeat Volume", category = "Hearts", min = 0f, max = 1f)
 * public static float heartbeatVolume = 0.5f;      // It's good practice to have it though.
 *  }</pre>
 */
public class JsonMigrator implements Migrator {

    protected JsonObject object;
    protected HashMap<String, Object> values = null;


    /**
     * <h2>{@link JsonMigrator Click ME} for the full javadoc on how to use JsonMigrator!</h2>
     * Construct a new JsonMigrator for this config file.
     *
     * @param filePath the full path to where the previous config file should be located.
     */
    public JsonMigrator(String filePath) {
        File file = new File(filePath);
        try {
            object = new JsonParser().parse(new FileReader(file)).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            object = null;
        }
    }

    /**
     * @param field       The target field of the option
     * @param name        <b>IGNORED!</b> Uses field.getName() if the annotation is not present.
     * @param category    <b>IGNORED!</b>
     * @param subcategory <b>IGNORED!</b>
     */
    @Override
    public Object getValue(Field field, @Nullable String name, @Nullable String category, @Nullable String subcategory) {
        if (object == null) return null;
        if (values == null) generateValues();
        String key;
        if (field.isAnnotationPresent(JsonName.class)) {
            JsonName annotation = field.getAnnotation(JsonName.class);
            key = annotation.value();
        } else key = field.getName();
        return values.get(parse(key));
    }

    protected String parse(@NotNull String value) {
        if (value.startsWith("/") || value.startsWith(".")) value = value.substring(1);
        return value.replaceAll("/", ".");
    }

    protected void generateValues() {
        if (object == null) return;
        values = new HashMap<>();
        for (Map.Entry<String, JsonElement> master : object.entrySet()) {
            loopThroughChildren(master.getKey(), master.getValue().getAsJsonObject());
        }
    }

    protected void loopThroughChildren(String path, JsonObject in) {
        for (Map.Entry<String, JsonElement> element : in.entrySet()) {
            String thisPath = path + "." + element.getKey();
            if (element.getValue().isJsonObject()) {
                loopThroughChildren(thisPath, element.getValue().getAsJsonObject());
            } else put(thisPath, element.getValue());
        }
    }

    /**
     * Take the JsonElement and add it as the correct type to the hashmap.
     *
     * @param key "." delimited key
     * @param val value to be parsed
     */
    protected void put(String key, JsonElement val) {
        if (val.isJsonNull()) values.put(key, null);
        else if (val.isJsonPrimitive()) values.put(key, cast(val.getAsJsonPrimitive()));
        else if (val.isJsonArray()) {
            JsonArray array = val.getAsJsonArray();
            Iterator<JsonElement> iterator = array.iterator();
            Object[] objects = new Object[array.size()];
            int i = 0;
            while (iterator.hasNext()) {
                objects[i] = cast(iterator.next().getAsJsonPrimitive());
            }
            values.put(key, objects);
        } else values.put(key, val);
    }

    /**
     * Cast the given JsonPrimitive to an appropriate number, boolean, or String.
     *
     * @param primitive the json primitive
     * @return the value in the correct type.
     */
    private Object cast(JsonPrimitive primitive) {
        if (primitive.isJsonNull()) return null;
        else if (primitive.isBoolean()) return primitive.getAsBoolean();
        else if (primitive.isNumber()) {
            Number number = primitive.getAsNumber();
            if (number.floatValue() % 1f != 0) {
                return number.floatValue();
            } else return number.intValue();
        } else
            return primitive.getAsString();                             // if is not boolean, null or number return as String
    }
}
