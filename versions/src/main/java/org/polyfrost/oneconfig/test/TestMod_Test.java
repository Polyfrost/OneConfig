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

package org.polyfrost.oneconfig.test;

import org.polyfrost.oneconfig.events.EventManager;
import org.polyfrost.oneconfig.events.event.InitializationEvent;
import org.polyfrost.oneconfig.internal.config.ConfigManager;
import org.polyfrost.oneconfig.libs.eventbus.Subscribe;
import org.polyfrost.oneconfig.api.commands.CommandManager;

//#if MC<=11202 && FORGE==1
@net.minecraftforge.fml.common.Mod(modid = "oneconfig-test-mod", name = "Test Mod", version = "0")
//#endif
public class TestMod_Test
//#if FABRIC==1
//$$ implements net.fabricmc.api.ClientModInitializer
//#endif
{
    public TestMod_Test() {
        EventManager.INSTANCE.register(this);
    }

    //#if FABRIC==1
    //$$ @Override
    //$$ public void onInitializeClient()
    //#else
    @Subscribe
    public void init(InitializationEvent e)
    //#endif
    {
        CommandManager.INSTANCE.registerCommand(new TestCommand_Test());
        ConfigManager.INSTANCE.registerConfig(new TestConfig_Test());
    }
}