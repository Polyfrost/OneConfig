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

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.renderer.data.PolyImage;

import java.util.ArrayList;
import java.util.function.Supplier;

public class Config {
    public transient final String id;
    public transient final PolyImage icon;
    public transient final Translator.Text name;
    public boolean enabled = true;
    public boolean favorite = false;
    // todo(?) public transient boolean hasUpdate = true;
    @ApiStatus.Internal
    public transient final ArrayList<PolyImage> data = new ArrayList<>(3);
    public transient final Category category;
    @ApiStatus.Internal
    public transient final Tree tree;

    public Config(@NotNull String id, @Nullable PolyImage icon, @NotNull Translator.Text name, Category category) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.category = category;
        this.tree = ConfigManager.INSTANCE.register(this);
    }

    public Config(String id, Translator.Text name, Category category) {
        this(id, null, name, category);
    }

    public Config(String id, String iconPath, String name, Category category) {
        this(id, iconPath == null ? null : new PolyImage(iconPath), new Translator.Text(name), category);
    }

    public Config(String id, String name, Category category) {
        this(id, null, new Translator.Text(name), category);
    }


    public void addDependency(String option, Supplier<Boolean> condition) {
        Property<?> p = tree.get(option);
        if(p == null) throw new IllegalArgumentException("Attempted to specify a condition for property " + option + " but it was not found");
        p.addDisplayCondition(condition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return id.equals(config.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public enum Category {
        HUD("hud.svg", "oneconfig.hud"),
        COMBAT("console.svg", "oneconfig.combat"),
        QOL("spanner.svg", "oneconfig.qol"),
        HYPIXEL("hypixel.svg", "oneconfig.hypixel"),
        OTHER(null, "oneconfig.other");

        public final String name;
        public final String iconPath;
        Category(String iconPath, String name) {
            this.iconPath = iconPath;
            this.name = name;
        }
    }
}
