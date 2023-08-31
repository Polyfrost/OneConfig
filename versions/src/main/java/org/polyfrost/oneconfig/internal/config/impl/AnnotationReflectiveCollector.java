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
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.annotations.Option;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AnnotationReflectiveCollector extends ReflectiveCollector {
    @Override
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
