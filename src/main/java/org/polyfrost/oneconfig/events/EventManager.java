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

package org.polyfrost.oneconfig.events;

import org.polyfrost.oneconfig.libs.eventbus.EventBus;
import org.polyfrost.oneconfig.libs.eventbus.exception.ExceptionHandler;
import org.polyfrost.oneconfig.libs.eventbus.invokers.LMFInvoker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages all events from OneConfig.
 */
public final class EventManager {
    /**
     * The instance of the {@link EventManager}.
     */
    public static final EventManager INSTANCE = new EventManager();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/EventManager");
    private final EventBus eventBus = new EventBus(new LMFInvoker(), new OneConfigExceptionHandler());
    private final Set<Object> listeners = new HashSet<>();

    /**
     * Returns the {@link EventBus} instance.
     *
     * @return The {@link EventBus} instance.
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Registers an object to the {@link EventBus}.
     *
     * @param object The object to register.
     * @see EventBus#register(Object)
     */
    public void register(Object object) {
        if (listeners.add(object)) {
            eventBus.register(object);
        } else {
            LOGGER.warn("Attempted to register an already registered listener: " + object);
        }
    }

    /**
     * Unregisters an object from the {@link EventBus}.
     *
     * @param object The object to unregister.
     * @see EventBus#unregister(Object)
     */
    public void unregister(Object object) {
        listeners.remove(object);
        eventBus.unregister(object);
    }

    /**
     * Posts an event to the {@link EventBus}.
     *
     * @param event The event to post.
     * @see EventBus#post(Object)
     */
    public void post(Object event) {
        eventBus.post(event);
    }


    /**
     * Bypass to allow special exceptions to actually crash
     */
    private static class OneConfigExceptionHandler implements ExceptionHandler {
        @Override
        public void handle(@NotNull Exception e) {
            if (e instanceof EventException) {
                throw (EventException) e;
            }
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            } else e.printStackTrace();
        }
    }

    /**
     * Exception thrown in an event if the game should crash.
     */
    public static class EventException extends RuntimeException {
        public EventException(String message) {
            super(message);
        }
    }
}
