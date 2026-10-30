package org.polyfrost.oneconfig.api.platform.v1.internal;

import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.platform.v1.CompatibilityPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Options;

//? if >= 1.21.4 {
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
//?}

//? if > 1.8.9 {
import net.minecraft.SharedConstants;
//?}

//? if < 1.21.4 {
/*import net.kyori.adventure.platform.fabric.FabricClientAudiences;
*///?}

public class CompatibilityPlatformImpl implements CompatibilityPlatform {
    Options options = new OptionsImpl();
    Keys keys = new KeysImpl();

    @Override
    public void displayChatMessage(Component text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> displayChatMessage(text));
            return;
        }
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
                        metadata.getDescription(),
                        mod.getContainingMod().map(parent -> parent.getMetadata().getId()).orElseGet(() -> modMenuParent(metadata))
                    );
                }).collect(Collectors.toSet());
        //? }

        //? neoforge {
        //? }
    }

    //? fabric || ornithe {
    /** The parent Mod Menu lists a separately shipped mod under: its "parent" custom value, a bare id or an object with an id, or Fabric API for its modules */
    private static String modMenuParent(ModMetadata metadata) {
        try {
            CustomValue parent = metadata.containsCustomValue("modmenu")
                ? metadata.getCustomValue("modmenu").getAsObject().get("parent")
                : null;
            if (parent != null && parent.getType() == CustomValue.CvType.OBJECT) parent = parent.getAsObject().get("id");
            if (parent != null && parent.getType() == CustomValue.CvType.STRING) return parent.getAsString();
        } catch (ClassCastException ignored) {
            // malformed metadata from another mod should not break the mod list
        }
        // Mod Menu files Fabric API modules under Fabric API itself even when they ship as separate jars
        if (metadata.containsCustomValue("fabric-api:module-lifecycle")) {
            FabricLoader loader = FabricLoader.getInstance();
            String parent = loader.isModLoaded("fabric-api") || !loader.isModLoaded("fabric") ? "fabric-api" : "fabric";
            return parent.equals(metadata.getId()) ? null : parent;
        }
        return null;
    }
    //? }

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
        //? fabric || ornithe
        return "fabric";
        //? neoforge
        //return "neoforge";
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
