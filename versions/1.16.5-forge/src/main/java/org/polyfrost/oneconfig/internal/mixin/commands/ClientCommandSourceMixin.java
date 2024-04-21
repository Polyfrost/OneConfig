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

package org.polyfrost.oneconfig.internal.mixin.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Fabric Client-side command manager implementation.
 * <br>
 * Taken from the Fabric API under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a>;
 * <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/impl/command/client/ClientCommandInternals.java">Click here for source</a>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(net.minecraft.client.multiplayer.ClientSuggestionProvider.class)
abstract class ClientCommandSourceMixin implements ClientCommandSource {
    @Shadow
    @Final
    private Minecraft mc;

    @Override
    public void sendFeedback(ITextComponent message) {
        this.mc.ingameGUI.getChatGUI().printChatMessage(message);
//        this.client.getNarratorManager().narrate(message);
    }

    @Override
    public void sendError(ITextComponent message) {
        sendFeedback(
                //#if MC<11900
                new net.minecraft.util.text.StringTextComponent("")
                //#else
                //#if FORGE
                //$$ net.minecraft.network.chat.Component
                //#else
                //$$ net.minecraft.text.Text
                //#endif
                //$$ .empty()
                //#endif
                        .append(message).mergeStyle(TextFormatting.RED));
    }

    @Override
    public Minecraft getClient() {
        return this.mc;
    }

    @Override
    public ClientPlayerEntity getPlayer() {
        return this.mc.player;
    }

    @Override
    public ClientWorld getWorld() {
        return this.mc.world;
    }
}