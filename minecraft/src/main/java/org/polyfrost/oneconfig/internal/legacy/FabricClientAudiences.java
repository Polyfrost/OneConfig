package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.text.Text;

import java.util.Map;

public final class FabricClientAudiences {
    private static final FabricClientAudiences INSTANCE = new FabricClientAudiences();
    private static final GsonComponentSerializer LEGACY_JSON = GsonComponentSerializer.colorDownsamplingGson();
    private final Audience audience = new Audience() {
        @Override
        public void sendMessage(Component message) {
            Minecraft.getInstance().gui.getChat().addMessage(toNative(message));
        }
    };

    private FabricClientAudiences() {
    }

    public static FabricClientAudiences of() {
        return INSTANCE;
    }

    public Audience audience() {
        return this.audience;
    }

    public Text toNative(Component component) {
        JsonElement json = LEGACY_JSON.serializeToTree(component);
        try {
            return Text.Serializer.fromJson(json.toString());
        } catch (JsonParseException e) {
            removeUnparsableHovers(json);
            return Text.Serializer.fromJson(json.toString());
        }
    }

    private static void removeUnparsableHovers(JsonElement json) {
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                removeUnparsableHovers(element);
            }
        } else if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                removeUnparsableHovers(entry.getValue());
            }
            JsonElement hover = object.get("hoverEvent");
            if (hover != null && hover.isJsonObject() && hover.getAsJsonObject().has("value")) {
                try {
                    Text.Serializer.fromJson(hover.getAsJsonObject().get("value").toString());
                } catch (JsonParseException e) {
                    object.remove("hoverEvent");
                }
            }
        }
    }

    public Component asAdventure(Text component) {
        if (component instanceof net.minecraft.network.chat.Component) {
            return ((net.minecraft.network.chat.Component) component).asComponent();
        }
        return LEGACY_JSON.deserialize(Text.Serializer.toJson(component));
    }
}
*///?}
