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

import org.polyfrost.oneconfig.api.config.data.Category;
import org.polyfrost.oneconfig.api.config.data.Mod;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.renderer.data.PolyImage;

public class Config {
    public final String id;
    public final Mod mod;
    public Config(String id, PolyImage icon, Translator.Text name, Category category) {
        this.id = id;
        mod = new Mod(icon, name, category, null);
    }

    public Config(String id, Translator.Text name, Category category) {
        this(id, null, name, category);
    }

    public Config(String id, String iconPath, String name, Category category) {
        this(id, new PolyImage(iconPath), new Translator.Text(name), category);
    }

    public Config(String id, String name, Category category) {
        this(id, null, new Translator.Text(name), category);
    }
}
