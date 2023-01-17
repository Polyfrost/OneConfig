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

package cc.polyfrost.oneconfig.api.v1.config.option;

import cc.polyfrost.oneconfig.api.v1.config.OneConfig;
import cc.polyfrost.oneconfig.api.v1.config.option.type.annotations.Button;
import cc.polyfrost.oneconfig.api.v1.config.option.type.OptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.annotations.Accordion;

/**
 * Represents an option from an {@link OneConfig}.
 */
public interface OptionHolder {
    /**
     * @return The {@link OptionType} that this {@link OptionHolder} represents.
     */
    OptionType getOptionType();

    /**
     * Either returns the value of the option, or invokes the method if the option is a {@link Button}.
     * @return The value of the option, or {@link NoReturnValue} if the option is a {@link Button}.
     */
    Object invoke();

    /**
     * Sets the value of the option.
     * @param value The value to set.
     */
    void set(Object value);

    /**
     * Returns the name of the option as it appears in code.
     * @return The name of the option as it appears in code.
     */
    String getJavaName();

    /**
     * Returns the name of the option as it appears visually.
     * @return The name of the option as it appears visually.
     */
    String getDisplayName();

    /**
     * Returns the category of the option.
     * @return The category of the option.
     */
    String getDisplayCategory();

    /**
     * Returns the subcategory of the option.
     * @return The subcategory of the option.
     */
    String getDisplaySubcategory();

    /**
     * Returns the search tags of the option.
     * @return The search tags of the option.
     */
    String[] getSearchTags();

    /**
     * Returns the description of the option.
     * @return The description of the option.
     */
    String getDescription();

    /**
     * Returns the {@link Class} of the option.
     * @return The {@link Class} of the option.
     */
    Class<?> getJavaClass();

    /**
     * Returns whether the option is an {@link Accordion}.
     * @return Whether the option is an {@link Accordion}.
     */
    boolean isAccordion();

    /**
     * Returns the {@link Accordion} that this option is a part of, or null if it is not an {@link Accordion}.
     * @return The {@link Accordion} that this option is a part of, or null if it is not an {@link Accordion}.
     */
    Accordion getAccordion();

    /**
     * Used when methods like {@link #invoke()} and {@link #set(Object)} are called, but the
     * option is not accessible.
     */
    @SuppressWarnings("InstantiationOfUtilityClass")
    final class NoReturnValue {
        /**
         * The only instance of {@link NoReturnValue}.
         */
        public static final NoReturnValue INSTANCE = new NoReturnValue();

        private NoReturnValue() {
        }
    }
}
