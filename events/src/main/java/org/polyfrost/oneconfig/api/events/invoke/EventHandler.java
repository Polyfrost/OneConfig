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

package org.polyfrost.oneconfig.api.events.invoke;

import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.Event;

import java.util.function.Consumer;

/**
 * Class which represents an event handler.
 * @param <T> The event type
 */
public abstract class EventHandler<T extends Event> {
    public abstract void handle(T event) throws Throwable;

    public abstract Class<T> getEventClass();

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
    public int hashCode() {
        return this.getEventClass().hashCode() + (31 * super.hashCode());
    }




    public static <E extends Event> EventHandler<E> create(Class<E> cls, Consumer<E> handler) {
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

    /**
     * Convenience method for registering an event handler. Equal to
     * {@link EventManager#INSTANCE}{@code .register(}{@link EventHandler#create(Class, Consumer)}{@code )}
     */
    public static <E extends Event> EventHandler<E> register(Class<E> cls, Consumer<E> handler) {
        EventHandler<E> h = create(cls, handler);
        EventManager.INSTANCE.register(h);
        return h;
    }
}
