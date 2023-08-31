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

package org.polyfrost.oneconfig.internal.config.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.annotations.Accordion;
import org.polyfrost.oneconfig.api.config.annotations.Button;
import org.polyfrost.oneconfig.api.config.annotations.Option;
import org.polyfrost.oneconfig.api.config.collector.PropertyCollector;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.polyfrost.oneconfig.api.config.Property.prop;
import static org.polyfrost.oneconfig.api.config.Tree.LOGGER;
import static org.polyfrost.oneconfig.api.config.backend.impl.ObjectSerializer.unboxFully;

/**
 * Collects properties from an object using reflection, and from its inner classes.
 * Ignores transient and synthetic fields.
 */
public class ReflectiveCollector implements PropertyCollector {
    protected final int maxDepth;

    public ReflectiveCollector(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public ReflectiveCollector() {
        this(1);
    }


    @Override
    public @Nullable Tree collect(@Nullable String id, @NotNull Object src) {
        // if(!(src instanceof Config)) return null;
        Tree.Builder b = Tree.tree(id == null ? src.getClass().getSimpleName() : id);
        handle(b, src, 0);
        return b.build();
    }


    public void handleField(@NotNull Field f, @NotNull Object src, @NotNull Tree.Builder builder) {
        for (Annotation a : f.getAnnotations()) {
            for (Annotation aa : a.annotationType().getAnnotations()) {
                if (aa.annotationType().equals(Option.class)) {
                    try {
                        f.setAccessible(true);
                        // asm: use method handle as it fails NOW instead of at set time, and is faster
                        MethodHandle mh = MethodHandles.lookup().unreflectSetter(f);
                        if (!Modifier.isStatic(f.getModifiers())) mh = mh.bindTo(src);
                        final MethodHandle setter = mh;
                        Property<?> p = prop(f.getName(), f.get(src)).addCallback(v -> {
                            try {
                                System.out.println("glSET " + f.getName() + " -> " + v);
                                if (f.getType().isArray() && v instanceof List<?>) {
                                    setter.invoke(unboxFully(v, null));
                                } else setter.invoke(v);
                            } catch (Throwable e) {
                                throw new RuntimeException("[internal failure] Failed to setback field", e);
                            }
                        });
                        handleMetadata(p, f);
                        builder.put(p);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to create setter for field " + f.getName() + "; ensure it is not static final", e);
                    }
                }
            }
        }
    }

    public void handleMethod(@NotNull Method m, @NotNull Object src, @NotNull Tree.Builder builder) {
        Button b = m.getDeclaredAnnotation(Button.class);
        if (b == null) return;
        if (m.getParameterCount() != 0) throw new IllegalArgumentException("Button method " + m.getName() + " must have no parameters");
        Property<?> p = prop(m.getName() + "$synthetic", m);
        m.setAccessible(true);
        MethodHandle methodHandle;
        try {
            methodHandle = MethodHandles.lookup().unreflect(m);
            if (!Modifier.isStatic(m.getModifiers())) methodHandle = methodHandle.bindTo(src);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unreflect " + m.getName() + "() for button " + b.title(), e);
        }
        final MethodHandle mh = methodHandle;
        p.addMetadata("runnable", (Runnable) () -> {
            try {
                mh.invokeExact();
            } catch (Throwable e) {
                LOGGER.error("Failed to invoke method for button " + b.title(), e);
            }
        });
        p.addMetadata("visualizer", b.annotationType().getAnnotation(Option.class).display());
        p.addMetadata("annotation", b);
        p.addMetadata("title", b.title());
        p.addMetadata("description", b.description().isEmpty() ? null : b.description());
        p.addMetadata("icon", b.icon());
        p.addMetadata("category", b.category());
        p.addMetadata("subcategory", b.subcategory());
        builder.put(p);
    }

    public void handleInnerClass(@NotNull Class<?> c, @NotNull Object src, int depth, @NotNull Tree.Builder builder) {
        Accordion a = c.getDeclaredAnnotation(Accordion.class);
        if (a == null) return;
        try {
            Object innerObject;
            if (Modifier.isStatic(c.getModifiers())) {
                Constructor<?> ctor = c.getDeclaredConstructor();
                ctor.setAccessible(true);
                innerObject = ctor.newInstance();
            } else {
                Constructor<?> ctor = c.getDeclaredConstructor(src.getClass());
                ctor.setAccessible(true);
                innerObject = ctor.newInstance(src);
            }
            Tree.Builder innerBuilder = Tree.tree(c.getSimpleName());
            handle(innerBuilder, innerObject, depth + 1);
            Tree t = innerBuilder.build();
            t.addMetadata("annotation", a);
            builder.put(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public final void handle(@NotNull Tree.Builder builder, @NotNull Object src, int depth) {
        for (Field f : src.getClass().getDeclaredFields()) {
            handleField(f, src, builder);
        }
        for (Method m : src.getClass().getDeclaredMethods()) {
            handleMethod(m, src, builder);
        }
        for (Class<?> c : src.getClass().getDeclaredClasses()) {
            if (depth == maxDepth) {
                LOGGER.warn("Reached max depth for tree " + builder.id + " ignoring further subclasses!");
                return;
            }
            handleInnerClass(c, src, depth, builder);
        }
    }

    /**
     * Handle metadata for the property.
     *
     * @param property the property
     * @param f        the backing field
     */
    public void handleMetadata(@NotNull Property<?> property, @NotNull Field f) {
    }


}
