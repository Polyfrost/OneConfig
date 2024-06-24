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

package org.polyfrost.oneconfig.api.event.v1.invoke;

import org.polyfrost.oneconfig.api.event.v1.EventException;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Class which represents an event handler.
 *
 * @param <E> The event type
 * @see #of(Class, Consumer)
 */
public abstract class EventHandler<E extends Event> {
    public static final byte ERROR_THRESHOLD = 10;
    private byte errors = 0;

    /**
     * Create an event handler from a consumer, in a fabric-style way.
     *
     * @param cls     the event class
     * @param handler the predicate for the event. Return true to remove the event handler.
     * @param <E>     the event type
     * @return the event handler
     */
    @kotlin.OverloadResolutionByLambdaReturnType
    public static <E extends Event> EventHandler<E> ofRemoving(Class<E> cls, Predicate<E> handler) {
        return new EventHandler<E>() {
            @Override
            public boolean handle(E event) {
                return handler.test(event);
            }

            @Override
            public Class<E> getEventClass() {
                return cls;
            }
        };
    }

    /**
     * Create an event handler from a consumer, in a fabric-style way.
     *
     * @param cls     the event class
     * @param handler the consumer
     * @param <E>     the event type
     * @return the event handler
     */
    @kotlin.OverloadResolutionByLambdaReturnType
    public static <E extends Event> EventHandler<E> of(Class<E> cls, Consumer<E> handler) {
        return new EventHandler<E>() {
            @Override
            public boolean handle(E event) {
                handler.accept(event);
                return false;
            }

            @Override
            public Class<E> getEventClass() {
                return cls;
            }
        };
    }

    @kotlin.OverloadResolutionByLambdaReturnType
    public static <E extends Event> EventHandler<E> of(Class<E> cls, Runnable handler) {
        return new EventHandler<E>() {
            @Override
            public boolean handle(E event) {
                handler.run();
                return false;
            }

            @Override
            public Class<E> getEventClass() {
                return cls;
            }
        };
    }

    /**
     * Create an event handler from a method. <br>
     * Note the intended usage of this is using the {@link Subscribe} annotation, and not this method directly.
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

    public abstract boolean handle(E event);

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

    public final boolean onError() {
        return !(errors++ > ERROR_THRESHOLD);
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
