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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
//? if !neoforge {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
//?}

public final class ConfigEntrypoints {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/ConfigEntrypoints");

    private record Provider(String modId, String name, Supplier<ConfigEntrypoint> factory) {
    }

    // modId is the mod the hook resolved, which for a nested mod is not its provider
    private record Skipped(String modId, Runnable extraction) {
    }

    private static volatile List<Provider> providers;
    private static volatile Set<String> providerIds;
    // mods nested in a provider's jar, whose classes compat resolves to the nested id rather than the provider's
    private static volatile Map<String, String> nestedIds;
    private static final Object DISCOVERY_LOCK = new Object();
    private static boolean discoveryFailureLogged;
    // mods whose original config is extracted after all, read without the lock by provides()
    private static final Set<String> failed = ConcurrentHashMap.newKeySet();
    private static final Set<String> succeeded = new HashSet<>();
    // keyed so a hook that fires more than once before the mod is decided is only retried once
    private static final Map<String, Map<Object, Skipped>> skippedExtractions = new HashMap<>();
    private static final AtomicBoolean registered = new AtomicBoolean();
    private static final String OPEN_ORIGINAL_SCREEN = "open_original_screen";
    private static final Map<String, String> treeOwners = new ConcurrentHashMap<>();
    private static final Map<String, Runnable> customOpeners = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Runnable>> defaultOpeners = new ConcurrentHashMap<>();
    // mods whose entrypoints ran but which are only decided once ConfigManager.initialize() built their configs
    private static final Map<String, Boolean> undecided = new LinkedHashMap<>();
    // per thread so trees and configs made elsewhere while an entrypoint runs are not claimed by it
    private static final ThreadLocal<String> registeringModId = new ThreadLocal<>();
    // configs of the current call whose constructor may still be running, with where that constructor sits on the stack
    private static final ThreadLocal<Map<Config, Construction>> constructing = new ThreadLocal<>();

    /**
     * @param depth    the stack depth of the outermost constructor frame, counted from the frame of register() or else the bottom
     * @param caller   the frame that called that constructor, null if it is the bottom of the stack
     * @param anchored whether the depth is counted from register() rather than the bottom
     */
    record Construction(int depth, @Nullable StackTraceElement caller, boolean anchored) {
        Construction(int depth, @Nullable StackTraceElement caller) {
            this(depth, caller, false);
        }
    }

    private ConfigEntrypoints() {
    }

    public static List<String> providerModIds() {
        return new ArrayList<>(providerIds());
    }

    public static boolean provides(@Nullable String modId) {
        modId = providerOf(modId);
        return modId != null && !failed.contains(modId) && providerIds().contains(modId);
    }

    // the provider a mod nested in its jar belongs to, or the id itself
    private static @Nullable String providerOf(@Nullable String modId) {
        if (modId == null) return null;
        providers();
        Map<String, String> nested = nestedIds;
        return nested == null ? modId : nested.getOrDefault(modId, modId);
    }

    /**
     * Skips an extraction for a mod that provides its own config, queueing it to be retried if the entrypoint
     * fails. Extractions sharing a {@code key} are only queued once.
     */
    public static synchronized boolean skipExtraction(@Nullable String modId, Object key, Runnable extraction) {
        if (!provides(modId)) return false;
        String provider = providerOf(modId);
        if (!succeeded.contains(provider)) skippedExtractions.computeIfAbsent(provider, k -> new LinkedHashMap<>()).putIfAbsent(key, new Skipped(modId, extraction));
        return true;
    }

    /**
     * The trees of the active profile that belong to a mod's config entrypoint
     */
    public static List<Tree> ownedTrees(String modId) {
        List<Tree> owned = new ArrayList<>();
        if (treeOwners.isEmpty()) return owned;
        for (Tree tree : ConfigManager.active().trees()) {
            String id = tree.getID();
            if (id != null && modId.equals(treeOwners.get(id))) owned.add(tree);
        }
        return owned;
    }

    /**
     * Runs the entrypoints, before {@link ConfigManager#initialize()} so the configs they create are initialized
     * with the rest. Whether each mod's original is extracted is decided by {@link #finishRegistration()}.
     */
    // never holds the lock while running mod code or touching CompatLoader, whose init calls back into here
    public static void registerAll() {
        if (!registered.compareAndSet(false, true)) return;
        if (providers().isEmpty()) return;
        ConfigManager.addTreeRegistrationListener(ConfigEntrypoints::onTreeRegistered);
        ConfigManager.addConfigSubmissionListener(ConfigEntrypoints::onConfigSubmitted);
        claimEarlierConfigs();
        // a mod may name more than one entrypoint, which only decide together whether its original is extracted
        Map<String, List<Provider>> byMod = new LinkedHashMap<>();
        for (Provider provider : providers()) byMod.computeIfAbsent(provider.modId, k -> new ArrayList<>()).add(provider);
        byMod.forEach((modId, entrypoints) -> undecided.put(modId, register(modId, entrypoints)));
    }

    /**
     * Decides for each mod that ran an entrypoint whether to keep its configs or extract its original. Called after
     * {@link ConfigManager#initialize()}, as a config only counts once its tree was built and registered.
     */
    // compat waits for this, so it always ends by releasing it even if deciding a mod threw
    public static void finishRegistration() {
        try {
            Map<String, Boolean> pending = new LinkedHashMap<>(undecided);
            undecided.clear();
            pending.forEach((modId, broken) -> {
                try {
                    decide(modId, broken);
                } catch (Throwable t) {
                    // a mod already marked as succeeded keeps its configs, extracting its original too would show two cards
                    if (isDecided(modId)) {
                        LOGGER.error("Failed to finish deciding the OneConfig config entrypoint of {}", modId, t);
                        return;
                    }
                    LOGGER.error("Failed to decide the OneConfig config entrypoint of {}, extracting its original config instead", modId, t);
                    fallBackToOriginal(modId);
                }
            });
            // providers registerAll never reached, such as when it threw partway, would otherwise stay skipped forever
            for (String modId : providerIds()) {
                if (pending.containsKey(modId) || isDecided(modId)) continue;
                LOGGER.warn("The OneConfig config entrypoint of {} never ran, extracting its original config instead", modId);
                try {
                    fallBackToOriginal(modId);
                } catch (Throwable t) {
                    LOGGER.error("Failed to extract the original config of {}", modId, t);
                }
            }
        } finally {
            // caught so a broken CompatLoader never hides an exception from ConfigManager.initialize()
            try {
                CompatLoader.INSTANCE.onEntrypointsDecided();
            } catch (Throwable t) {
                LOGGER.error("Failed to release the compat waiting for the OneConfig config entrypoints", t);
            }
        }
    }

    // configs a mod created before its entrypoint ran, such as during its own initializer, belong to it too,
    // otherwise a mod that forgot Context.register would show them next to its extracted original
    private static void claimEarlierConfigs() {
        Set<String> ids = providerIds();
        for (Config config : ConfigManager.pendingConfigs()) {
            ModInfo owner = CompatLoader.INSTANCE.findModByClass(config.getClass());
            String modId = owner == null ? null : providerOf(owner.getId());
            if (modId == null || !ids.contains(modId)) continue;
            treeOwners.putIfAbsent(config.id, modId);
        }
    }

    // returns whether any of the entrypoints threw
    private static boolean register(String modId, List<Provider> entrypoints) {
        boolean broken = false;
        registeringModId.set(modId);
        try {
            for (Provider provider : entrypoints) {
                Context context = new Context(modId);
                constructing.set(new IdentityHashMap<>());
                try {
                    provider.factory.get().registerConfigs(context);
                    LOGGER.info("Ran OneConfig config entrypoint {} ({})", provider.name, modId);
                } catch (Throwable t) {
                    broken = true;
                    LOGGER.error("Mod {} provides a broken OneConfig config entrypoint {}", modId, provider.name, t);
                    discardHalfBuilt(t, constructing.get());
                } finally {
                    context.open = false;
                }
            }
        } finally {
            registeringModId.remove();
            constructing.remove();
        }
        return broken;
    }

    // a config whose subclass constructor threw was already submitted by the Config base constructor
    private static void discardHalfBuilt(Throwable t, Map<Config, Construction> constructing) {
        constructing.forEach((config, construction) -> {
            if (!constructorFailed(t, config.getClass(), construction)) return;
            ConfigManager.cancelInitialization(config);
            treeOwners.remove(config.id);
            LOGGER.warn("Discarding config {} whose constructor failed", config.id);
        });
    }

    // checks the frame at the depth of this instance's constructor, as another instance of the same class may have
    // been built fine from a different depth
    // the caller frame tells apart another instance at the same depth whose constructor threw before it reached
    // Config, so it never replaced this one in the map, as long as it was constructed from another line
    static boolean constructorFailed(Throwable t, Class<?> type, Construction construction) {
        List<String> chain = constructorChain(type);
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = t; cause != null && seen.add(cause); cause = cause.getCause()) {
            StackTraceElement[] frames = cause.getStackTrace();
            // a trace cut off above register() cannot be lined up with the depth, such as a StackOverflowError's,
            // so the config is left to ConfigManager.initialize(), which skips it if it fails to initialize
            if ((registerFrame(frames) >= 0) != construction.anchored()) continue;
            int i = bottom(frames) - construction.depth();
            if (i < 0 || i >= frames.length || !"<init>".equals(frames[i].getMethodName()) || !chain.contains(frames[i].getClassName())) continue;
            StackTraceElement caller = i + 1 < frames.length ? frames[i + 1] : null;
            if (Objects.equals(caller, construction.caller())) return true;
        }
        return false;
    }

    /**
     * Where the outermost constructor frame building a config of this type sits, or null if it is not being constructed
     */
    static @Nullable Construction construction(Class<?> type, StackTraceElement[] frames) {
        int depth = constructorDepth(type, frames);
        if (depth < 0) return null;
        int caller = bottom(frames) - depth + 1;
        return new Construction(depth, caller < frames.length ? frames[caller] : null, registerFrame(frames) >= 0);
    }

    // depths are counted from the frame of register(), which sits below every construction and throw of an entrypoint,
    // as the JVM drops the bottom of stack traces deeper than MaxJavaStackTraceDepth, by a different amount for each
    // trace, falling back to the bottom of the stack when it is missing
    private static int bottom(StackTraceElement[] frames) {
        int register = registerFrame(frames);
        return register >= 0 ? register : frames.length - 1;
    }

    private static int registerFrame(StackTraceElement[] frames) {
        for (int i = 0; i < frames.length; i++) {
            if ("register".equals(frames[i].getMethodName()) && ConfigEntrypoints.class.getName().equals(frames[i].getClassName())) return i;
        }
        return -1;
    }

    // the config class and its superclasses up to Config, most derived first
    private static List<String> constructorChain(Class<?> type) {
        List<String> chain = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            chain.add(c.getName());
            if (c == Config.class) break;
        }
        return chain;
    }

    /**
     * The depth from the bottom of the stack of the outermost constructor frame building a config of this type,
     * or -1 if it is not being constructed
     */
    static int constructorDepth(Class<?> type, StackTraceElement[] frames) {
        List<String> chain = constructorChain(type);
        int outer = -1;
        int last = Integer.MAX_VALUE;
        for (int i = 0; i < frames.length; i++) {
            int index = "<init>".equals(frames[i].getMethodName()) ? chain.indexOf(frames[i].getClassName()) : -1;
            // going down the stack one construction only moves to more derived classes, anything else is another instance
            if (index >= 0 && index <= last) {
                outer = i;
                last = index;
            } else if (outer >= 0) {
                break;
            }
        }
        return outer < 0 ? -1 : bottom(frames) - outer;
    }

    private static void decide(String modId, boolean broken) {
        if (!ownedTrees(modId).isEmpty()) {
            // keep what it registered rather than also extracting the original, which would show two cards
            if (broken) LOGGER.warn("Keeping the OneConfig configs {} registered before its entrypoint failed", modId);
            markSucceeded(modId);
            // CompatLoader only marked the providers it saw when it initialized
            CompatLoader.INSTANCE.getNativeLoadedConfigs().add(modId);
            LOGGER.info("Using the OneConfig configs registered by {}", modId);
            return;
        }
        if (!broken) LOGGER.info("Mod {} registered no working OneConfig configs from its entrypoint, extracting its original config instead", modId);
        fallBackToOriginal(modId);
    }

    private static void fallBackToOriginal(String modId) {
        // un-marked before failing so a hook firing in between still sees provides() and queues itself
        try {
            CompatLoader.INSTANCE.getNativeLoadedConfigs().remove(modId);
        } finally {
            retrySkippedExtractions(modId, markFailed(modId));
        }
    }

    private static synchronized boolean isDecided(String modId) {
        return succeeded.contains(modId) || failed.contains(modId);
    }

    private static synchronized void markSucceeded(String modId) {
        succeeded.add(modId);
        skippedExtractions.remove(modId);
    }

    private static synchronized Collection<Skipped> markFailed(String modId) {
        failed.add(modId);
        Map<Object, Skipped> skipped = skippedExtractions.remove(modId);
        return skipped == null ? Collections.emptyList() : skipped.values();
    }

    private static void retrySkippedExtractions(String modId, Collection<Skipped> skipped) {
        if (skipped.isEmpty()) return;
        LOGGER.info("Retrying {} skipped config extraction(s) for {}", skipped.size(), modId);
        for (Skipped extraction : skipped) {
            try {
                CompatLoader.INSTANCE.withForcedModId(extraction.modId, () -> {
                    extraction.extraction.run();
                    return Unit.INSTANCE;
                });
            } catch (Throwable t) {
                LOGGER.error("Failed to retry a skipped config extraction for {}", modId, t);
            }
        }
    }

    //keep the "open original" button for configs that replaced an extracted one
    //done on registration rather than once since switching profiles rebuilds the trees
    private static void onTreeRegistered(Tree tree) {
        String id = tree.getID();
        if (id == null) return;
        // trees registered directly with the ConfigManager from within the entrypoint
        String current = registeringModId.get();
        if (current != null) treeOwners.putIfAbsent(id, current);
        String modId = treeOwners.get(id);
        if (modId == null || tree.getMetadata(OPEN_ORIGINAL_SCREEN) != null) return;
        attachOpener(tree, modId);
    }

    // configs created within the entrypoint belong to it even when they are not passed to Context.register,
    // otherwise a failing entrypoint would leave them next to the extracted original
    private static void onConfigSubmitted(Config config) {
        String current = registeringModId.get();
        if (current == null) return;
        // a config whose class resolves to another mod, such as one a library built when this entrypoint initialized
        // its class, belongs to that mod, otherwise it would count as this mod's and stop its original being extracted
        ModInfo owner = CompatLoader.INSTANCE.findModByClass(config.getClass());
        if (owner != null && !current.equals(providerOf(owner.getId()))) return;
        treeOwners.putIfAbsent(config.id, current);
        Map<Config, Construction> pending = constructing.get();
        if (pending == null) return;
        Construction construction = construction(config.getClass(), new Throwable().getStackTrace());
        if (construction == null) return;
        // this config is not nested in one at the same depth or deeper, so their constructors have returned
        pending.values().removeIf(c -> c.depth() >= construction.depth());
        pending.put(config, construction);
    }

    private static void attachOpener(Tree tree, String modId) {
        if (failed.contains(modId)) return;
        Runnable opener = customOpeners.get(modId);
        // looked up once per mod, as trees are registered again on every profile switch
        if (opener == null) opener = defaultOpeners.computeIfAbsent(modId, id -> Optional.ofNullable(CompatLoader.INSTANCE.originalScreenOpener(id))).orElse(null);
        if (opener == null) return;
        tree.addMetadata(OPEN_ORIGINAL_SCREEN, opener);
    }

    private static final class Context implements ConfigEntrypoint.Context {
        private final String modId;
        private volatile boolean open = true;

        Context(String modId) {
            this.modId = modId;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public <T extends Config> T register(T config) {
            if (!open) {
                LOGGER.warn("Mod {} registered config {} after its config entrypoint returned", modId, config.id);
                // its original was already extracted, claiming the config now would not remove that card
                if (failed.contains(modId)) return config;
            }
            String owner = treeOwners.putIfAbsent(config.id, modId);
            if (owner != null && !owner.equals(modId)) {
                LOGGER.warn("Mod {} registered config {} which already belongs to {}", modId, config.id, owner);
                return config;
            }
            // the tree normally only exists once the config initializes, where onTreeRegistered picks it up
            Tree tree = config.getTree();
            if (tree != null) attachOpener(tree, modId);
            return config;
        }

        @Override
        public void originalScreen(Runnable opener) {
            customOpeners.put(modId, opener);
            // swap the opener on trees that already got the default one, such as a config initialized before
            // registerAll whose tree ConfigManager.initialize() will not register again
            // looked up from the active profile so trees left over from a profile switch are not touched
            for (Tree tree : ownedTrees(modId)) attachOpener(tree, modId);
        }
    }

    private static Set<String> providerIds() {
        providers();
        Set<String> ids = providerIds;
        return ids == null ? Collections.emptySet() : ids;
    }

    private static List<Provider> providers() {
        List<Provider> cached = providers;
        if (cached != null) return cached;
        synchronized (DISCOVERY_LOCK) {
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
                // not cached so a discovery attempted too early is retried
                if (!discoveryFailureLogged) LOGGER.error("Failed to discover OneConfig config entrypoints", t);
                discoveryFailureLogged = true;
                return Collections.emptyList();
            }
            Set<String> ids = new LinkedHashSet<>();
            for (Provider provider : found) ids.add(provider.modId);
            Map<String, String> nested = new HashMap<>();
            //? if !neoforge {
            for (String id : ids) {
                FabricLoader.getInstance().getModContainer(id).ifPresent(container -> collectNested(container, id, ids, nested));
            }
            //?}
            nestedIds = Collections.unmodifiableMap(nested);
            providerIds = Collections.unmodifiableSet(ids);
            providers = Collections.unmodifiableList(found);
            return providers;
        }
    }

    //? if !neoforge {
    // a nested mod that provides an entrypoint itself is left as its own provider
    private static void collectNested(net.fabricmc.loader.api.ModContainer container, String providerId, Set<String> ids, Map<String, String> nested) {
        for (net.fabricmc.loader.api.ModContainer child : container.getContainedMods()) {
            String childId = child.getMetadata().getId();
            if (ids.contains(childId) || nested.putIfAbsent(childId, providerId) != null) continue;
            collectNested(child, providerId, ids, nested);
        }
    }
    //?}
}
