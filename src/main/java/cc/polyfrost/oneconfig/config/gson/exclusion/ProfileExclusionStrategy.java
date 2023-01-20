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

package cc.polyfrost.oneconfig.config.gson.exclusion;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.HypixelKey;
import cc.polyfrost.oneconfig.config.annotations.NonProfileSpecific;
import cc.polyfrost.oneconfig.gui.pages.Page;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class ProfileExclusionStrategy extends ExclusionUtils implements ExclusionStrategy {
    /**
     * @param f the field object that is under test
     * @return true if the field should be ignored; otherwise false
     */
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        if (isSuperClassOf(f.getDeclaredClass(), Config.class)) return true;
        if (isSuperClassOf(f.getDeclaredClass(), Page.class)) return true;
        if (f.getDeclaredClass().isAssignableFrom(Runnable.class)) return true;
        if (f.getAnnotation(NonProfileSpecific.class) != null) return true;
        if (f.getAnnotation(HypixelKey.class) != null) return true;
        Exclude exclude = f.getAnnotation(Exclude.class);
        return exclude != null;
    }

    /**
     * @param clazz the class object that is under test
     * @return true if the class should be ignored; otherwise false
     */
    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        Exclude exclude = clazz.getAnnotation(Exclude.class);
        return exclude != null;
    }
}
