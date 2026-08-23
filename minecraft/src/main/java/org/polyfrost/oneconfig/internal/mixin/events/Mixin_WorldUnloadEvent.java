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

package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_WorldUnloadEvent {

    @Shadow public ClientLevel level;

    @Inject(
            method = "setLevel"
            , at = @At("HEAD"))
    private void onWorldUnloadCallback(CallbackInfo ci) {
        oneconfig$postWorldUnload();
    }

    //? if >= 1.21.11 {
    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V"
            , at = @At("HEAD"))
    //?} else {
    /*@Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"
            , at = @At("HEAD"))
    *///?}
    private void onDisconnectCallback(CallbackInfo ci) {
        oneconfig$postWorldUnload();
    }

    @Inject(
            method = "clearClientLevel"
            , at = @At("HEAD"))
    private void onClearClientLevelCallback(CallbackInfo ci) {
        oneconfig$postWorldUnload();
    }

    @Unique
    private void oneconfig$postWorldUnload() {
        if (this.level != null) {
            EventManager.INSTANCE.post(new WorldEvent.Unload(this.level));
        }
    }

}
