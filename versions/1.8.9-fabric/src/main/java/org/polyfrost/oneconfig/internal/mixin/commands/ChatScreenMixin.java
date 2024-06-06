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

import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.polyfrost.oneconfig.api.commands.v1.internal.ClientCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
//#if MC==10809
        net.minecraft.client.gui.screen.ChatScreen.class
//#else
//$$ net.minecraft.entity.ai.pathing.PathNodeMaker.class
//#endif
)
public abstract class ChatScreenMixin {
    //@formatter:off
    private static final String m_showSuggestion =
            //#if MC==10809
            "showSuggestion";
            //#else
            //$$ "method_12183";
            //#endif

    private static final String m_setSuggestions =
            //#if MC==10809
            "setSuggestions";
            //#else
            //$$ "method_12185";
            //#endif

    private static final String m_complete =
            //#if MC==10809
            "method_908";
            //#else
            //$$ "method_12184";
            //#endif
    //@formatter:on


    @ModifyArg(method = m_showSuggestion, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;write(Ljava/lang/String;)V"), index = 0)
    private String ocfg$commands$patchRemoveFormatting(String par1) {
        return Formatting.strip(par1);
    }

    @Inject(method = m_complete, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void ocfg$coammands$processAutoComplete(String string, String string2, CallbackInfo ci) {
        ClientCommandHandler.instance.autoComplete(string);
    }

    @ModifyVariable(method = m_setSuggestions, at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = At.Shift.AFTER), argsOnly = true, index = 1)
    private String[] ocfg$coammands$processAutoComplete(String[] suggestions) {
        String[] complete = ClientCommandHandler.instance.latestAutoComplete;
        if (complete != null) {
            return com.google.common.collect.ObjectArrays.concat(complete, suggestions, String.class);
        }
        return suggestions;
    }

    @Redirect(method = m_setSuggestions, at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/StringUtils;getCommonPrefix([Ljava/lang/String;)Ljava/lang/String;", remap = false))
    private String ocfg$commands$patchRemoveFormatting(String[] strs) {
        return Formatting.strip(StringUtils.getCommonPrefix(strs));
    }
}
