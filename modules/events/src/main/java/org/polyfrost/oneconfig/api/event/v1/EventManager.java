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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventCollector;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.AnnotationEventMapper;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages all events from OneConfig.
 */
public final class EventManager {
    /**
     * The instance of the {@link EventManager}.
     */
    public static final EventManager INSTANCE = new EventManager();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Events");
    private final Deque<EventCollector> collectors = new ArrayDeque<>(2);
    private final Map<Object, List<EventHandler<?>>> cache = new WeakHashMap<>(5);
    private final Map<Class<? extends Event>, List<EventHandler<?>>> handlers = new HashMap<>(8);


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
            if (h == null || h.isEmpty()) continue;
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
     */
    public void register(EventHandler<?> handler) {
        List<EventHandler<?>> handles = handlers.computeIfAbsent(handler.getEventClass(), k -> new CopyOnWriteArrayList<>());
        if (handles.isEmpty()) {
            handles.add(handler);
            return;
        }
        int idx = Collections.binarySearch(handles, handler);
        if (idx < 0) {
            handles.add(-idx - 1, handler);
        } else {
            handles.add(idx, handler);
        }
    }

    @SafeVarargs
    public final void unregister(EventHandler<? extends Event>... handlers) {
        for (EventHandler<?> handler : handlers) {
            unregister(handler);
        }
    }

    public boolean unregister(EventHandler<?> handler) {
        Collection<EventHandler<?>> set = handlers.get(handler.getEventClass());
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
        Collection<EventHandler<?>> h = cache.remove(object);
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends Event> void post(E event) {
        if (event == null) return;
        Collection<EventHandler<E>> handles = (Collection) handlers.get(event.getClass());
        if (handles == null) return;
        Event.Cancellable evc = event instanceof Event.Cancellable ? (Event.Cancellable) event : null;
        for (EventHandler<E> handler : handles) {
            if (evc != null && evc.cancelled) return;
            try {
                if (handler.handle(event)) handles.remove(handler);
            } catch (EventException ex) {
                throw ex;
            } catch (Throwable throwable) {
                LOGGER.error("Failed to invoke event handler for {}", event.getClass().getName(), throwable);
                if (handler.onError()) {
                    LOGGER.error("removing {} registered to {} as it has failed too many times ({})", handler, event.getClass().getName(), EventHandler.ERROR_THRESHOLD);
                    unregister(handler);
                }
            }
        }
    }


}
