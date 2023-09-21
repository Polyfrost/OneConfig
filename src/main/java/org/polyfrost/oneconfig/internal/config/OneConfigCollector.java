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

package org.polyfrost.oneconfig.internal.config;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.annotations.Accordion;
import org.polyfrost.oneconfig.api.config.annotations.Button;
import org.polyfrost.oneconfig.api.config.annotations.Option;
import org.polyfrost.oneconfig.api.config.collector.ReflectiveCollector;

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
public class OneConfigCollector extends ReflectiveCollector {

    public OneConfigCollector(int maxDepth) {
        super(maxDepth);
    }

    public OneConfigCollector() {
        super(1);
    }


    @Override
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

    @Override
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

    @Override
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

    public void handleMetadata(@NotNull Property<?> property, @NotNull Field f) {
        for (Annotation a : f.getDeclaredAnnotations()) {
            Option opt = a.annotationType().getAnnotation(Option.class);
            property.addMetadata("annotation", a);
            property.addMetadata("visualizer", opt.display());
            String title = null;
            String description = "";
            String icon = "";
            String category = "General";
            String subcategory = "General";
            for (Method m : a.getClass().getDeclaredMethods()) {
                if (m.getName().equals("title")) {
                    title = (String) invoke(m, a);
                }
                if (m.getName().equals("description")) {
                    description = (String) invoke(m, a);
                }
                if (m.getName().equals("icon")) {
                    icon = (String) invoke(m, a);
                }
                if (m.getName().equals("category")) {
                    category = (String) invoke(m, a);
                }
                if (m.getName().equals("subcategory")) {
                    subcategory = (String) invoke(m, a);
                }
            }
            if (title == null) throw new IllegalArgumentException("Property annotation " + a.getClass().getSimpleName() + " must have a property String title() / cannot be empty");
            property.addMetadata("title", title);
            if (!description.isEmpty()) property.addMetadata("description", description);
            if (!icon.isEmpty()) property.addMetadata("icon", icon);
            property.addMetadata("category", category);
            property.addMetadata("subcategory", subcategory);
        }
    }

    private static Object invoke(Method m, Object o) {
        try {
            return m.invoke(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	/*
	I LIKED IT WHEN I COULD REFLECT ANYTHING I WANTED WHY DID THEY CHANGE IT
		InvocationHandler h = Proxy.getInvocationHandler(a);
		Map<String, Object> memberValues;
		try {
			Field ff = h.getClass().getDeclaredField("memberValues");
			ff.setAccessible(true); // SHUT UP FUCKING MODULE SYSTEM
			memberValues = (Map<String, Object>) ff.get(h);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for(Map.Entry<String, Object> entry : memberValues.entrySet()) {
			if(entry.getKey().equals("title")) {
				title = (String) entry.getValue();
			}
			if(entry.getKey().equals("description")) {
				description = (String) entry.getValue();
			}
			if(entry.getKey().equals("icon")) {
				icon = (String) entry.getValue();
			}
			if(entry.getKey().equals("category")) {
				category = (String) entry.getValue();
			}
			if(entry.getKey().equals("subcategory")) {
				subcategory = (String) entry.getValue();
			}
		}
	}*/


}
