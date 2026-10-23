package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.1 {
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
//?}

//? if >= 1.21.4 {
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
//?}

//? if > 1.8.9 {
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.MessageSignature;
//?}

//? if < 26.1 && > 1.8.9 {
/*import net.minecraft.client.GuiMessageTag;
*///?}

//? if < 1.21.4 {
/*import net.kyori.adventure.platform.fabric.FabricClientAudiences;
*///?}

//? if = 1.8.9 {
/*import net.minecraft.client.gui.chat.ChatGui;
import net.minecraft.text.Text;
*///?}

//~ if = 1.8.9 'ChatComponent' -> 'ChatGui'
@Mixin(ChatComponent.class)
public abstract class Mixin_ChatReceiveEvent {
    @Unique private static volatile boolean ocfg$warned;
    @Unique private ChatEvent.Receive ocfg$chatEvent;
    //~ if = 1.8.9 'Component' -> 'Text'
    @Unique private Component ocfg$nativeMessage;
    @Unique private net.kyori.adventure.text.Component ocfg$postedMessage;

    @Inject(
            //? if >= 26.1 {
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            //?} elif > 1.8.9 {
            /*method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            *///?} else
            //method = "addMessage(Lnet/minecraft/text/Text;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    //? if >= 26.1 {
    public void chatReceiveCallback(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
    //?} elif > 1.8.9 {
    /*public void chatReceiveCallback(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
    *///?} else
    //private void chatReceiveCallback(Text message, int id, CallbackInfo ci) {
        if (ocfg$post(message)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            //? if >= 26.1 {
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            //?} elif > 1.8.9 {
            /*method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            *///?} else
            //method = "addMessage(Lnet/minecraft/text/Text;I)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    //~ if = 1.8.9 'Component' -> 'Text'
    public Component modifyMessage(Component message) {
        return ocfg$consume(message);
    }

    @Unique
    //~ if = 1.8.9 'Component' -> 'Text'
    private boolean ocfg$post(Component message) {
        ocfg$chatEvent = null;
        ocfg$nativeMessage = message;
        ocfg$postedMessage = null;

        net.kyori.adventure.text.Component adventure;
        try {
            adventure = ocfg$toAdventure(message);
        } catch (Throwable t) {
            ocfg$warn("Failed to convert a chat message for ChatEvent.Receive; passing it through unchanged", t);
            return false;
        }

        ChatEvent.Receive event = new ChatEvent.Receive(adventure);
        EventManager.INSTANCE.post(event);

        ocfg$chatEvent = event;
        ocfg$nativeMessage = message;
        ocfg$postedMessage = adventure;
        return event.cancelled;
    }

    /**
     * Applies the message the listeners left behind then drops the references so a later call cannot
     * pick up a stale event if the two HEAD injectors apply in the opposite order
     */
    @Unique
    //~ if = 1.8.9 'Component' -> 'Text'
    private Component ocfg$consume(Component message) {
        ChatEvent.Receive event = ocfg$chatEvent;
        net.kyori.adventure.text.Component posted = ocfg$postedMessage;
        //~ if = 1.8.9 'Component' -> 'Text'
        Component original = ocfg$nativeMessage;
        ocfg$chatEvent = null;
        ocfg$postedMessage = null;
        ocfg$nativeMessage = null;

        if (event == null) return message;
        net.kyori.adventure.text.Component result = event.getMessage();
        if (result == posted || result.equals(posted)) return original != null ? original : message;
        try {
            return ocfg$toNative(result);
        } catch (Throwable t) {
            ocfg$warn("Failed to convert a chat message edited by a ChatEvent.Receive listener", t);
            return original != null ? original : message;
        }
    }

    @Unique
    private static Logger ocfg$logger() {
        return LogManager.getLogger("OneConfig/Chat");
    }

    @Unique
    private static void ocfg$warn(String message, Throwable t) {
        if (ocfg$warned) {
            ocfg$logger().debug(message, t);
        } else {
            ocfg$warned = true;
            ocfg$logger().warn(message + " (further failures are only logged at debug level)", t);
        }
    }

    @Unique
    //~ if = 1.8.9 'Component component' -> 'Text text'
    private static net.kyori.adventure.text.Component ocfg$toAdventure(Component component) {
        //? if >= 26.1 {
        com.google.gson.JsonElement tree = net.minecraft.network.chat.ComponentSerialization.CODEC
                .encodeStart(ocfg$registries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), component)
                .getOrThrow(com.google.gson.JsonParseException::new);
        ocfg$stripColorAlpha(tree);
        return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserializeFromTree(tree);
        //?} elif >=1.21.4 {
        /*return MinecraftClientAudiences.of().asAdventure(component);
        *///?} elif > 1.8.9 {
        /*return component.asComponent();
        *///?} else
        //return FabricClientAudiences.of().asAdventure(text);
    }

    //? if >= 26.1 {
    @Unique
    private static net.minecraft.core.RegistryAccess ocfg$registries() {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }

    @Unique
    private static void ocfg$stripColorAlpha(com.google.gson.JsonElement element) {
        if (element.isJsonArray()) {
            for (com.google.gson.JsonElement child : element.getAsJsonArray()) {
                ocfg$stripColorAlpha(child);
            }
        } else if (element.isJsonObject()) {
            com.google.gson.JsonObject object = element.getAsJsonObject();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
                com.google.gson.JsonElement value = entry.getValue();
                if ("color".equals(entry.getKey()) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    String color = value.getAsString();
                    if (color.length() == 9 && color.charAt(0) == '#') {
                        entry.setValue(new com.google.gson.JsonPrimitive("#" + color.substring(3)));
                    }
                } else {
                    ocfg$stripColorAlpha(value);
                }
            }
        }
    }
    //?}

    @Unique
    //~ if = 1.8.9 'static Component' -> 'static Text'
    private static Component ocfg$toNative(net.kyori.adventure.text.Component component) {
        //? if >=1.21.4 {
        return MinecraftClientAudiences.of().asNative(component);
        //?} else {
        /*return FabricClientAudiences.of().toNative(component);
        *///?}
    }
}
