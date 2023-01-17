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
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Switch;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class SwitchOptionType implements OptionType {

    @Override
    public String name() {
        return "Switch";
    }

    @Override
    public List<Class<?>> supportedClasses() {
        return Lists.newArrayList(Boolean.class, boolean.class);
    }

    @Override
    public String getName(Field field) {
        return field.getAnnotation(Switch.class).name();
    }

    @Override
    public String getName(Method method) {
        return method.getAnnotation(Switch.class).name();
    }

    @Override
    public String getDescription(Field field) {
        return field.getAnnotation(Switch.class).description();
    }

    @Override
    public String getDescription(Method method) {
        return method.getAnnotation(Switch.class).description();
    }

    @Override
    public String getCategory(Field field) {
        return field.getAnnotation(Switch.class).category();
    }

    @Override
    public String getCategory(Method method) {
        return method.getAnnotation(Switch.class).category();
    }

    @Override
    public String getSubcategory(Field field) {
        return field.getAnnotation(Switch.class).subcategory();
    }

    @Override
    public String getSubcategory(Method method) {
        return method.getAnnotation(Switch.class).subcategory();
    }

    @Override
    public String[] getTags(Field field) {
        return field.getAnnotation(Switch.class).tags();
    }

    @Override
    public String[] getTags(Method method) {
        return method.getAnnotation(Switch.class).tags();
    }
}
