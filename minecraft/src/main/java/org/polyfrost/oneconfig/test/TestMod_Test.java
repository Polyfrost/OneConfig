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

package org.polyfrost.oneconfig.test;

import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;

public final class TestMod_Test {
    public static void initialize() {
        EventManager.register(InitializationEvent.class, () -> {
            System.err.println("TestMod::init");
            CommandManager.register(new TestCommand_Test());
            TestConfig_Test config = TestConfig_Test.getInstance();
            if (config.getTree() == null) {
                config.preload();
            }
            ConfigRegistry.INSTANCE.registerTree(config.getTree(), ConfigSource.OC);
            TestKtConfig.INSTANCE.preload();
            ConfigRegistry.INSTANCE.registerTree(TestKtConfig.INSTANCE.getTree(), ConfigSource.OC);

            HudManager.register(new TestLegacyHud_Test(), "test_mod", "combat");
            HudManager.register(new TestItemHud_Test(), "test_mod");
        });
    }
}
