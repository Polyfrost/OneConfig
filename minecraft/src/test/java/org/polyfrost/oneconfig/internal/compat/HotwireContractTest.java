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

package org.polyfrost.oneconfig.internal.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Released builds of Hotwire reach these names by reflection to open a mod's page from its own screen, and to take a
 * mod's event handlers out, put them back and run them. They stop doing so without a word when one is missing, so
 * renaming one needs a Hotwire that knows the new name.
 */
class HotwireContractTest {
    private static final String CONFIG = "org.polyfrost.oneconfig.api.config.v1.";
    private static final String EVENT = "org.polyfrost.oneconfig.api.event.v1.";

    @Test
    void modPages() throws Exception {
        fields(type(CONFIG + "ConfigManager"), "initializedConfigs");
        fields(type(CONFIG + "Config"), "tree", "id");
        type(CONFIG + "Node").getMethod("getMetadata", String.class);
        type("org.polyfrost.oneconfig.api.ui.v1.OneConfigUI").getMethod("createScreen", Object.class);
        type("org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute")
                .getConstructor(String.class, String.class, String.class);
    }

    @Test
    void events() throws Exception {
        Class<?> manager = type(EVENT + "EventManager");
        Class<?> handler = type(EVENT + "invoke.EventHandler");
        fields(manager, "handlers");
        manager.getMethod("register", handler);
        manager.getMethod("unregister", handler);
        // to run the handlers of a mod it starts late, highest priority first
        handler.getMethod("getEventClass");
        handler.getMethod("getPriority");
        handler.getMethod("handle", type(EVENT + "events.Event"));
        type(EVENT + "events.InitializationEvent").getField("INSTANCE");
    }

    // not initialized, as most of these need a running game for that
    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, HotwireContractTest.class.getClassLoader());
    }

    private static void fields(Class<?> type, String... names) {
        for (String name : names) assertDoesNotThrow(() -> type.getDeclaredField(name), type.getName() + "." + name);
    }
}
