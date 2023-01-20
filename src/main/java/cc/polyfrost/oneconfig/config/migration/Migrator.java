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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public interface Migrator {
    /**
     * Get the value from its name, category, and subcategory. The target field is also supplied, which can be used to check for migration names.
     * The returned Object is intended to be a "Duck" object, and should be cast to the correct type. The Migrator should never return ClassCastExceptions.
     *
     * @param field       The target field of the option
     * @param name        The name of the option (has to be present)
     * @param category    The category of the option
     * @param subcategory The subcategory of the option
     * @return Value of the option, null if not found
     * @apiNote <b>The nullability of the subcategory or category depends on the implementation. Please check for @NotNull or @Nullable in your implementation.</b>
     */
    @Nullable
    Object getValue(Field field, @NotNull String name, String category, String subcategory);


}
