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

package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.gson.exclusion.ProfileExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class InternalConfig extends Config {
    transient protected final Gson gson = addGsonOptions(new GsonBuilder()
            .setExclusionStrategies(new InternalConfigExclusionStrategy()))
            .create();

    /**
     * @param title      title that is displayed
     * @param configFile file where config is stored
     */
    public InternalConfig(String title, String configFile) {
        super(new Mod(title, null), configFile);
    }

    @Override
    public void initialize() {
        if (new File("OneConfig/" + configFile).exists()) load();
        else save();
        generateOptionList(this, mod.defaultPage, mod, false);
        mod.config = this;
    }

    @Override
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get("OneConfig/" + configFile)), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load() {
        Path path = Paths.get("OneConfig/" + configFile);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
            gson.fromJson(reader, this.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            File file = path.toFile();
            file.renameTo(new File(file.getParentFile(), file.getName() + ".corrupted"));
        }
    }

    private static final class InternalConfigExclusionStrategy extends ProfileExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("hypixelKey") || super.shouldSkipField(f);
        }
    }
}
