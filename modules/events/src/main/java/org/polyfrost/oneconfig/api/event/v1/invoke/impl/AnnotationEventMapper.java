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

package org.polyfrost.oneconfig.api.event.v1.invoke.impl;

import org.polyfrost.oneconfig.api.event.v1.EventException;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventCollector;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AnnotationEventMapper implements EventCollector {
    @Override
    public List<EventHandler<?>> collect(Object object) {
        Method[] methods = object.getClass().getDeclaredMethods();
        List<EventHandler<?>> list = new ArrayList<>(Math.min(10, methods.length));
        for (Method m : methods) {
            if (!m.isAnnotationPresent(Subscribe.class)) continue;
            list.add(create(m, object));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static EventHandler<?> create(Method m, Object owner) {
        try {
            if (m.getParameterCount() != 1) {
                throw new EventException("Failed to register event handler: Method must have 1 parameter of type Event");
            }
            Class<Event> eventClass = (Class<Event>) m.getParameterTypes()[0];
            if (m.getReturnType() == boolean.class) {
                Predicate<Event> f = MHUtils.getPredicateHandle(owner, m.getName(), eventClass).getOrThrow();
                return new EventHandler<Event>() {
                    @Override
                    public boolean handle(Event event) {
                        return f.test(event);
                    }

                    @Override
                    public Class<Event> getEventClass() {
                        return eventClass;
                    }
                };
            } else if (m.getReturnType() == void.class) {
                Consumer<Event> f = MHUtils.getConsumerHandle(owner, m.getName(), eventClass).getOrThrow();
                return new EventHandler<Event>() {
                    @Override
                    public boolean handle(Event event) {
                        f.accept(event);
                        return false;
                    }

                    @Override
                    public Class<Event> getEventClass() {
                        return eventClass;
                    }
                };
            } else throw new IllegalArgumentException("Failed to register event handler: Method must return boolean or void");
        } catch (Throwable e) {
            throw new EventException("Failed to register event handler: signature should be public void method(Event event) {}", e);
        }
    }


}
