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

import net.minecraft.client.KeyboardListener;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.KeyInputEvent;
import org.polyfrost.oneconfig.api.events.event.RawKeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardListener.class)
public class KeyboardMixin {

    @ModifyVariable(method = "onKeyEvent", at = @At(value = "STORE"), ordinal = 0)
    private boolean onKeyEvent(boolean original, long windowPointer, int key, int scanCode, int action, int modifiers) {
        EventManager.INSTANCE.post(new RawKeyEvent(key, (char) 0, action));
        return original;
    }

    @ModifyVariable(method = "onCharEvent", at = @At(value = "STORE"), ordinal = 0)
    private boolean onKeyEvent(boolean original, long windowPointer, char key, int code, int modifiers) {
        EventManager.INSTANCE.post(new RawKeyEvent(0, key, 1));
        return original;
    }

    @Inject(method = "onKeyEvent", at = @At(
            //#if FORGE
            value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;fireKeyInput(IIII)V", remap = false
            //#else
            //$$ "TAIL"
            //#endif
    ), remap = true)
    private void onKeyInputEvent(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent());
    }
}
