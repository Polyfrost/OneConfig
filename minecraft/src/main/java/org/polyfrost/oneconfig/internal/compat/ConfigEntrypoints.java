/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

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
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Tree;
//? if !neoforge {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
//?}

public final class ConfigEntrypoints {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/ConfigEntrypoints");

	private record Provider(String modId, String name, Supplier<ConfigEntrypoint> factory) {
	}

    private static List<Provider> providers;
    private static final Set<String> failed = new HashSet<>();
    private static boolean registered;
    private static final String OPEN_ORIGINAL_SCREEN = "open_original_screen";
    // tree id -> id of the mod whose entrypoint created it
    private static final Map<String, String> treeOwners = new ConcurrentHashMap<>();
    // mod id -> opener the mod gave through Context#originalScreen
    private static final Map<String, Runnable> customOpeners = new ConcurrentHashMap<>();
    private static volatile @Nullable String registeringModId;
    private static volatile boolean treesRegistered;

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
            registeringModId = provider.modId;
            try {
                provider.factory.get().registerConfigs(new Context(provider.modId));
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
    //done on registration rather than once since switching profiles rebuilds the trees
    private static void onTreeRegistered(Tree tree) {
        String id = tree.getID();
        if (id == null) return;
        treesRegistered = true;
        // trees registered directly with the ConfigManager from within the entrypoint
        String current = registeringModId;
        if (current != null) treeOwners.putIfAbsent(id, current);
        String modId = treeOwners.get(id);
        if (modId == null || tree.getMetadata(OPEN_ORIGINAL_SCREEN) != null) return;
        attachOpener(tree, modId);
    }

    private static void attachOpener(Tree tree, String modId) {
        if (failed.contains(modId)) return;
        Runnable opener = customOpeners.get(modId);
        if (opener == null) opener = CompatLoader.INSTANCE.originalScreenOpener(modId);
        if (opener != null) tree.addMetadata(OPEN_ORIGINAL_SCREEN, opener);
    }

    private static final class Context implements ConfigEntrypoint.Context {
        private final String modId;

        Context(String modId) {
            this.modId = modId;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public <T extends Config> T register(T config) {
            treeOwners.put(config.id, modId);
            // the tree normally only exists once the config initializes, where onTreeRegistered picks it up
            Tree tree = config.getTree();
            if (tree != null) attachOpener(tree, modId);
            return config;
        }

        @Override
        public void originalScreen(Runnable opener) {
            customOpeners.put(modId, opener);
            // swap the opener on trees that were already registered with the default one
            // ConfigManager.active() would set up the profiles early if nothing has been registered yet
            if (!treesRegistered) return;
            for (Tree tree : ConfigManager.active().trees()) {
                if (modId.equals(treeOwners.get(tree.getID()))) attachOpener(tree, modId);
            }
        }
    }

    private static List<Provider> providers() {
        if (providers != null) return providers;
        List<Provider> found = new ArrayList<>();
        try {
            //? if !neoforge {
            for (EntrypointContainer<ConfigEntrypoint> container : FabricLoader.getInstance().getEntrypointContainers(ConfigEntrypoint.ENTRYPOINT, ConfigEntrypoint.class)) {
                found.add(new Provider(
                        container.getProvider().getMetadata().getId(),
                        container.getDefinition(),
                        container::getEntrypoint
                ));
            }
            //?}
        } catch (Throwable t) {
            LOGGER.error("Failed to discover OneConfig config entrypoints", t);
        }
        providers = Collections.unmodifiableList(found);
        return providers;
    }
}
