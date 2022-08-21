package cc.polyfrost.oneconfig.internal.mixin.commands;

import cc.polyfrost.oneconfig.utils.commands.ClientCommandHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "method_908", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ChatScreen;client:Lnet/minecraft/client/MinecraftClient;", ordinal = 0))
    private void addAutoComplete(String string, String string2, CallbackInfo ci) {
        ClientCommandHandler.instance.autoComplete(string);
    }

    @ModifyVariable(method = "setSuggestions", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = At.Shift.AFTER), argsOnly = true, index = 1)
    private String[] addAutoComplete(String[] suggestions) {
        String[] complete = ClientCommandHandler.instance.latestAutoComplete;
        if (complete != null) {
            return com.google.common.collect.ObjectArrays.concat(complete, suggestions, String.class);
        }
        return suggestions;
    }

    @Redirect(method = "setSuggestions", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/StringUtils;getCommonPrefix([Ljava/lang/String;)Ljava/lang/String;"))
    private String removeFormatting(String[] strs) {
        return Formatting.strip(StringUtils.getCommonPrefix(strs));
    }
}
