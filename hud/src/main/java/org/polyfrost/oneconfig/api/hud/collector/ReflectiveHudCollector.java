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

package org.polyfrost.oneconfig.api.hud.collector;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.hud.annotations.CustomComponent;
import org.polyfrost.oneconfig.api.hud.annotations.HudComponent;
import org.polyfrost.oneconfig.internal.config.OneConfigCollector;
import org.polyfrost.polyui.component.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import static org.polyfrost.oneconfig.api.config.Property.prop;

public class ReflectiveHudCollector extends OneConfigCollector {
	@Override
	public void handleField(@NotNull Field f, @NotNull Object src, @NotNull Tree tree) {
		super.handleField(f, src, tree);
		HudComponent c = f.getDeclaredAnnotation(HudComponent.class);
		if (c == null) return;
		try {
			f.setAccessible(true);
			if (!Component.class.isAssignableFrom(f.getType())) {
				throw new IllegalArgumentException("@HudComponent " + f.getName() + " must be of type Component");
			}
			Property<?> p = prop(f.getName(), f.get(src));
			p.addMetadata("annotation", c);
			p.addMetadata("isHud", "");
			tree.put(p);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get value of " + f.getName() + " for HUD component", e);
		}
	}

	@Override
	public void handleMethod(@NotNull Method m, @NotNull Object src, @NotNull Tree tree) {
		super.handleMethod(m, src, tree);
		CustomComponent c = m.getDeclaredAnnotation(CustomComponent.class);
		if (c == null) return;
		if (m.getParameterCount() != 8) throw new IllegalArgumentException("CustomComponent method " + m.getName() + " must have signature of (UMatrixStack stack, float x, float y, float w, float h, float sx, float sy, double rotation");
		Parameter[] ps = m.getParameters();
		// todo
		if (!ps[0].getType().equals(float.class)) throw new IllegalArgumentException("CustomComponent method " + m.getName() + " must have first parameter of type UMatrixStack");
		for (int i = 1; i < 7; i++) {
			if (!ps[i].getType().equals(float.class)) throw new IllegalArgumentException("CustomComponent method " + m.getName() + " must have parameter " + i + " of type float");
		}
		if (!ps[7].getType().equals(double.class)) throw new IllegalArgumentException("CustomComponent method " + m.getName() + " must have last parameter of type double");

		Property<?> p = prop(m.getName() + "$synthetic", m);
        p.addMetadata("synthetic", true);
		m.setAccessible(true);
		MethodHandle methodHandle;
		try {
			methodHandle = MethodHandles.lookup().unreflect(m);
			if (!Modifier.isStatic(m.getModifiers())) methodHandle = methodHandle.bindTo(src);
		} catch (Exception e) {
			throw new RuntimeException("Failed to unreflect " + m.getName() + "() for custom component", e);
		}
		p.addMetadata("annotation", c);
		p.addMetadata("render", methodHandle);
		tree.put(p);
	}
}
