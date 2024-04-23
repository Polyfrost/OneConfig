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

package org.polyfrost.oneconfig.api.event.v1;

import org.polyfrost.oneconfig.api.event.v1.events.Event;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventCollector;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.AnnotationEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Manages all events from OneConfig.
 */
public final class EventManager {
    /**
     * The instance of the {@link EventManager}.
     */
    public static final EventManager INSTANCE = new EventManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/Events");
    private final Deque<EventCollector> collectors = new ArrayDeque<>(2);
    private final Map<Object, List<EventHandler<?>>> cache = new WeakHashMap<>(5);
    private final Map<Class<?>, Set<EventHandler<?>>> handlers = new HashMap<>();


    private EventManager() {
        registerCollector(new AnnotationEventMapper());
    }

    /**
     * Convenience method for registering an event handler. Equal to
     * {@link EventManager#INSTANCE}{@code .register(}{@link EventHandler#of(Class, Consumer)}{@code )}
     */
    public static <E extends Event> EventHandler<E> register(Class<E> cls, Consumer<E> handler) {
        return EventHandler.of(cls, handler).register();
    }

    public static <E extends Event> EventHandler<E> register(Class<E> cls, Runnable handler) {
        return EventHandler.of(cls, handler).register();
    }

    /**
     * Convenience method for registering an event handler. Equal to
     * {@link EventManager#INSTANCE}{@code .register(}{@link EventHandler#of(Class, Consumer)}{@code )}
     */
    public static <E extends Event> EventHandler<E> register(kotlin.reflect.KClass<E> cls, Consumer<E> handler) {
        return EventHandler.of(kotlin.jvm.JvmClassMappingKt.getJavaClass(cls), handler).register();
    }

    /**
     * Convenience method for registering an event handler. Equal to
     * {@link EventManager#INSTANCE}{@code .register(}{@link EventHandler#of(Method, Object)}{@code )}
     */
    @SuppressWarnings("unchecked")
    public static <E extends Event> EventHandler<E> register(Method m, Object owner) {
        return (EventHandler<E>) EventHandler.of(m, owner).register();
    }

    /**
     * Registers an object to the event manager. If you wish to be able to remove/unregister you events, make sure you set removable to true.
     *
     * @param object The object to register.
     */
    public void register(Object object) {
        register(object, false);
    }

    /**
     * Register an object to the event manager.
     *
     * @param removable weather this object's event handlers can be removed.
     */
    public void register(Object object, boolean removable) {
        for (EventCollector m : collectors) {
            List<EventHandler<?>> h = m.collect(object);
            if (h == null) continue;
            if (removable) cache.put(object, h);
            for (EventHandler<?> handler : h) {
                register(handler);
            }
        }
    }

    @SafeVarargs
    public final void register(EventHandler<? extends Event>... handlers) {
        for (EventHandler<?> handler : handlers) {
            register(handler);
        }
    }

    /**
     * Register an event handler.
     *
     * @param handler The handler to register.
     * @return true if the handler was registered successfully; false if it was already registered.
     */
    public boolean register(EventHandler<?> handler) {
        Set<EventHandler<?>> set = handlers.computeIfAbsent(handler.getEventClass(), k -> new HashSet<>());
        if (!set.add(handler)) {
            LOGGER.warn("Attempted to register a handler twice!");
            return false;
        }
        return true;
    }

    @SafeVarargs
    public final void unregister(EventHandler<? extends Event>... handlers) {
        for (EventHandler<?> handler : handlers) {
            unregister(handler);
        }
    }

    public boolean unregister(EventHandler<?> handler) {
        Set<EventHandler<?>> set = handlers.get(handler.getEventClass());
        if (set == null) return false;
        if (!set.remove(handler)) {
            LOGGER.warn("Attempted to unregister a handler that was not registered!");
            return false;
        }
        return true;
    }

    /**
     * Remove the event handler's that were provided by the given object.
     * <br><b>This method only works if the object was registered with removable true!</b>
     */
    public boolean unregister(Object object) {
        List<EventHandler<?>> h = cache.remove(object);
        if (h == null) return false;
        boolean state = true;
        for (EventHandler<?> handler : h) {
            if (!unregister(handler)) {
                state = false;
            }
        }
        return state;
    }

    public void registerCollector(EventCollector collector) {
        collectors.addFirst(collector);
    }

    /**
     * Posts an event to any registered listeners.
     *
     * @param event The event to post.
     */
    @SuppressWarnings("unchecked")
    public <E extends Event> void post(E event) {
        if (event == null) return;
        Set<EventHandler<?>> set = handlers.get(event.getClass());
        if (set == null) return;
        Event.Cancellable evc = event instanceof Event.Cancellable ? (Event.Cancellable) event : null;
        try {
            for (EventHandler<?> ev : set) {
                if (evc != null && evc.cancelled) break;
                try {
                    ((EventHandler<E>) ev).handle(event);
                } catch (EventException e) {
                    throw e;
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to invoke event handler!", throwable);
                }
            }
        } catch (ConcurrentModificationException ignored0) {
            LOGGER.error("Handler modified event handlers when calling, failed to dispatch {}", event.getClass().getName());
        }
    }


}
