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

package org.polyfrost.oneconfig.api.events.invoke;

import org.polyfrost.oneconfig.api.events.EventException;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.Event;
import org.polyfrost.oneconfig.utils.MHUtils;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Class which represents an event handler.
 *
 * @param <E> The event type
 * @see #of(Class, Consumer)
 */
public abstract class EventHandler<E extends Event> {
    /**
     * Create an event handler from a consumer, in a fabric-style way.
     *
     * @param cls     the event class
     * @param handler the consumer
     * @param <E>     the event type
     * @return the event handler
     */
    public static <E extends Event> EventHandler<E> of(Class<E> cls, Consumer<E> handler) {
        return new EventHandler<E>() {
            @Override
            public void handle(E event) {
                handler.accept(event);
            }

            @Override
            public Class<E> getEventClass() {
                return cls;
            }
        };
    }

    public static <E extends Event> EventHandler<E> of(Class<E> cls, Runnable handler) {
        return new EventHandler<E>() {
            @Override
            public void handle(E event) {
                handler.run();
            }

            @Override
            public Class<E> getEventClass() {
                return cls;
            }
        };
    }

    /**
     * Create an event handler from a method. <br>
     * Note the intended usage of this is using the {@link org.polyfrost.oneconfig.api.events.invoke.impl.Subscribe} annotation, and not this method directly.
     *
     * @param m     the method. Can be of any visibility; static or non-static; and must take exactly 1 parameter of type {@link Event}.
     * @param owner the instance where the method is located. If the method is static, this can be null.
     * @return the event handler
     */
    @SuppressWarnings("unchecked")
    public static EventHandler<?> of(Method m, Object owner) {
        try {
            if (m.getParameterCount() != 1) {
                throw new EventException("Failed to register event handler: Method must have 1 parameter of type Event");
            }
            Class<Event> eventClass = (Class<Event>) m.getParameterTypes()[0];
            Consumer<Event> f = MHUtils.getConsumerFunctionHandle(owner, m.getName(), eventClass).getOrThrow();
            return new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    f.accept(event);
                }

                @Override
                public Class<Event> getEventClass() {
                    return eventClass;
                }
            };
        } catch (Throwable e) {
            throw new EventException("Failed to register event handler", e);
        }
    }

    public abstract void handle(E event);

    public abstract Class<E> getEventClass();

    /**
     * Convenience method for registering this event handler.
     * Equivalent to {@code EventManager.INSTANCE.register(this)}.
     *
     * @return this
     */
    public final EventHandler<E> register() {
        EventManager.INSTANCE.register(this);
        return this;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof EventHandler)) return false;
        return this.equals((EventHandler<?>) obj);
    }

    public boolean equals(EventHandler<?> other) {
        return false;
    }

    @Override
    public final int hashCode() {
        return this.getEventClass().hashCode() + (31 * super.hashCode());
    }

}
