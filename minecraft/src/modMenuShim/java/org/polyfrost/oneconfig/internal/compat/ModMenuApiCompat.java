package org.polyfrost.oneconfig.internal.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import kotlin.Unit;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModMenuApiCompat {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/ModMenuApiCompat");

    private ModMenuApiCompat() {
    }

    public static void enable() {
        if (CompatLoader.INSTANCE.hasMod("modmenu")) return;
        CompatLoader.INSTANCE.requireTranslations(-1000, true, () -> {
            preloadFactories();
            return Unit.INSTANCE;
        });
    }

    private static void preloadFactories() {
        collectFactories().forEach((modId, factory) -> {
            if (CompatLoader.INSTANCE.getNativeLoadedConfigs().contains(modId) || hasRegisteredTree(modId)) return;
            Screen screen = createScreen(modId, factory);
            if (screen != null && !CompatLoader.INSTANCE.getNativeLoadedConfigs().contains(modId) && !hasRegisteredTree(modId)) {
                registerFallbackTree(modId, factory, screen);
            }
        });
    }

    private static Map<String, ConfigScreenFactory<?>> collectFactories() {
        Map<String, ConfigScreenFactory<?>> providedFactories = new LinkedHashMap<>();
        Map<String, ConfigScreenFactory<?>> ownFactories = new LinkedHashMap<>();

        Iterable<EntrypointContainer<ModMenuApi>> containers;
        try {
            containers = FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to load Mod Menu API entrypoints", throwable);
            return Map.of();
        }

        for (EntrypointContainer<ModMenuApi> container : containers) {
            ModMenuApi api;
            try {
                api = container.getEntrypoint();
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to initialize Mod Menu API entrypoint {}", container.getDefinition(), throwable);
                continue;
            }

            try {
                api.getProvidedConfigScreenFactories().forEach((modId, factory) -> {
                    if (factory != null && isLoadedMod(modId)) {
                        providedFactories.putIfAbsent(modId, factory);
                    }
                });
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to collect provided Mod Menu factories from {}", container.getDefinition(), throwable);
            }

            ConfigScreenFactory<?> ownFactory;
            try {
                ownFactory = api.getModConfigScreenFactory();
            } catch (Throwable throwable) {
                ownFactory = null;
            }
            String providerId = container.getProvider().getMetadata().getId();
            if (ownFactory != null && isLoadedMod(providerId)) {
                ownFactories.put(providerId, ownFactory);
            }
        }

        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>(providedFactories);
        factories.putAll(ownFactories);
        return factories;
    }

    private static Screen createScreen(String modId, ConfigScreenFactory<?> factory) {
        try {
            Screen[] screen = new Screen[1];
            CompatLoader.INSTANCE.withForcedModId(modId, () -> {
                screen[0] = factory.create(Platform.screen().current());
                return Unit.INSTANCE;
            });
            return screen[0];
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to create Mod Menu config screen for {}", modId, throwable);
            return null;
        }
    }

    private static void registerFallbackTree(String modId, ConfigScreenFactory<?> factory, Screen screen) {
        ModInfo mod = findLoadedMod(modId);
        Tree tree = Tree.tree();
        tree.setID(modId);
        tree.setTitle(mod != null && !mod.getName().isBlank() ? mod.getName() : screen.getTitle());
        tree.addMetadata("description", "(Mod Menu API Compat)");
        if (mod != null) {
            String iconPath = mod.extractIconFile();
            if (iconPath != null) {
                tree.addMetadata("icon_path", iconPath);
            }
        }
        tree.addMetadata("on_click", (Runnable) () -> {
            Screen next = createScreen(modId, factory);
            if (next != null) {
                Platform.screen().display(next);
            }
        });
        tree.addMetadata(Backend.UI_ONLY_METADATA, true);
        ConfigManager.active().register(tree);
    }

    private static boolean hasRegisteredTree(String modId) {
        return ConfigManager.active().trees().stream().anyMatch(tree -> modId.equals(tree.getID()));
    }

    private static boolean isLoadedMod(String modId) {
        return findLoadedMod(modId) != null;
    }

    private static ModInfo findLoadedMod(String modId) {
        return ModInfo.getLoadedMods().stream().filter(mod -> modId.equals(mod.getId())).findFirst().orElse(null);
    }
}
