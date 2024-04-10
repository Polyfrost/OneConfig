/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

//#if MC<=11202 && FORGE==1
package org.polyfrost.oneconfig.utils.forge;

import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.ILanguageAdapter;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * An adapter for FML to allow for the use of Kotlin objects as a mod class.
 * This is not required if you use a Kotlin class, only if you use a Kotlin object.
 * <br>
 * Adapted from Crimson under LGPL 3.0
 * <a href="https://github.com/Deftu-Archive/Crimson/blob/main/LICENSE">(click here)</a>
 */
public class KotlinLanguageAdapter implements ILanguageAdapter {

    @Override
    public boolean supportsStatics() {
        return false;
    }

    @Override
    public Object getNewInstance(FMLModContainer container, Class<?> objectClass, ClassLoader classLoader, Method factoryMarkedAnnotation) {
        return getObjectInstance(objectClass);
    }

    @Override
    public void setProxy(Field target, Class<?> proxyTarget, Object proxy) {
        try {
            target.set(getObjectInstance(proxyTarget), proxy);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set proxy", e);
        }
    }

    private static Object getObjectInstance(Class<?> objectClass) {
        try {
            Object instance = kotlin.jvm.JvmClassMappingKt.getKotlinClass(objectClass).getObjectInstance();
            return instance == null ? objectClass.getDeclaredConstructor().newInstance() : instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object instance", e);
        }
    }

    @Override
    public void setInternalProxies(ModContainer mod, Side side, ClassLoader loader) {
    }
}
//#endif