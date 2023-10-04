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

package org.polyfrost.oneconfig.internal.mixin;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.HelpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Taken from the Fabric API under the <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>.
 * Source: <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/main/java/net/fabricmc/fabric/mixin/command/HelpCommandAccessor.java">click here</a>
 */
@Mixin(HelpCommand.class)
public interface HelpCommandAccessor {
    @Accessor("FAILED_EXCEPTION")
    static SimpleCommandExceptionType getFailedException() {
        throw new AssertionError("mixin");
    }
}
