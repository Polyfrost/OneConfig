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

package org.polyfrost.oneconfig.internal.libs.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;

/**
 * Fabric Client-side command manager implementation.
 * Extensions to {@link ISuggestionProvider} for client-sided commands.
 * <br>
 * Taken from the Fabric API under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a>;
 * <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/api/client/command/v2/FabricClientCommandSource.java">Click here for source</a>
 */
@SuppressWarnings("unused")
public interface ClientCommandSource extends ISuggestionProvider {
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