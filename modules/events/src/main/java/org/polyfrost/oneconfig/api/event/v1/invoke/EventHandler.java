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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.Event;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Class which represents an event handler.
 *
 * @param <E> The event type
 * @see #of(Class, Consumer)
 */
public abstract class EventHandler<E extends Event> implements Comparable<EventHandler<E>> {
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

    public abstract boolean handle(E event);

    public abstract Class<E> getEventClass();

    /**
     * Set the priority of this event handler. Higher priority handlers are called first.
     * <br> The default priority is 0. <br> if two handlers have the same priority, the order of registration is used.
     */
    public int getPriority() {
        return 0;
    }

    @Override
    public final int compareTo(@NotNull EventHandler<E> o) {
        return Integer.compare(o.getPriority(), this.getPriority());
    }

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

    @ApiStatus.Internal
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
