/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1

import org.jetbrains.annotations.Contract
import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty0

object Properties {
    /**
     * create a new property which is internally backed by a field.
     */
    @JvmStatic
    @JvmOverloads
    @Contract("_, _, _, null, null -> fail")
    fun <T> simple(
        id: String? = null,
        name: String? = null,
        description: String? = null,
        value: T? = null, cls: Class<T>? = null
    ): Property<T> = Property.Simple(id, name, description, value, cls)

    /**
     * create a property which is internally backed by a field.
     */
    @JvmStatic
    fun <T> simple(value: T): Property<T> = Property.Simple(null, null, null, value, null)

    /**
     * create a property backed by the given [field]. The [owner] is the object that owns the field.
     *
     * If the field is static, the owner should be `null`.
     */
    @JvmStatic
    @JvmOverloads
    fun <T> field(
        name: String? = null,
        description: String? = null,
        field: Field,
        owner: Any? = null
    ): Property<T> = Property.Field(name, description, field, owner)

    /**
     * create a property which has no serializable value. the type is [Void] and the value is always `null`.
     */
    @JvmStatic
    @JvmOverloads
    fun dummy(
        id: String? = null,
        name: String? = null,
        description: String? = null,
    ): Property<Void> = Property.Dummy(id, name, description)

    /**
     * create a property which is backed by the given kotlin property reference, such as `this::foo`.
     *
     * for the inverse, where a property is backed by a property, see `KtConfig`.
     */
    @Suppress("DEPRECATION")
    @JvmSynthetic
    inline fun <reified T> ktProperty(
        ref: KMutableProperty0<T>,
        name: String? = null,
        description: String? = null,
    ): Property<T> = Property.KtProperty(name, description, ref, T::class.java)

    /**
     * Return a property which delegates to the given [getter] and [setter].
     */
    @JvmStatic
    @JvmOverloads
    fun <T> functional(
        getter: java.util.function.Supplier<T>,
        setter: java.util.function.Consumer<T>,
        id: String? = null,
        name: String? = null,
        description: String? = null,
        type: Class<T>? = null,
    ): Property<T> = Property.Functional(id, name, description, setter, getter, type)

}