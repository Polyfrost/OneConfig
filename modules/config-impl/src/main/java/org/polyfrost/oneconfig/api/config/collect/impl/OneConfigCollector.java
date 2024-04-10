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

package org.polyfrost.oneconfig.api.config.collect.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.Config;
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.annotations.Accordion;
import org.polyfrost.oneconfig.api.config.annotations.Button;
import org.polyfrost.oneconfig.api.config.annotations.DependsOn;
import org.polyfrost.oneconfig.api.config.annotations.Option;
import org.polyfrost.oneconfig.api.config.util.ObjectSerializer;
import org.polyfrost.oneconfig.utils.MHUtils;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.polyfrost.oneconfig.api.config.DummyProperty.dummy;
import static org.polyfrost.oneconfig.api.config.Node.strv;

/**
 * Collects properties from an object using reflection, and from its inner classes.
 * Ignores fields without an annotation with the {@link Option} annotation.
 */
public class OneConfigCollector extends ReflectiveCollector {
    public OneConfigCollector() {
        super();
    }

    @Override
    public @Nullable Tree collect(@NotNull Object src) {
        if (!(src instanceof Config)) return null;
        Tree tree = super.collect(src);
        assert tree != null;
        tree.onAll((s, n) -> {
            if (!(n instanceof Property)) return;
            Property<?> p = (Property<?>) n;
            String[] conditions = n.getMetadata("conditions");
            if (conditions == null) return;
            for (String cond : conditions) {
                Property<?> condition = tree.getProp(cond);
                if (condition == null) throw new IllegalArgumentException("Property " + n.getID() + " is dependant on property " + cond + ", but that property does not exist");
                if (condition.type == Boolean.class || condition.type == boolean.class) {
                    p.addDisplayCondition(condition::getAs);
                } else throw new IllegalArgumentException("Property " + n.getID() + " is dependant on property " + cond + ", but it is not a boolean property");
            }
        });
        return tree;
    }

    @Override
    public void handleField(@NotNull Field f, @NotNull Object src, @NotNull Tree builder) {
        for (Annotation a : f.getAnnotations()) {
            Option opt = a.annotationType().getAnnotation(Option.class);
            if (opt == null) continue;
            try {
                // asm: use method handle as it fails NOW instead of at set time, and is faster
                final MethodHandle setter = MHUtils.getFieldSetter(f, src).getOrThrow();
                Class<?> type = f.getType();
                Property<?> p = Property.prop(f.getName(), null, MHUtils.getFieldGetter(f, src).getOrThrow().invoke(), type).addCallback(v -> {
                    try {
                        if (type.isArray() && v instanceof List<?>) {
                            setter.invoke(ObjectSerializer.unbox(v, type));
                        } else setter.invoke(v);
                    } catch (Throwable e) {
                        throw new RuntimeException("[internal failure] Failed to setback field", e);
                    }
                });
                handleMetadata(p, a, opt, f);
                builder.put(p);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to create setter for field " + f.getName() + "; ensure it is not static final", e);
            }
            break;
        }
    }

    @Override
    public void handleMethod(@NotNull Method m, @NotNull Object src, @NotNull Tree builder) {
        Button b = m.getDeclaredAnnotation(Button.class);
        if (b == null) return;
        if (m.getParameterCount() != 0) throw new IllegalArgumentException("Button method " + m.getName() + " must have no parameters");
        Property<?> p = dummy(m.getName(), b.title(), b.description());
        final MethodHandle mh = MHUtils.getMethodHandle(m, src).getOrThrow();
        final String methodString = m.toString();
        p.addMetadata("runnable", (Runnable) () -> {
            try {
                mh.invokeExact();
            } catch (Throwable e) {
                Tree.LOGGER.error("Failed to invoke method for button {}", methodString, e);
            }
        });
        p.addMetadata("visualizer", b.annotationType().getAnnotation(Option.class).display());
        p.addMetadata("text", strv(b.text()));
        p.addMetadata("icon", strv(b.icon()));
        p.addMetadata("category", b.category());
        p.addMetadata("subcategory", b.subcategory());
        builder.put(p);
    }

    @Override
    public void handleInnerClass(@NotNull Class<?> c, @NotNull Object src, int depth, @NotNull Tree builder) {
        Accordion a = c.getDeclaredAnnotation(Accordion.class);
        if (a == null) return;
        try {
            Tree t = Tree.tree(c.getSimpleName());
            t.addMetadata(MHUtils.getAnnotationValues(a).getOrThrow());
            handle(t, MHUtils.instantiate(c, false).getOrThrow(), depth + 1);
            builder.put(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleMetadata(@NotNull Property<?> property, @NotNull Annotation a, Option opt, Field f) {
        property.addMetadata("visualizer", opt.display());
        property.addMetadata(MHUtils.getAnnotationValues(a).getOrThrow());
        DependsOn d = f.getDeclaredAnnotation(DependsOn.class);
        if (d != null) {
            property.addMetadata("conditions", d.value());
        }
    }

}
