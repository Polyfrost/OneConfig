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

package org.polyfrost.oneconfig.api.config.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.collect.PropertyCollector;
import org.polyfrost.oneconfig.api.config.v1.collect.impl.OneConfigCollector;
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl.PolyColorAdapter;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

import static org.polyfrost.oneconfig.api.config.v1.Tree.tree;

public final class ConfigManager {
    public static final Path PROFILES_DIR = Paths.get("profiles");
    static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    private static final List<PropertyCollector> collectors = new ArrayList<>(1);
    private static final ConfigManager internal = new ConfigManager(Paths.get("oneconfig"), NightConfigSerializer.ALL);
    private static final ConfigManager core = new ConfigManager(Paths.get("config"), NightConfigSerializer.ALL);
    private static final ConfigManager backup = new ConfigManager(Paths.get("oneconfig", "backup"), NightConfigSerializer.ALL);
    private static ConfigManager active;
    private static boolean initialized = false;
    private static boolean isFirstRun = false;
//    @UnmodifiableView
//    public static List<String> newOrUpdatedModIds;
    private static final Queue<Config> pendingInitialization = new ArrayDeque<>();
    private static final Map<String, Config> initializedConfigs = new LinkedHashMap<>();
    private static boolean rebindingProfiles = false;
    private static final java.util.concurrent.CopyOnWriteArrayList<ProfileChangeListener> profileListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static final String PROFILE_LOCAL_METADATA = "profileLocal";

    public interface ProfileChangeListener {
        void onProfileChanged(String newProfile);
    }

    public static void addProfileChangeListener(ProfileChangeListener listener) {
        profileListeners.add(listener);
    }

    public static Path profileDir(String profile) {
        profile = normalizeProfileName(profile, true);
        return profile.isEmpty() ? Paths.get("config") : PROFILES_DIR.resolve(profile);
    }

    static {
        ObjectSerializer.INSTANCE.registerTypeAdapter(new PolyColorAdapter());
        registerCollector(new OneConfigCollector());
    }

    final FileBackend backend;
    private volatile boolean shutdown = false;


    @SuppressWarnings("unchecked")
    private ConfigManager(Path onto, FileSerializer<?>... serializers) {
        backend = new FileBackend(onto, (FileSerializer<String>[]) serializers);
    }

    /**
     * Returns a reference to the internal config manager, which is mounted onto the ./OneConfig directory.
     */
    @ApiStatus.Internal
    public static ConfigManager internal() {
        return internal;
    }

    /**
     * Returns a reference to config manager which contains the backup configs, which is mounted onto the ./OneConfig/backup directory.
     * <b>internal use only!</b>
     * <br>used for the restore to default buttons.
     */
    @ApiStatus.Internal
    public static ConfigManager backup() {
        return backup;
    }

    /**
     * Returns a reference to the active config manager, which is mounted to the current active profile.
     */
    public static synchronized ConfigManager active() {
        if (active == null) initProfiles();
        return active;
    }

    @ApiStatus.Internal
    public static void initialize() {
        if (initialized) {
            LOGGER.error("Config already initialized!");
            return;
        }
        initialized = true;
        long t1 = System.nanoTime();
        LOGGER.info("Initializing {} configs...", pendingInitialization.size());
        while (!pendingInitialization.isEmpty()) {
            Config config = pendingInitialization.poll();
            if (config != null) config.initialize(true);
        }
        // newOrUpdatedModIds = Collections.unmodifiableList(doModsListScan());
        LOGGER.info("Initialized configs in {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }

    @ApiStatus.Internal
    public static void submitForInitialization(Config config) {
        // IMPORTANT: never initialize synchronously here. This is called from the Config base
        // constructor, *before* the subclass's instance-field initializers have run. Initializing
        // now would make collect(), default-capture and the backup save read uninitialized
        // (zero/null) instance fields.
        //
        // Instead the config is initialized lazily, after construction has completed:
        //   - configs created before OneConfig startup are drained by initialize() below;
        //   - configs created afterwards initialize on first access through a `tree == null` guard
        //     (e.g. preload(), createScreen(), getProperty()), or from their own constructor body
        //     (which runs after field initializers).
        if (!initialized) {
            pendingInitialization.add(config);
        }
    }

    static synchronized void markInitialized(Config config) {
        initializedConfigs.put(config.id, config);
    }

    static boolean isRebindingProfiles() {
        return rebindingProfiles;
    }

    /*private static List<String> doModsListScan() {
        List<String> modIds = new ArrayList<>();
        try {
            Path listFile = internal().getFolder().resolve("mods-list");
            HashMap<String, String> oldModToVersion = new HashMap<>();
            if (Files.exists(listFile)) {
                List<String> p = Files.readAllLines(internal().getFolder().resolve("mods-list"));
                for (String s : p) {
                    int sep = s.indexOf(':');
                    if (sep == -1) continue;
                    oldModToVersion.put(s.substring(0, sep), s.substring(sep + 1));
                }
            }

            StringBuilder out = new StringBuilder();
            OmniLoader.getLoadedMods().forEach(info -> {
                String id = info.getId();
                String v = oldModToVersion.get(id);
                if (v == null || !v.equals(info.getVersion())) {
                    // successfully detected a new or updated mod
                    modIds.add(id);
                }
                out.append(id).append(':').append(info.getVersion()).append('\n');
            });
            Files.write(listFile, out.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            LOGGER.error("Failed to scan mods list", e);
        }
        LOGGER.info("detected " + modIds.size() + " new or updated mods");
        return modIds;
    }*/

    static void removePendingInitialization(Config config) {
        pendingInitialization.remove(config);
    }

    static void reportResetOptions(Config config, List<String> options) {
        Tree tree = config.getTree();
        if (tree == null || tree.getID() == null) return;
        String id = tree.getID();
        ConfigManager mgr = active();
        try {
            Path file = mgr.getFolder().resolve(id);
            if (Files.exists(file)) {
                Files.copy(file, mgr.getFolder().resolve(id + ".corrupted"), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.warn("backed up problematic config {} to {}.corrupted", id, id);
            }
        } catch (IOException e) {
            LOGGER.error("failed to back up problematic config {}", id, e);
        }
        // persist the reset (default) values so the incompatible entries are scrubbed from the file.
        mgr.save(id);
        LOGGER.warn("reset {} incompatible option(s) in config {}: {}", options.size(), id, options);
        notifyResetOptions(config, options);
    }

    private static void notifyResetOptions(Config config, List<String> options) {
        try {
            String name = config.title != null ? config.title : config.id;
            String message = options.size() == 1
                    ? "The option '" + options.get(0) + "' could not be loaded and was reset to its default. A backup was saved as " + config.getTree().getID() + ".corrupted."
                    : options.size() + " options could not be loaded and were reset to their defaults (" + String.join(", ", options) + "). A backup was saved as " + config.getTree().getID() + ".corrupted.";
            org.polyfrost.oneconfig.api.notifications.v1.Notifications.error(name + ": options reset", message);
        } catch (Throwable t) {
            // notifications are best-effort and must never break config loading.
            LOGGER.error("failed to notify about reset options for config {}", config.id, t);
        }
    }

    private static synchronized void initProfiles() {
        addProfileChangeListener(CompatSnapshots.INSTANCE);
        Backend.RegistrationResult result = internal().register(
                tree("profiles.json").put(
                        Properties.simple("activeProfile", "Active Profile", "The profile which is currently open.", ""),
                        Properties.simple("favoriteProfiles", "Favorite Profiles", "Profiles marked as favorites.", new String[0], String[].class),
                        Properties.simple("profileIcons", "Profile Icons", "Icon names assigned to profiles.", new String[0], String[].class)
                )
        );
        if (result.state == Backend.RegistrationResult.NEW) {
            // asm: first run
            isFirstRun = true;
            LOGGER.info("Welcome to OneConfig!");
        }
        if (result.get().getProp("favoriteProfiles") == null) {
            result.get().put(Properties.simple("favoriteProfiles", "Favorite Profiles", "Profiles marked as favorites.", new String[0], String[].class));
            internal().save("profiles.json");
        }
        if (result.get().getProp("profileIcons") == null) {
            result.get().put(Properties.simple("profileIcons", "Profile Icons", "Icon names assigned to profiles.", new String[0], String[].class));
            internal().save("profiles.json");
        }
        String activeProfile = result.get().getProp("activeProfile").getAs();
        try {
            activeProfile = normalizeProfileName(activeProfile, true);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Active profile {} is invalid, falling back to root", activeProfile);
            activeProfile = "";
        }
        if (activeProfile != null && !activeProfile.isEmpty() && !Files.isDirectory(profilePath(activeProfile))) {
            LOGGER.warn("Active profile {} does not exist, falling back to root", activeProfile);
            activeProfile = "";
        }
        openProfile(activeProfile);
    }

    public static synchronized void openProfile(String profile) {
        openProfile(profile, true);
    }

    private static void openProfile(String profile, boolean saveCurrent) {
        profile = normalizeProfileName(profile, true);
        if (!profile.isEmpty() && !Files.isDirectory(profilePath(profile))) {
            throw new IllegalArgumentException("Profile does not exist: " + profile);
        }
        List<Tree> externalTrees = new ArrayList<>();
        if (active != null) {
            for (Tree t : active.trees()) {
                String id = t.getID();
                if (id == null || initializedConfigs.containsKey(id)) continue;
                if (Boolean.TRUE.equals(t.getMetadata(PROFILE_LOCAL_METADATA))) continue;
                externalTrees.add(t);
            }
            if (saveCurrent) active.saveAll();
            active.close();
        }
        internal().get("profiles.json").getProp("activeProfile").setAs(profile);
        internal().save("profiles.json");
        if (profile.isEmpty()) {
            LOGGER.info("opened config manager onto root (no profile)");
            active = new ConfigManager(Paths.get("config"), core.backend.getSerializers().toArray(new FileSerializer[0])).withWatcher().withHook();
        } else {
            LOGGER.info("opening profile {}", profile);
            active = new ConfigManager(PROFILES_DIR.resolve(profile), core.backend.getSerializers().toArray(new FileSerializer[0])).withHook().withWatcher();
        }
        rebindInitializedConfigs();
        for (Tree t : externalTrees) {
            try {
                active.register(t);
            } catch (Throwable ex) {
                LOGGER.error("Failed to rebind external tree {} onto profile {}", t.getID(), profile, ex);
            }
        }
        for (ProfileChangeListener listener : profileListeners) {
            try {
                listener.onProfileChanged(profile);
            } catch (Throwable t) {
                LOGGER.error("Profile change listener failed", t);
            }
        }
    }

    public static synchronized String activeProfile() {
        active();
        String profile = internal().get("profiles.json").getProp("activeProfile").getAs();
        return profile == null ? "" : profile;
    }

    public static synchronized List<String> profiles() {
        active();
        ArrayList<String> out = new ArrayList<>();
        out.add("");
        try {
            Files.createDirectories(PROFILES_DIR);
            try (Stream<Path> stream = Files.list(PROFILES_DIR)) {
                stream.filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(out::add);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list profiles", e);
        }
        return Collections.unmodifiableList(out);
    }

    public static void createProfile(String profile) {
        String name = normalizeProfileName(profile, false);
        Path path = profilePath(name);
        Path source;
        Set<String> ownedSubdirs;
        synchronized (ConfigManager.class) {
            if (Files.exists(path)) throw new IllegalArgumentException("Profile already exists: " + name);
            active().saveAll();
            source = active.getFolder();
            ownedSubdirs = oneConfigSubdirs(active);
        }
        try {
            Files.createDirectories(path);
            copyProfileFiles(source, path, ownedSubdirs);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create profile: " + name, e);
        }
        openProfile(name);
    }

    private static Set<String> oneConfigSubdirs(ConfigManager mgr) {
        Set<String> subdirs = new HashSet<>();
        for (Tree t : mgr.trees()) {
            String id = t.getID();
            if (id == null) continue;
            int slash = id.indexOf('/');
            int back = id.indexOf('\\');
            if (back >= 0 && (slash < 0 || back < slash)) slash = back;
            if (slash > 0) subdirs.add(id.substring(0, slash));
        }
        return subdirs;
    }

    private static void copyProfileFiles(Path source, Path target, Set<String> ownedSubdirs) throws IOException {
        if (!Files.exists(source)) return;
        Files.createDirectories(target);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    if (ownedSubdirs.contains(name)) copyDirectory(entry, target.resolve(name));
                } else if (Files.isRegularFile(entry)) {
                    copyFileSafely(entry, target.resolve(name));
                }
            }
        }
    }

    private static void copyFileSafely(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        try {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchFileException ignored) {
            if (Files.exists(from)) throw ignored;
        }
    }

    public static synchronized void renameProfile(String profile, String newProfile) {
        profile = normalizeProfileName(profile, false);
        newProfile = normalizeProfileName(newProfile, false);
        if (profile.equals(newProfile)) return;
        Path oldPath = profilePath(profile);
        Path newPath = profilePath(newProfile);
        if (!Files.isDirectory(oldPath)) throw new IllegalArgumentException("Profile does not exist: " + profile);
        if (Files.exists(newPath)) throw new IllegalArgumentException("Profile already exists: " + newProfile);
        if (activeProfile().equals(profile)) active.saveAll();
        String icon = profileIcon(profile);
        try {
            Files.move(oldPath, newPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to rename profile: " + profile, e);
        }
        if (isFavoriteProfile(profile)) {
            setFavoriteProfile(profile, false);
            setFavoriteProfile(newProfile, true);
        }
        if (!icon.equals(defaultProfileIcon())) {
            setProfileIcon(newProfile, icon);
        }
        if (activeProfile().equals(profile)) {
            openProfile(newProfile, false);
        }
    }

    public static synchronized void deleteProfile(String profile) {
        profile = normalizeProfileName(profile, false);
        Path path = profilePath(profile);
        if (!Files.isDirectory(path)) throw new IllegalArgumentException("Profile does not exist: " + profile);
        if (activeProfile().equals(profile)) openProfile("");
        setProfileIcon(profile, null);
        try {
            deleteDirectory(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete profile: " + profile, e);
        }
        setFavoriteProfile(profile, false);
    }

    public static synchronized List<String> favoriteProfiles() {
        active();
        Object favorites = internal().get("profiles.json").getProp("favoriteProfiles").get();
        if (favorites == null) return Collections.emptyList();
        ArrayList<String> out = new ArrayList<>();
        if (favorites instanceof Object[]) {
            for (Object favorite : (Object[]) favorites) {
                addFavoriteProfile(out, favorite);
            }
        } else if (favorites instanceof Iterable<?>) {
            for (Object favorite : (Iterable<?>) favorites) {
                addFavoriteProfile(out, favorite);
            }
        } else {
            addFavoriteProfile(out, favorites);
        }
        return Collections.unmodifiableList(out);
    }

    private static void addFavoriteProfile(List<String> out, Object favorite) {
        if (favorite != null) {
            try {
                String normalized = normalizeProfileName(favorite.toString(), true);
                if ((normalized.isEmpty() || Files.isDirectory(profilePath(normalized))) && !out.contains(normalized)) {
                    out.add(normalized);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static synchronized boolean isFavoriteProfile(String profile) {
        profile = normalizeProfileName(profile, true);
        return favoriteProfiles().contains(profile);
    }

    public static synchronized void setFavoriteProfile(String profile, boolean favorite) {
        profile = normalizeProfileName(profile, true);
        if (favorite && !profile.isEmpty() && !Files.isDirectory(profilePath(profile))) {
            throw new IllegalArgumentException("Profile does not exist: " + profile);
        }
        ArrayList<String> favorites = new ArrayList<>(favoriteProfiles());
        if (favorite) {
            if (!favorites.contains(profile)) favorites.add(profile);
        } else {
            favorites.remove(profile);
        }
        favorites.sort(String.CASE_INSENSITIVE_ORDER);
        internal().get("profiles.json").getProp("favoriteProfiles").setAs(favorites.toArray(new String[0]));
        internal().save("profiles.json");
    }

    public static synchronized Map<String, String> profileIcons() {
        active();
        Object icons = internal().get("profiles.json").getProp("profileIcons").get();
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (icons instanceof Object[]) {
            for (Object icon : (Object[]) icons) {
                addProfileIcon(out, icon);
            }
        } else if (icons instanceof Iterable<?>) {
            for (Object icon : (Iterable<?>) icons) {
                addProfileIcon(out, icon);
            }
        } else {
            addProfileIcon(out, icons);
        }
        return Collections.unmodifiableMap(out);
    }

    private static void addProfileIcon(Map<String, String> out, Object entry) {
        if (entry == null) return;
        String value = entry.toString();
        int separator = value.indexOf('=');
        if (separator <= 0 || separator == value.length() - 1) return;
        try {
            String profile = normalizeProfileName(value.substring(0, separator), false);
            String icon = normalizeProfileIcon(value.substring(separator + 1));
            if (Files.isDirectory(profilePath(profile))) {
                out.put(profile, icon);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static synchronized String profileIcon(String profile) {
        profile = normalizeProfileName(profile, true);
        if (profile.isEmpty()) return defaultProfileIcon();
        return profileIcons().getOrDefault(profile, defaultProfileIcon());
    }

    public static synchronized void setProfileIcon(String profile, @Nullable String icon) {
        profile = normalizeProfileName(profile, true);
        if (profile.isEmpty()) return;
        if (!Files.isDirectory(profilePath(profile))) {
            throw new IllegalArgumentException("Profile does not exist: " + profile);
        }
        LinkedHashMap<String, String> icons = new LinkedHashMap<>(profileIcons());
        String normalizedIcon = normalizeProfileIcon(icon);
        if (normalizedIcon.equals(defaultProfileIcon())) {
            icons.remove(profile);
        } else {
            icons.put(profile, normalizedIcon);
        }
        ArrayList<String> entries = new ArrayList<>();
        icons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> entries.add(entry.getKey() + "=" + entry.getValue()));
        internal().get("profiles.json").getProp("profileIcons").setAs(entries.toArray(new String[0]));
        internal().save("profiles.json");
    }

    private static String defaultProfileIcon() {
        return "profiles";
    }

    private static void rebindInitializedConfigs() {
        if (initializedConfigs.isEmpty()) return;
        ArrayList<Config> configs = new ArrayList<>(initializedConfigs.values());
        rebindingProfiles = true;
        try {
            for (Config config : configs) {
                try {
                    config.rebindToActiveProfile();
                } catch (Throwable ex) {
                    LOGGER.error("Failed to rebind config {} onto active profile", config.id, ex);
                }
            }
        } finally {
            rebindingProfiles = false;
        }
    }

    private static String normalizeProfileName(String profile, boolean allowRoot) {
        String normalized = profile == null ? "" : profile.trim();
        if (normalized.isEmpty()) {
            if (allowRoot) return "";
            throw new IllegalArgumentException("Profile name cannot be empty");
        }
        if (normalized.equals(".") || normalized.equals("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Invalid profile name: " + profile);
        }
        profilePath(normalized);
        return normalized;
    }

    private static String normalizeProfileIcon(String icon) {
        String normalized = icon == null ? "" : icon.trim();
        if (normalized.isEmpty()) return defaultProfileIcon();
        if (normalized.indexOf('=') >= 0) {
            throw new IllegalArgumentException("Invalid profile icon: " + icon);
        }
        return normalized;
    }

    private static Path profilePath(String profile) {
        Path root = PROFILES_DIR.toAbsolutePath().normalize();
        Path path = root.resolve(profile).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Invalid profile name: " + profile);
        }
        return path;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) return;
        try (Stream<Path> stream = Files.walk(source)) {
            Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path from = iterator.next();
                Path to = target.resolve(source.relativize(from).toString());
                if (Files.isDirectory(from)) {
                    Files.createDirectories(to);
                } else if (Files.isRegularFile(from)) {
                    Files.createDirectories(to.getParent());
                    try {
                        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                    } catch (NoSuchFileException ignored) {
                        if (Files.exists(from)) throw ignored;
                        // The active config folder can change underneath profile creation via file watchers.
                    }
                }
            }
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            Iterator<Path> iterator = stream.sorted(Comparator.reverseOrder()).iterator();
            while (iterator.hasNext()) {
                Files.deleteIfExists(iterator.next());
            }
        }
    }

    public static boolean isFirstRun() {
        return isFirstRun;
    }

    /**
     * Returns a reference to the core config manager, which is mounted onto the ./config directory.
     * <b>internal use only!</b>
     */
    @ApiStatus.Internal
    public static ConfigManager core() {
        return core;
    }

    @ApiStatus.Internal
    public static Tree collect(@NotNull Object o, @NotNull String id) {
        if (o instanceof Tree) return (Tree) o;
        for (PropertyCollector collector : collectors) {
            Tree t = collector.collect(o);
            if (t != null) {
                t.setID(id);
                return t;
            }
        }
        LOGGER.error("No registered collector for object {}", o.getClass().getName());
        return null;
    }

    /**
     * Register a collector that can be used to collect trees from objects. these are shared between all config managers.
     */
    public static void registerCollector(PropertyCollector collector) {
        collectors.add(collector);
    }

    @UnmodifiableView
    public Collection<Tree> trees() {
        return backend.getTrees();
    }

    public @Nullable Tree load(String id) {
        return backend.load(id);
    }

    public Tree get(String id) {
        return backend.get(id);
    }

    @ApiStatus.Internal
    public Tree getNoRegister(Path p) throws Exception {
        return backend.load0(p, p.getFileName().toString());
    }

    public boolean save(String id) {
        return backend.save(id);
    }

    public boolean save(Tree t) {
        return backend.save(t);
    }

    public void saveAll() {
        backend.saveAll();
    }

    public Path getFolder() {
        return backend.folder;
    }

    public Backend.RegistrationResult register(Tree t) {
        return backend.register(t);
    }

    public boolean delete(String id) {
        return backend.delete(id);
    }

    @ApiStatus.Internal
    public Collection<Tree> gatherAll(String sub) {
        return backend.gatherAll(sub);
    }

    public Tree register(@NotNull Object o, @NotNull String id) {
        Tree t = collect(o, id);
        if (t == null) return null;
        return register(t).get();
    }

    private ConfigManager withHook() {
        // two hooks that guarantee that we save lol
        // seems to improve the reliability of saving when the game crashes
        Runtime.getRuntime().addShutdownHook(new Thread(this::onClose));
        return this;
    }

    private ConfigManager withWatcher() {
        try {
            backend.addWatcher();
        } catch (Exception e) {
            LOGGER.error("Failed to register watcher onto {}", backend.folder, e);
        }
        return this;
    }

    private synchronized void onClose() {
        if (shutdown) return;
        shutdown = true;
        LOGGER.info("shutdown requested; saving all configs in ./{}", backend.folder.getFileName());
        backend.saveAll();
        backend.closeWatcher();
    }

    private synchronized void close() {
        if (shutdown) return;
        shutdown = true;
        backend.closeWatcher();
    }
}
