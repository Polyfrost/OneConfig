package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.text.Text;

public final class FabricClientAudiences {
    private static final FabricClientAudiences INSTANCE = new FabricClientAudiences();
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
        return Text.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
    }

    public Component asAdventure(Text component) {
        if (component instanceof net.minecraft.network.chat.Component) {
            return ((net.minecraft.network.chat.Component) component).asComponent();
        }
        return GsonComponentSerializer.gson().deserialize(Text.Serializer.toJson(component));
    }
}
*///?}
