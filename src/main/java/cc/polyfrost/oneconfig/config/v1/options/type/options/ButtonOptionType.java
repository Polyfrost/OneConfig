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

package cc.polyfrost.oneconfig.config.v1.options.type.options;

import cc.polyfrost.oneconfig.config.v1.options.type.OptionType;
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ButtonOptionType implements OptionType {

    @Override
    public String name() {
        return "Button";
    }

    @Override
    public boolean isSupportedClass(Class<?> type) {
        return type.isAssignableFrom(Runnable.class) || type.isAssignableFrom(Void.TYPE);
    }

    @Override
    public boolean serializable() {
        return false;
    }

    @Override
    public String getName(Field field) {
        return field.getAnnotation(Button.class).name();
    }

    @Override
    public String getName(Method method) {
        return method.getAnnotation(Button.class).name();
    }

    @Override
    public String getDescription(Field field) {
        return field.getAnnotation(Button.class).description();
    }

    @Override
    public String getDescription(Method method) {
        return method.getAnnotation(Button.class).description();
    }

    @Override
    public String getCategory(Field field) {
        return field.getAnnotation(Button.class).category();
    }

    @Override
    public String getCategory(Method method) {
        return method.getAnnotation(Button.class).category();
    }

    @Override
    public String getSubcategory(Field field) {
        return field.getAnnotation(Button.class).subcategory();
    }

    @Override
    public String getSubcategory(Method method) {
        return method.getAnnotation(Button.class).subcategory();
    }

    @Override
    public String[] getTags(Field field) {
        return field.getAnnotation(Button.class).tags();
    }

    @Override
    public String[] getTags(Method method) {
        return method.getAnnotation(Button.class).tags();
    }
}