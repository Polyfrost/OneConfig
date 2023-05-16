/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.RawMouseEvent;
import cc.polyfrost.oneconfig.events.event.MouseInputEvent;
import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class MouseMixin {
    //#if FORGE
    @Inject(method = "mouseButtonCallback", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;onRawMouseClicked(III)Z", remap = false), remap = true)
    private void onMouse(long handle, int button, int action, int mods, CallbackInfo ci) {
        EventManager.INSTANCE.post(new RawMouseEvent(button, action));
    }
    //#else
    //$$ @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "onMouseButton", at = @At("STORE"), ordinal = 0)
    //$$ private int onMouse(int button, long handle, int b, int action, int mods) {
    //$$      EventManager.INSTANCE.post(new RawMouseEvent(button, action));
    //$$      return button;
    //$$  }
    //#endif

    @Inject(method = "mouseButtonCallback", at = @At(
            //#if FORGE
            value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;fireMouseInput(III)V", remap = false
            //#else
            //$$ "TAIL"
            //#endif
    ), remap = true)
    private void onMouseInput(long handle, int button, int action, int mods, CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(button));
    }
}
