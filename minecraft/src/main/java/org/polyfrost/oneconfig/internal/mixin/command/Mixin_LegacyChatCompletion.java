package org.polyfrost.oneconfig.internal.mixin.command;

//? if = 1.8.9 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.polyfrost.oneconfig.internal.legacy.command.ClientCommandInternals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;

// Apply above default mixin priority
@Mixin(value = ChatScreen.class, priority = 1100)
public abstract class Mixin_LegacyChatCompletion {
    @Shadow
    private boolean completed;

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    @Nullable
    private ArrayDeque<CompletableFuture<String[]>> ocfg$clientSuggestions;

    @Unique
    @Nullable
    private ArrayDeque<String> ocfg$suggestionInputs;

    @Unique
    @Nullable
    private ArrayDeque<Integer> ocfg$suggestionRequests;

    @Unique
    private int ocfg$suggestionRequest;

    @Inject(
            method = "goThroughHistory(Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/ChatScreen;completed:Z",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void ocfg$prepareClientSuggestions(String text, String cursor, CallbackInfo ci) {
        if (ocfg$clientSuggestions == null) {
            ocfg$clientSuggestions = new ArrayDeque<>();
            ocfg$suggestionInputs = new ArrayDeque<>();
            ocfg$suggestionRequests = new ArrayDeque<>();
        }

        ocfg$suggestionRequest++;
        ocfg$clientSuggestions.addLast(ClientCommandInternals.getCompletions(text));
        ocfg$suggestionInputs.addLast(text);
        ocfg$suggestionRequests.addLast(ocfg$suggestionRequest);
    }

    @WrapMethod(method = "setMessageHistory")
    private void ocfg$addClientSuggestions(String[] serverSuggestions, Operation<Void> original) {
        if (ocfg$clientSuggestions == null || ocfg$clientSuggestions.isEmpty()) {
            original.call((Object) serverSuggestions);
            return;
        }

        CompletableFuture<String[]> future = ocfg$clientSuggestions.removeFirst();
        String input = ocfg$suggestionInputs.removeFirst();
        int request = ocfg$suggestionRequests.removeFirst();
        future.whenCompleteAsync((clientSuggestions, throwable) -> {
            //noinspection ConstantValue
            if (request != ocfg$suggestionRequest || !completed || Minecraft.getInstance().screen != (Object) this) return;
            int cursor = chatField.getCursor();
            if (cursor != input.length() || !chatField.getText().startsWith(input)) return;
            if (throwable != null) clientSuggestions = new String[0];
            original.call((Object) ClientCommandInternals.mergeSuggestions(serverSuggestions, clientSuggestions));
        }, Minecraft.getInstance()::executeTask);
    }
}
*///?}
