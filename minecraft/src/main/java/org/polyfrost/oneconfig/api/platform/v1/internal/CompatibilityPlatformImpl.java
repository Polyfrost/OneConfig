package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.fabricmc.loader.api.FabricLoader;
//? if >=1.21.4 {
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
//?} else {
/*import net.kyori.adventure.platform.fabric.FabricClientAudiences;
*///?}
import net.kyori.adventure.text.Component;
//? if > 1.8.9
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.platform.v1.CompatibilityPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Options;

import java.util.Set;
import java.util.stream.Collectors;

public class CompatibilityPlatformImpl implements CompatibilityPlatform {
    Options options = new OptionsImpl();
    Keys keys = new KeysImpl();

    @Override
    public void displayChatMessage(Component text) {
        //? if >=1.21.4 {
        MinecraftClientAudiences.of().audience().sendMessage(text);
        //?} else {
        /*FabricClientAudiences.of().audience().sendMessage(text);
        *///?}
    }

    @Override
    public Set<ModInfo> getMods() {
        //? fabric || ornithe {

        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> {
                    var metadata = mod.getMetadata();
                    return new ModInfo(
                        metadata.getId(),
                        metadata.getName(),
                        metadata.getVersion().getFriendlyString(),
                        mod.getRootPaths().getFirst(),
                        metadata.getIconPath(Integer.MAX_VALUE).orElse(null),
                        metadata.getAuthors().stream()
                                .map(person -> person.getName())
                                .filter(name -> !name.isBlank())
                                .collect(Collectors.joining(", ")),
                        metadata.getContributors().stream()
                                .map(person -> person.getName())
                                .filter(name -> !name.isBlank())
                                .collect(Collectors.joining(", ")),
                        metadata.getDescription()
                    );
                }).collect(Collectors.toSet());
        //? }

        //? neoforge {
        //? }
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String version() {
        //? if > 1.8.9 {
        //~ if >= 1.21.8 '.getName()' -> '.name()'
        return SharedConstants.getCurrentVersion().name();
        //?} else
        //return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public String loader() {
        //? fabric
        return "fabric";
        //? neoforge
        //return "neoforge";
        //? ornithe
        //return "ornithe";
    }

    @Override
    public Options options() {
        return options;
    }

    @Override
    public Keys keys() {
        return keys;
    }

    @Override
    public long windowHandle() {
        //~ if >= 1.21.10 '.getWindow();' -> '.handle();'
        return Minecraft.getInstance().getWindow().handle();
    }

    @Override
    public int fps() {
        return Minecraft.getInstance().getFps();
    }

    @Override
    public String resolveComponent(Component component) {
        //? if >=1.21.4 {
        var minecraftComponent = MinecraftClientAudiences.of().asNative(component);
        //?} else {
        /*var minecraftComponent = FabricClientAudiences.of().toNative(component);
        *///?}
        return minecraftComponent.getString();
    }

    @Override
    public Object wrapPlatformComponent(Object component) {
        if (component instanceof net.minecraft.network.chat.Component comp) {
            //? if >=1.21.4 {
            return MinecraftClientAudiences.of().asAdventure(comp);
            //?} else {
            /*return comp.asComponent();
            *///?}
        }
        return component;
    }
}
