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
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;

import java.util.function.Consumer;

public final class EventDelay {

    private EventDelay() {
    }

    /**
     * Schedules a Consumer to be called after a certain amount polls from the given event.
     * <p>
     * If the amount of polls is below 1, the Consumer will be called immediately, with the event being null.
     */
    public static <E extends Event> void of(Class<E> cls, int polls, Consumer<E> function) {
        if (polls < 1) {
            function.accept(null);
        } else {
            new EventHandler<E>() {
                private int delay = polls;

                @Override
                public void handle(E event) {
                    // Delay expired
                    if (delay < 1) {
                        function.accept(event);
                        EventManager.INSTANCE.unregister(this);
                    } else {
                        delay--;
                    }
                }

                @Override
                public Class<E> getEventClass() {
                    return cls;
                }
            }.register();
        }
    }

    /**
     * {@link #of(Class, int, Consumer)} with a Runnable.
     */
    public static <E extends Event> void of(Class<E> cls, int polls, Runnable function) {
        of(cls, polls, e -> function.run());
    }

    /**
     * {@link #of(Class, int, Runnable)} with TickEvent.End.
     */
    public static void tick(int ticks, Runnable function) {
        of(TickEvent.End.class, ticks, function);
    }

    /**
     * {@link #of(Class, int, Runnable)} with RenderEvent.End.
     */
    public static void render(int ticks, Runnable function) {
        of(RenderEvent.End.class, ticks, function);
    }
}
