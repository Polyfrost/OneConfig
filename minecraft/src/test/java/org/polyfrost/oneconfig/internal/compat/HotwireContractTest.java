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

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Released builds of Hotwire reach these names by reflection to take a mod's objects out of OneConfig and put them
 * back, and stop doing so without a word when one is missing. Renaming one needs a Hotwire that knows the new name.
 */
class HotwireContractTest {
    private static final String CONFIG = "org.polyfrost.oneconfig.api.config.v1.";
    private static final String HUD = "org.polyfrost.oneconfig.api.hud.v1.";
    private static final String UI = "org.polyfrost.oneconfig.internal.ui.";

    @Test
    void configManager() throws Exception {
        Class<?> manager = type(CONFIG + "ConfigManager");
        Class<?> tree = type(CONFIG + "Tree");
        manager.getMethod("active");
        manager.getMethod("trees");
        manager.getMethod("save", tree);
        manager.getMethod("initialize");
        fields(manager, "initialized", "pendingInitialization", "initializedConfigs");
        tree.getMethod("getID");
        fields(tree, "theMap");
        fields(type(CONFIG + "Property$Field"), "owner");
        fields(type(CONFIG + "Config"), "tree", "id");
        type(CONFIG + "Node").getMethod("getMetadata", String.class);
    }

    @Test
    void hudManager() throws Exception {
        Class<?> manager = type(HUD + "HudManager");
        manager.getField("INSTANCE");
        fields(manager, "activeInstances", "hudProviders");
        manager.getMethod("invalidate");
        manager.getMethod("getRevision");
        manager.getDeclaredMethod("setRevision", int.class);
        manager.getDeclaredMethod("notifyRegistrationChanged");
        methods(manager, "getInstancesRevision");
        Class<?> hud = type(HUD + "Hud");
        hud.getMethod("getRuntimeOrNull");
        methods(hud, "set_runtime$hud", "setLastDraw$hud");
        type(HUD + "OneConfigHudCompat").getMethod("getWrapper").getReturnType().getMethod("getModId");
    }

    @Test
    void hudEditor() throws Exception {
        Class<?> editor = type(UI + "compose.impls.HudEditorUIScreen");
        fields(editor, "instance");
        editor.getSuperclass().getDeclaredMethod("disposeScene");
    }

    @Test
    void modCards() throws Exception {
        Class<?> registry = type(UI + "api.ConfigRegistry");
        Class<?> card = type(UI + "api.ConfigData");
        registry.getField("INSTANCE");
        registry.getMethod("getConfigs");
        registry.getMethod("findTree", String.class);
        registry.getMethod("register", card);
        registry.getMethod("unregister", String.class);
        card.getMethod("getId");
        type("org.polyfrost.oneconfig.api.ui.v1.OneConfigUI").getMethod("createScreen", Object.class);
        type(UI + "navigation.graph.ModConfigRoute").getConstructor(String.class, String.class, String.class);
    }

    @Test
    void events() throws Exception {
        Class<?> manager = type("org.polyfrost.oneconfig.api.event.v1.EventManager");
        Class<?> handler = type("org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler");
        fields(manager, "handlers");
        manager.getMethod("register", handler);
        manager.getMethod("unregister", handler);
        // to run the handlers of a mod it starts late
        handler.getMethod("getEventClass");
        handler.getMethod("handle", type("org.polyfrost.oneconfig.api.event.v1.events.Event"));
        type("org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent").getField("INSTANCE");
    }

    // not initialized, as most of these need a running game for that
    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, HotwireContractTest.class.getClassLoader());
    }

    private static void fields(Class<?> type, String... names) {
        for (String name : names) assertDoesNotThrow(() -> type.getDeclaredField(name), type.getName() + "." + name);
    }

    private static void methods(Class<?> type, String... names) {
        for (String name : names) {
            assertTrue(Arrays.stream(type.getMethods()).map(Method::getName).anyMatch(name::equals), type.getName() + "." + name);
        }
    }
}
