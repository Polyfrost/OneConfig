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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventCollector;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.AnnotationEventMapper;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.utils.v1.LogScanner;
import org.polyfrost.oneconfig.utils.v1.TableHelper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages all events from OneConfig
 */
public final class EventManager {
    /**
     * The instance of the {@link EventManager}
     */
    public static final EventManager INSTANCE = new EventManager();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Events");
    private final Deque<EventCollector> collectors = new ArrayDeque<>(2);
    private final Map<Object, Iterable<EventHandler<?>>> cache = new WeakHashMap<>(5);
    private final Map<Class<? extends Event>, List<EventHandler<?>>> handlers = new HashMap<>(8);

    private EventManager() {
        registerCollector(new AnnotationEventMapper());
        if (Platform.compatibility().isDevelopment()) {
            register(EventHandler.of(
                    InitializationEvent.class, () -> {
                        if (!handlers.isEmpty()) {
                            LOGGER.info(getRegisteredHandlersDebugTable());
                        }
                    }));
        }
    }

    /**
     * Convenience method for registering an event handler
     * <br> Equal to
     * {@link EventManager#INSTANCE}{@code .register(}{@link EventHandler#of(Class, Consumer)}{@code )}
     */
    public static <E extends Event> EventHandler<E> register(Class<E> cls, Consumer<E> handler) {
        return EventHandler.of(cls, handler).register();
    }

    public static <E extends Event> EventHandler<E> register(Class<E> cls, Runnable handler) {
        return EventHandler.of(cls, handler).register();
    }

    /**
     * Convenience method for registering an event handler
     * <br> Equal to
     * {@link EventManager#INSTANCE}{@code .register(}{@link EventHandler#of(Class, Consumer)}{@code )}
     */
    public static <E extends Event> EventHandler<E> register(kotlin.reflect.KClass<E> cls, Consumer<E> handler) {
        return EventHandler.of(kotlin.jvm.JvmClassMappingKt.getJavaClass(cls), handler).register();
    }

    public String getRegisteredHandlersDebugTable() {
        String[] classes = new String[handlers.size() + 1];
        String[] handles = new String[handlers.size() + 1];
        classes[0] = "Event Class";
        handles[0] = "Handlers";
        int i = 1;
        for (Map.Entry<Class<? extends Event>, List<EventHandler<?>>> entry : handlers.entrySet()) {
            handles[i] = String.valueOf(entry.getValue().size());
            classes[i++] = entry.getKey().getName().replace("org.polyfrost.oneconfig.api.event.v1.events.", "builtin.");
        }
        return TableHelper.makeTableFromColumns("Registered Event Handlers:", classes, handles);
    }

    /**
     * Registers an object to the event manager
     * <p>
     * Set removable to true if you want to unregister these events later
     *
     * @param object The object to register
     */
    public void register(Object object) {
        register(object, false);
    }

    /**
     * Register an object to the event manager
     *
     * @param removable whether this object's event handlers can be removed
     */
    public void register(Object object, boolean removable) {
        if (object instanceof Class) {
            throw new IllegalArgumentException("Pass an instance of an object, not the class itself!");
        }
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null!");
        }
        boolean registeredSomething = false;
        for (EventCollector m : collectors) {
            Iterable<EventHandler<?>> h = m.collect(object);
            if (h == null) {
                continue;
            }
            Iterator<EventHandler<?>> iter = h.iterator();
            if (!iter.hasNext()) {
                continue;
            }
            if (removable) {
                cache.put(object, h);
            }
            while (iter.hasNext()) {
                register(iter.next());
                registeredSomething = true;
            }
        }
        if (!registeredSomething) {
            throw new IllegalArgumentException("Didn't find any valid event handlers in " +
                    object.getClass()
                            .getName() + "! Make sure you are passing an instance, and that you have a method annotated with @Subscribe or that your custom collector is registered before you call register.");
        }
    }

    @SafeVarargs
    public final void register(EventHandler<? extends Event>... handlers) {
        for (EventHandler<?> handler : handlers) {
            register(handler);
        }
    }

    /**
     * Register an event handler
     *
     * @param handler The handler to register
     */
    public void register(EventHandler<?> handler) {
        List<EventHandler<?>> handles = handlers.computeIfAbsent(
                handler.getEventClass(),
                k -> new CopyOnWriteArrayList<>());
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
        if (set == null) {
            return false;
        }
        if (!set.remove(handler)) {
            return false;
        }
        return true;
    }

    /**
     * Remove the event handlers that were provided by the given object
     * <br><b>This method only works if the object was registered with removable true</b>
     */
    public boolean unregister(Object object) {
        Iterable<EventHandler<?>> h = cache.remove(object);
        if (h == null) {
            throw new IllegalArgumentException(
                    "Event handling object needs to be registered with removable as true to be unregistered; use register(Object, true)");
        }
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
     * Posts an event to any registered listeners
     *
     * @param event The event to post
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends Event> void post(E event) {
        if (event == null) {
            return;
        }
        Collection<EventHandler<E>> handles = (Collection) handlers.get(event.getClass());
        if (handles == null) {
            return;
        }
        Event.Cancellable evc = event instanceof Event.Cancellable ? (Event.Cancellable) event : null;
        for (EventHandler<E> handler : handles) {
            if (evc != null && evc.cancelled) {
                return;
            }
            try {
                if (handler.handle(event)) {
                    handles.remove(handler);
                }
            } catch (EventException ex) {
                throw ex;
            } catch (Throwable throwable) {
                //noinspection StringConcatenationArgumentToLogCall
                LOGGER.error("Failed to invoke event handler for " + event.getClass().getName(), throwable);
                if (Platform.compatibility().isDevelopment()) {
                    throw new EventException(
                            "Event handler " + handler.getClass()
                                    .getName() + " for " + handler.getEventClass().getName() + " failed", throwable);
                }
                if (handler.onError()) {
                    LOGGER.error(
                            "removing {} registered to {} as it has failed too many times ({})",
                            handler,
                            event.getClass().getName(),
                            EventHandler.ERROR_THRESHOLD);
                    String blamed = LogScanner.identifyFromStacktrace(throwable)
                            .stream()
                            .map(ModInfo::getName)
                            .collect(Collectors.joining(", "));
                    Platform.compatibility().displayChatMessage(
                            Component.text(
                                    "Something internally in the mod(s) " + blamed + " has failed. Please report this to their developers and attach the log."
                            ).color(NamedTextColor.RED)
                    );
                    unregister(handler);
                }
            }
        }
    }


}
