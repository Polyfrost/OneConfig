/* This file contains an adaptation of code from Fabric API (FabricMC/fabric)
 * Project found at <https://github.com/FabricMC/fabric/tree/1.18.2/>
 * For the avoidance of doubt, this file is still licensed under the terms
 * of OneConfig's Licensing.
 *
 *                 LICENSE NOTICE FOR ADAPTED CODE
 *
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Significant changes (as required by the Apache 2.0):
 * - Removed Fabric "help" command.
 * - Removed unnecessary Fabric event for command registration.
 * - Renamed some classes that use Mixin to use MCP mappings instead.
 *
 * As per the terms of the Apache 2.0 License, a copy of the License
 * is found at `src/main/resources/licenses/Fabric-License.txt`.
 */

/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost and FabricMC.
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
package cc.polyfrost.oneconfig.internal.mixin.commands;

import cc.polyfrost.oneconfig.utils.commands.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientCommandSource.class)
public abstract class ClientSuggestionProviderMixin implements FabricClientCommandSource {
    @Shadow
    @Final
    private MinecraftClient client;

    @Override
    public void sendFeedback(Text message) {
        client.inGameHud.addChatMessage(MessageType.SYSTEM, message, Util.NIL_UUID);
    }

    @Override
    public void sendError(Text message) {
        client.inGameHud.addChatMessage(MessageType.SYSTEM, new LiteralText("").append(message).formatted(Formatting.RED), Util.NIL_UUID);
    }

    @Override
    public MinecraftClient getClient() {
        return client;
    }

    @Override
    public ClientPlayerEntity getPlayer() {
        return client.player;
    }

    @Override
    public ClientWorld getWorld() {
        return client.world;
    }
}