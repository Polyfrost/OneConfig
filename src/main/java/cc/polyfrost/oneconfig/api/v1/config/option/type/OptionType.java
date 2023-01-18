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

package cc.polyfrost.oneconfig.api.v1.config.option.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Represents a type of option.
 */
public interface OptionType {
    /**
     * The name of the option type.
     * @return The name of the option type.
     */
    String name();

    /**
     * Whether the given class is supported by this option type.
     * @param type The class to check.
     * @return Whether the given class is supported by this option type.
     */
    boolean isSupportedClass(Class<?> type);

    /**
     * Whether the option type supports serialization.
     * @return Whether the option type supports serialization.
     */
    default boolean serializable() {
        return true;
    }

    /**
     * Get the name of the field provided.
     * @param field The field to get the name of.
     * @return The name of the field provided.
     */
    String getName(Field field);

    /**
     * Get the name of the method provided.
     * @param method The method to get the name of.
     * @return The name of the method provided.
     */
    String getName(Method method);

    /**
     * Get the description of the field provided.
     * @param field The field to get the description of.
     * @return The description of the field provided.
     */
    String getDescription(Field field);

    /**
     * Get the description of the method provided.
     * @param method The method to get the description of.
     * @return The description of the method provided.
     */
    String getDescription(Method method);

    /**
     * Get the category of the field provided.
     * @param field The field to get the category of.
     * @return The category of the field provided.
     */
    String getCategory(Field field);

    /**
     * Get the category of the method provided.
     * @param method The method to get the category of.
     * @return The category of the method provided.
     */
    String getCategory(Method method);

    /**
     * Get the subcategory of the field provided.
     * @param field The field to get the subcategory of.
     * @return The subcategory of the field provided.
     */
    String getSubcategory(Field field);

    /**
     * Get the subcategory of the method provided.
     * @param method The method to get the subcategory of.
     * @return The subcategory of the method provided.
     */
    String getSubcategory(Method method);

    /**
     * Get the search tags of the field provided.
     * @param field The field to get the search tags of.
     * @return The search tags of the field provided.
     */
    String[] getTags(Field field);

    /**
     * Get the search tags of the method provided.
     * @param method The method to get the search tags of.
     * @return The search tags of the method provided.
     */
    String[] getTags(Method method);

    /**
     * Specifies the {@link OptionType} of an annotation.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface TypeTarget {
        /**
         * The {@link OptionType} of the annotation.
         * @return The {@link OptionType} of the annotation.
         */
        Class<? extends OptionType> value();
    }
}
