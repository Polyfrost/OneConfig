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

package org.polyfrost.oneconfig.internal.mixin.commands;
//#if FABRIC==1 && MC<=11200
import org.polyfrost.oneconfig.utils.commands.ClientCommandHandler;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathNodeMaker.class)
public class ChatScreenMixin {
    @ModifyArg(method = "method_12183", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;write(Ljava/lang/String;)V"), index = 0)
    private String removeFormatting1(String par1) {
        return Formatting.strip(par1);
    }

    @Inject(method = "method_12184", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void addAutoComplete(String string, CallbackInfo ci) {
        ClientCommandHandler.instance.autoComplete(string);
    }

    @ModifyVariable(method = "method_12185", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = At.Shift.AFTER), argsOnly = true, index = 1)
    private String[] addAutoComplete(String[] suggestions) {
        String[] complete = ClientCommandHandler.instance.latestAutoComplete;
        if (complete != null) {
            return com.google.common.collect.ObjectArrays.concat(complete, suggestions, String.class);
        }
        return suggestions;
    }

    @Redirect(method = "method_12185", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/StringUtils;getCommonPrefix([Ljava/lang/String;)Ljava/lang/String;"))
    private String removeFormatting2(String[] strs) {
        return Formatting.strip(StringUtils.getCommonPrefix(strs));
    }
}
//#endif