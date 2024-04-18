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

//#if FORGE
package org.polyfrost.oneconfig.internal.mixin;

// guys MAKE UP YOUR FUCKING MINDS like what?? three times consecutively??
// i swear if i have to change this for 1.20 as well im going to start throwing hands
//#if MC<11700
import net.minecraftforge.fml.client.ClientModLoader;
//#else
//#if MC<11900
//$$ import net.minecraftforge.fmlclient.ClientModLoader;
//#else
//$$ import net.minecraftforge.client.loading.ClientModLoader;
//#endif
//#endif
import org.polyfrost.oneconfig.internal.OneConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientModLoader.class, remap = false)
public class ClientModLoaderMixin {

    @Inject(method = "finishModLoading", at = @At(value = "TAIL"), remap = false)
    private static void onFinishModLoading(CallbackInfo ci) {
        OneConfig.INSTANCE.init();
    }
}
//#endif