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

package org.polyfrost.oneconfig.api.events.invoke.impl;

import org.polyfrost.oneconfig.api.events.EventException;
import org.polyfrost.oneconfig.api.events.event.Event;
import org.polyfrost.oneconfig.api.events.invoke.EventHandler;
import org.polyfrost.oneconfig.utils.MHUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public class MHHandler<T extends Event> extends EventHandler<T> {
    private final MethodHandle mh;
    private final Class<T> cls;
    private final String name;

    public MHHandler(MethodHandle mh, Class<T> cls, String name) {
        this.mh = mh;
        this.cls = cls;
        this.name = name;
    }

    @Override
    public void handle(T event) throws Throwable {
        mh.invokeExact(event);
    }

    @Override
    public String toString() {
        return "MHHandler(method=" + name + ")";
    }

    @Override
    public Class<T> getEventClass() {
        return cls;
    }

    @Override
    public boolean equals(EventHandler<?> other) {
        if (!(other instanceof MHHandler)) return false;
        MHHandler<?> h = (MHHandler<?>) other;
        return h.name.equals(name);
    }

    public static EventHandler<?> ofMethod(Method m, Object owner) {
        MethodHandle handle = MHUtils.getMethodHandle(m, owner);
        if (m.getParameterCount() != 1) {
            throw new EventException("Failed to register event handler: Method annotated with @Subscribe must have 1 parameter of type Event");
        }
        Class<?> eventClass = m.getParameterTypes()[0];
        if (!Event.class.isAssignableFrom(eventClass)) {
            throw new EventException("Failed to register event handler: Method annotated with @Subscribe must have 1 parameter of type Event");
        }
        handle = handle.asType(handle.type().changeParameterType(0, Event.class));
        return new MHHandler(handle, eventClass, m.getName());
    }
}
