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
 * - Refactored to use MCP mappings.
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
package cc.polyfrost.oneconfig.utils.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;

public interface FabricClientCommandSource extends ISuggestionProvider {
    /**
     * Sends a feedback message to the player.
     *
     * @param message the feedback message
     */
    void sendFeedback(ITextComponent message);

    /**
     * Sends an error message to the player.
     *
     * @param message the error message
     */
    void sendError(ITextComponent message);

    /**
     * Gets the client instance used to run the command.
     *
     * @return the client
     */
    Minecraft getClient();

    /**
     * Gets the player that used the command.
     *
     * @return the player
     */
    ClientPlayerEntity getPlayer();

    /**
     * Gets the entity that used the command.
     *
     * @return the entity
     */
    default Entity getEntity() {
        return getPlayer();
    }

    /**
     * Gets the position from where the command has been executed.
     *
     * @return the position
     */
    default Vector3d getPosition() {
        return getPlayer().getPositionVec();
    }

    /**
     * Gets the rotation of the entity that used the command.
     *
     * @return the rotation
     */
    default Vector2f getRotation() {
        return getPlayer().getPitchYaw();
    }

    /**
     * Gets the world where the player used the command.
     *
     * @return the world
     */
    ClientWorld getWorld();

    /**
     * Gets the meta property under {@code key} that was assigned to this source.
     *
     * <p>This method should return the same result for every call with the same {@code key}.
     *
     * @param key the meta key
     * @return the meta
     */
    default Object getMeta(String key) {
        return null;
    }
}