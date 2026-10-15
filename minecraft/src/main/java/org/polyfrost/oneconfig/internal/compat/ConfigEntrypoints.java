package org.polyfrost.oneconfig.internal.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

public final class ConfigEntrypoints {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/ConfigEntrypoints");

	private record Provider(String modId, String name, Supplier<ConfigEntrypoint> factory) {
	}

    private static List<Provider> providers;
    private static final Set<String> failed = new HashSet<>();
    private static boolean registered;
    private static final Map<String, String> treeOwners = new ConcurrentHashMap<>();
    private static volatile @Nullable String registeringModId;

    private ConfigEntrypoints() {
    }

    public static synchronized List<String> providerModIds() {
        List<String> ids = new ArrayList<>();
        for (Provider provider : providers()) {
            if (!ids.contains(provider.modId)) ids.add(provider.modId);
        }
        return ids;
    }

    public static synchronized boolean provides(@Nullable String modId) {
        if (modId == null || failed.contains(modId)) return false;
        for (Provider provider : providers()) {
            if (modId.equals(provider.modId)) return true;
        }
        return false;
    }

    public static synchronized void registerAll() {
        if (registered) return;
        registered = true;
        if (providers().isEmpty()) return;
        ConfigManager.addTreeRegistrationListener(ConfigEntrypoints::onTreeRegistered);
        for (Provider provider : providers()) {
            Set<String> pendingBefore = ConfigManager.pendingConfigIds();
            registeringModId = provider.modId;
            try {
                provider.factory.get().registerConfigs();
                for (String id : ConfigManager.pendingConfigIds()) {
                    if (!pendingBefore.contains(id)) treeOwners.put(id, provider.modId);
                }
                LOGGER.info("Registered OneConfig config from entrypoint {} ({})", provider.name, provider.modId);
            } catch (Throwable t) {
                LOGGER.error("Mod {} provides a broken OneConfig config entrypoint {}", provider.modId, provider.name, t);
                failed.add(provider.modId);
                CompatLoader.INSTANCE.getNativeLoadedConfigs().remove(provider.modId);
            } finally {
                registeringModId = null;
            }
        }
    }

    //keep the "open original" button for configs that replaced an extracted one
    private static void onTreeRegistered(Tree tree) {
        String id = tree.getID();
        if (id == null) return;
        String current = registeringModId;
        if (current != null) treeOwners.putIfAbsent(id, current);
        String modId = treeOwners.get(id);
        if (modId == null || failed.contains(modId) || tree.getMetadata("open_original_screen") != null) return;
        Runnable opener = CompatLoader.INSTANCE.originalScreenOpener(modId);
        if (opener != null) tree.addMetadata("open_original_screen", opener);
    }

    private static List<Provider> providers() {
        if (providers != null) return providers;
        List<Provider> found = new ArrayList<>();
        try {
            for (EntrypointContainer<ConfigEntrypoint> container : FabricLoader.getInstance().getEntrypointContainers("oneconfig", ConfigEntrypoint.class)) {
                found.add(new Provider(
                        container.getProvider().getMetadata().getId(),
                        container.getDefinition(),
                        container::getEntrypoint
                ));
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to discover OneConfig config entrypoints", t);
        }
        providers = Collections.unmodifiableList(found);
        return providers;
    }
}
