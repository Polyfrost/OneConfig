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

package cc.polyfrost.oneconfig.api.v1.config.option.type.options;

import cc.polyfrost.oneconfig.api.v1.config.option.type.OptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.annotations.Number;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NumberOptionType implements OptionType {
    @Override
    public String name() {
        return "Number";
    }

    @Override
    public boolean isSupportedClass(Class<?> type) {
        return type.isAssignableFrom(Double.class) || type.isAssignableFrom(Double.TYPE) || type.isAssignableFrom(Float.class) || type.isAssignableFrom(Float.TYPE) || type.isAssignableFrom(Integer.class) || type.isAssignableFrom(Integer.TYPE) || type.isAssignableFrom(java.lang.Number.class);
    }

    @Override
    public String getName(Field field) {
        return field.getAnnotation(Number.class).name();
    }

    @Override
    public String getName(Method method) {
        return method.getAnnotation(Number.class).name();
    }

    @Override
    public String getDescription(Field field) {
        return field.getAnnotation(Number.class).description();
    }

    @Override
    public String getDescription(Method method) {
        return method.getAnnotation(Number.class).description();
    }

    @Override
    public String getCategory(Field field) {
        return field.getAnnotation(Number.class).category();
    }

    @Override
    public String getCategory(Method method) {
        return method.getAnnotation(Number.class).category();
    }

    @Override
    public String getSubcategory(Field field) {
        return field.getAnnotation(Number.class).subcategory();
    }

    @Override
    public String getSubcategory(Method method) {
        return method.getAnnotation(Number.class).subcategory();
    }

    @Override
    public String[] getTags(Field field) {
        return field.getAnnotation(Number.class).tags();
    }

    @Override
    public String[] getTags(Method method) {
        return method.getAnnotation(Number.class).tags();
    }
}
