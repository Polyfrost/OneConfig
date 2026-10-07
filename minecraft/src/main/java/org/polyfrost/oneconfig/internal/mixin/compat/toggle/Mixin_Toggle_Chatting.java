package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.polyfrost.chatting.config.ChattingConfig", remap = false)
public class Mixin_Toggle_Chatting {

    @Inject(method = {"getChatTabs", "getChatShortcuts", "getChatPeek", "getSmoothChat", "getSmoothScrolling", "getShowChatHeads", "getChatCopy", "getChatDelete", "getChatScreenshot", "getChatSearch", "getCustomChatWidth", "getCustomChatHeight", "getRoundedChatCorners", "getRemoveScrollBar", "getFade", "getUnderlinedLinks", "getRightClickCopy", "getExtendBG", "getChatWindowMoved"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$force1(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("chatting")) cir.setReturnValue(false);
    }
}
