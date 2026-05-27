package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.platform.v1.CompatibilityPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Options;
import org.polyfrost.oneconfig.api.platform.v1.commands.CommandPlatform;

import java.util.Set;
import java.util.stream.Collectors;

public class CompatibilityPlatformImpl implements CompatibilityPlatform {
    Options options = new OptionsImpl();

    @Override
    public void displayChatMessage(Component text) {

    }

    @Override
    public Set<ModInfo> getMods() {
        //? fabric {

        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> {
                    var metadata = mod.getMetadata();
                    return new ModInfo(metadata.getId(), metadata.getName(), metadata.getVersion().getFriendlyString(), mod.getRootPaths().getFirst());
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
        return SharedConstants.getCurrentVersion().name();
    }

    @Override
    public String loader() {
        //? fabric
        return "fabric";
        //? neoforge
        //return "neoforge";
    }

    @Override
    public Options options() {
        return options;
    }

    @Override
    public CommandPlatform commandPlatform() {
        return null;
    }

    @Override
    public Keys keys() {
        return null;
    }

    @Override
    public long windowHandle() {
        return Minecraft.getInstance().getWindow().handle();
    }
}
