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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private static final ReentrantLock PROFILE_LIFECYCLE_LOCK = new ReentrantLock();
    private static volatile boolean rebindingProfiles = false;
    private static final java.util.concurrent.CopyOnWriteArrayList<ProfileChangeListener> profileListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.concurrent.CopyOnWriteArrayList<TreeRegistrationListener> treeListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static final String PROFILE_LOCAL_METADATA = "profileLocal";

    public interface ProfileChangeListener {
        void onProfileChanged(String newProfile);

        default void onProfileSaving(String profile) {
        }

        default void onProfileCreated(String profile) {
        }

        default void onProfileRenamed(String oldProfile, String newProfile) {
        }

        default void onProfileDeleted(String profile) {
        }

        default void onProfileSpecificControlsChanged(boolean enabled) {
        }
    }

    public interface TreeRegistrationListener {
        void onTreeRegistered(@NotNull Tree tree);
    }

    public static void addProfileChangeListener(ProfileChangeListener listener) {
        profileListeners.add(listener);
    }

    @ApiStatus.Internal
    public static void addTreeRegistrationListener(TreeRegistrationListener listener) {
        treeListeners.add(listener);
    }

    public static void removeProfileChangeListener(ProfileChangeListener listener) {
        profileListeners.remove(listener);
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

    @ApiStatus.Internal
    public static boolean isRebindingProfiles() {
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
        Property<String[]> ownedProfileSubdirs = Properties.simple(
                "ownedProfileSubdirs", "Owned Profile Subdirectories",
                "Profile-backed config directories known to OneConfig.", new String[0], String[].class
        );
        ownedProfileSubdirs.addMetadata("hidden", true);
        Backend.RegistrationResult result = internal().register(
                tree("profiles.json").put(
                        Properties.simple("activeProfile", "Active Profile", "The profile which is currently open.", ""),
                        Properties.simple("favoriteProfiles", "Favorite Profiles", "Profiles marked as favorites.", new String[0], String[].class),
                        Properties.simple("profileIcons", "Profile Icons", "Icon names assigned to profiles.", new String[0], String[].class),
                        Properties.simple("profileSpecificControls", "Profile-specific Controls", "Whether Minecraft controls are stored per profile.", true),
                        ownedProfileSubdirs
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
        if (result.get().getProp("profileSpecificControls") == null) {
            result.get().put(Properties.simple("profileSpecificControls", "Profile-specific Controls", "Whether Minecraft controls are stored per profile.", true));
            internal().save("profiles.json");
        }
        if (result.get().getProp("ownedProfileSubdirs") == null) {
            Property<String[]> property = Properties.simple(
                    "ownedProfileSubdirs", "Owned Profile Subdirectories",
                    "Profile-backed config directories known to OneConfig.", new String[0], String[].class
            );
            property.addMetadata("hidden", true);
            result.get().put(property);
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
        openProfile(activeProfile, true, false);
        notifyProfileChanged(activeProfile);
    }

    public static void openProfile(String profile) {
        runProfileOperation(() -> openProfile0(profile));
    }

    private static void openProfile0(String profile) {
        openProfile0(profile, null);
    }

    private static void openProfile0(String profile, @Nullable String alreadySavedProfile) {
        profile = normalizeProfileName(profile, true);
        String previousProfile;
        synchronized (ConfigManager.class) {
            if (!profile.isEmpty() && !Files.isDirectory(profilePath(profile))) {
                throw new IllegalArgumentException("Profile does not exist: " + profile);
            }
            previousProfile = activeProfile();
        }
        // Profile listeners own state which is not part of the config backend (for example,
        // Minecraft controls). Save it while the outgoing profile is still the committed owner.
        if (!previousProfile.equals(alreadySavedProfile)) saveProfileState(previousProfile);
        synchronized (ConfigManager.class) {
            openProfile(profile, false, false);
        }
        notifyProfileChanged(profile);
    }

    private static void openProfile(String profile, boolean saveCurrent, boolean restoreDefaults) {
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
        rebindingProfiles = true;
        try {
            rebindInitializedConfigs(restoreDefaults);
            for (Tree t : externalTrees) {
                try {
                    if (restoreDefaults
                            && !Boolean.TRUE.equals(t.getMetadata(CompatSnapshots.SNAPSHOT_METADATA))
                            && !Boolean.TRUE.equals(t.getMetadata(Backend.UI_ONLY_METADATA))) {
                        Config.restoreCapturedDefaults(t);
                    }
                    active.register(t);
                } catch (Throwable ex) {
                    LOGGER.error("Failed to rebind external tree {} onto profile {}", t.getID(), profile, ex);
                }
            }
        } finally {
            rebindingProfiles = false;
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
        runProfileOperation(() -> createProfile0(profile));
    }

    private static void createProfile0(String profile) {
        String name = normalizeProfileName(profile, false);
        Path path = profilePath(name);
        String previousProfile;
        synchronized (ConfigManager.class) {
            if (Files.exists(path)) throw new IllegalArgumentException("Profile already exists: " + name);
            previousProfile = activeProfile();
        }
        saveProfileState(previousProfile);
        try {
            Files.createDirectories(PROFILES_DIR);
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalArgumentException("Profile already exists: " + name, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create profile: " + name, e);
        }
        synchronized (ConfigManager.class) {
            openProfile(name, false, true);
        }
        notifyProfileCreated(name);
        synchronized (ConfigManager.class) {
            active().saveAll();
        }
        notifyProfileChanged(name);
    }

    public static void cloneProfile(String profile, String newProfile) {
        runProfileOperation(() -> cloneProfile0(profile, newProfile));
    }

    private static void cloneProfile0(String profile, String newProfile) {
        profile = normalizeProfileName(profile, true);
        String name = normalizeProfileName(newProfile, false);
        Path source = profileDir(profile);
        Path target = profilePath(name);
        Set<String> ownedSubdirs;
        String currentProfile;
        synchronized (ConfigManager.class) {
            if (!profile.isEmpty() && !Files.isDirectory(source)) {
                throw new IllegalArgumentException("Profile does not exist: " + profile);
            }
            if (Files.exists(target)) throw new IllegalArgumentException("Profile already exists: " + name);
            ownedSubdirs = profile.isEmpty() ? oneConfigSubdirs() : Collections.emptySet();
            currentProfile = activeProfile();
        }
        saveProfileState(profile);
        if (!profile.equals(currentProfile)) saveProfileState(currentProfile);
        try {
            Files.createDirectories(PROFILES_DIR);
            Files.createDirectory(target);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalArgumentException("Profile already exists: " + name, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create cloned profile: " + name, e);
        }
        try {
            if (profile.isEmpty()) {
                copyProfileFiles(source, target, ownedSubdirs);
            } else {
                copyDirectory(source, target);
            }
        } catch (IOException e) {
            try {
                deleteDirectory(target);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw new IllegalStateException("Failed to clone profile: " + profile, e);
        }
        String icon = profileIcon(profile);
        if (!icon.equals(defaultProfileIcon())) setProfileIcon(name, icon);
        openProfile0(name, currentProfile);
    }

    private static Set<String> oneConfigSubdirs() {
        active();
        Set<String> subdirs = new HashSet<>();
        // HUD files belong to OneConfig even when their providing mod is not present in this run,
        // so they must not disappear from a clone or export of the Default profile.
        subdirs.add("huds");
        Property<?> property = internal().get("profiles.json").getProp("ownedProfileSubdirs");
        if (property != null) addOwnedProfileSubdirs(subdirs, property.get());
        return subdirs;
    }

    private static void rememberProfileSubdir(@Nullable String id) {
        String subdir = profileSubdir(id);
        if (subdir == null) return;
        synchronized (ConfigManager.class) {
            Property<?> property = internal().get("profiles.json").getProp("ownedProfileSubdirs");
            if (property == null) return;
            Set<String> subdirs = new HashSet<>();
            addOwnedProfileSubdirs(subdirs, property.get());
            if (!subdirs.add(subdir)) return;
            ArrayList<String> sorted = new ArrayList<>(subdirs);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            property.setAs(sorted.toArray(new String[0]));
            internal().save("profiles.json");
        }
    }

    private static void addOwnedProfileSubdirs(Set<String> out, @Nullable Object value) {
        if (value instanceof Object[]) {
            for (Object entry : (Object[]) value) addOwnedProfileSubdir(out, entry);
        } else if (value instanceof Iterable<?>) {
            for (Object entry : (Iterable<?>) value) addOwnedProfileSubdir(out, entry);
        } else {
            addOwnedProfileSubdir(out, value);
        }
    }

    private static void addOwnedProfileSubdir(Set<String> out, @Nullable Object value) {
        if (value == null) return;
        String subdir = value.toString();
        if (isSafeProfileSubdir(subdir)) out.add(subdir);
    }

    private static @Nullable String profileSubdir(@Nullable String id) {
        if (id == null) return null;
        int slash = id.indexOf('/');
        int backslash = id.indexOf('\\');
        int separator = slash < 0 ? backslash : backslash < 0 ? slash : Math.min(slash, backslash);
        if (separator <= 0) return null;
        String subdir = id.substring(0, separator);
        return isSafeProfileSubdir(subdir) ? subdir : null;
    }

    private static boolean isSafeProfileSubdir(String subdir) {
        return !subdir.isEmpty()
                && !subdir.equals(".")
                && !subdir.equals("..")
                && subdir.indexOf('/') < 0
                && subdir.indexOf('\\') < 0;
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

    public static void renameProfile(String profile, String newProfile) {
        runProfileOperation(() -> renameProfile0(profile, newProfile));
    }

    private static void renameProfile0(String profile, String newProfile) {
        profile = normalizeProfileName(profile, false);
        newProfile = normalizeProfileName(newProfile, false);
        if (profile.equals(newProfile)) return;
        Path oldPath = profilePath(profile);
        Path newPath = profilePath(newProfile);
        synchronized (ConfigManager.class) {
            if (!Files.isDirectory(oldPath)) throw new IllegalArgumentException("Profile does not exist: " + profile);
            if (Files.exists(newPath)) throw new IllegalArgumentException("Profile already exists: " + newProfile);
        }
        saveProfileState(profile);
        boolean activeProfile;
        synchronized (ConfigManager.class) {
            if (!Files.isDirectory(oldPath)) throw new IllegalArgumentException("Profile does not exist: " + profile);
            if (Files.exists(newPath)) throw new IllegalArgumentException("Profile already exists: " + newProfile);
            activeProfile = activeProfile().equals(profile);
            boolean favorite = isFavoriteProfile(profile);
            String icon = profileIcon(profile);
            if (activeProfile) active.saveAll();
            try {
                Files.move(oldPath, newPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to rename profile: " + profile, e);
            }
            if (favorite) {
                setFavoriteProfile(newProfile, true);
            }
            if (!icon.equals(defaultProfileIcon())) {
                setProfileIcon(newProfile, icon);
            }
            if (activeProfile) {
                openProfile(newProfile, false, false);
            }
        }
        notifyProfileRenamed(profile, newProfile);
        if (activeProfile) notifyProfileChanged(newProfile);
    }

    public static void deleteProfile(String profile) {
        runProfileOperation(() -> deleteProfile0(profile));
    }

    private static void deleteProfile0(String profile) {
        profile = normalizeProfileName(profile, false);
        synchronized (ConfigManager.class) {
            if (!Files.isDirectory(profilePath(profile))) {
                throw new IllegalArgumentException("Profile does not exist: " + profile);
            }
        }
        saveProfileState(profile);
        boolean switchedToRoot;
        IllegalStateException failure = null;
        synchronized (ConfigManager.class) {
            Path path = profilePath(profile);
            if (!Files.isDirectory(path)) throw new IllegalArgumentException("Profile does not exist: " + profile);
            switchedToRoot = activeProfile().equals(profile);
            if (switchedToRoot) openProfile("", false, false);
            try {
                deleteDirectory(path);
                setProfileIcon(profile, null);
                setFavoriteProfile(profile, false);
            } catch (IOException e) {
                failure = new IllegalStateException("Failed to delete profile: " + profile, e);
            }
        }
        if (failure != null) {
            if (switchedToRoot) notifyProfileChanged("");
            throw failure;
        }
        notifyProfileDeleted(profile);
        if (switchedToRoot) notifyProfileChanged("");
    }

    public static void exportProfile(String profile, Path destination) {
        runProfileOperation(() -> exportProfile0(profile, destination));
    }

    private static void exportProfile0(String profile, Path destination) {
        profile = normalizeProfileName(profile, true);
        Objects.requireNonNull(destination, "destination");
        Path source = profileDir(profile).toAbsolutePath().normalize();
        Path target = destination.toAbsolutePath().normalize();
        if (target.getParent() == null) {
            throw new IllegalArgumentException("Export destination must be a file path");
        }
        Set<String> ownedSubdirs;
        synchronized (ConfigManager.class) {
            if (!profile.isEmpty() && !Files.isDirectory(source)) {
                throw new IllegalArgumentException("Profile does not exist: " + profile);
            }
            if (target.startsWith(source)) {
                throw new IllegalArgumentException("Export destination cannot be inside the profile");
            }
            ownedSubdirs = profile.isEmpty() ? oneConfigSubdirs() : Collections.emptySet();
        }
        saveProfileState(profile);
        Path temporary = null;
        try {
            Path parent = target.getParent();
            Files.createDirectories(parent);
            Path realSource = source.toRealPath();
            Path realParent = parent.toRealPath();
            if (realParent.startsWith(realSource)) {
                throw new IllegalArgumentException("Export destination cannot be inside the profile");
            }
            temporary = Files.createTempFile(parent, "oneconfig-profile-", ".zip.tmp");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                if (profile.isEmpty()) {
                    if (Files.exists(source)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
                            for (Path entry : stream) {
                                if (Files.isDirectory(entry) && ownedSubdirs.contains(entry.getFileName().toString())) {
                                    zipDirectory(entry, source, zip);
                                } else if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                                    zipFile(entry, source, zip);
                                }
                            }
                        }
                    }
                } else {
                    zipDirectory(source, source, zip);
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException e) {
            try {
                if (temporary != null) Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw new IllegalStateException("Failed to export profile: " + profile, e);
        }
    }

    private static void zipDirectory(Path directory, Path root, ZipOutputStream zip) throws IOException {
        if (!Files.exists(directory)) return;
        try (Stream<Path> stream = Files.walk(directory)) {
            Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path entry = iterator.next();
                if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) zipFile(entry, root, zip);
            }
        }
    }

    private static void zipFile(Path file, Path root, ZipOutputStream zip) throws IOException {
        String name = root.relativize(file).toString().replace('\\', '/');
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private static void saveProfileState(String profile) {
        synchronized (ConfigManager.class) {
            if (activeProfile().equals(profile)) active().saveAll();
        }
        for (ProfileChangeListener listener : profileListeners) {
            try {
                listener.onProfileSaving(profile);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to save profile state: " + profile, t);
            }
        }
    }

    private static void notifyProfileDeleted(String profile) {
        for (ProfileChangeListener listener : profileListeners) {
            try {
                listener.onProfileDeleted(profile);
            } catch (Throwable t) {
                LOGGER.error("Profile delete listener failed", t);
            }
        }
    }

    private static void notifyProfileCreated(String profile) {
        for (ProfileChangeListener listener : profileListeners) {
            try {
                listener.onProfileCreated(profile);
            } catch (Throwable t) {
                LOGGER.error("Profile create listener failed", t);
            }
        }
    }

    private static void notifyProfileChanged(String profile) {
        for (ProfileChangeListener listener : profileListeners) {
            try {
                listener.onProfileChanged(profile);
            } catch (Throwable t) {
                LOGGER.error("Profile change listener failed", t);
            }
        }
    }

    private static void notifyProfileRenamed(String oldProfile, String newProfile) {
        for (ProfileChangeListener listener : profileListeners) {
            try {
                listener.onProfileRenamed(oldProfile, newProfile);
            } catch (Throwable t) {
                LOGGER.error("Profile rename listener failed", t);
            }
        }
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
        String normalizedIcon = normalizeProfileIcon(icon);
        if (!normalizedIcon.equals(defaultProfileIcon()) && !Files.isDirectory(profilePath(profile))) {
            throw new IllegalArgumentException("Profile does not exist: " + profile);
        }
        LinkedHashMap<String, String> icons = new LinkedHashMap<>(profileIcons());
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

    public static synchronized boolean profileSpecificControls() {
        active();
        Object value = internal().get("profiles.json").getProp("profileSpecificControls").get();
        return !(value instanceof Boolean) || (Boolean) value;
    }

    public static void setProfileSpecificControls(boolean enabled) {
        runProfileOperation(() -> setProfileSpecificControls0(enabled));
    }

    private static void setProfileSpecificControls0(boolean enabled) {
        boolean previous;
        synchronized (ConfigManager.class) {
            active();
            Property<?> property = internal().get("profiles.json").getProp("profileSpecificControls");
            if (Objects.equals(property.get(), enabled)) return;
            previous = !(property.get() instanceof Boolean) || (Boolean) property.get();
            property.setAs(enabled);
            if (!internal().save("profiles.json")) {
                property.setAs(previous);
                IllegalStateException failure = new IllegalStateException(
                        "Failed to persist profile-specific controls preference"
                );
                if (!internal().save("profiles.json")) {
                    failure.addSuppressed(new IllegalStateException(
                            "Failed to restore profile-specific controls preference after save failure"
                    ));
                }
                throw failure;
            }
        }
        ArrayList<ProfileChangeListener> attempted = new ArrayList<>();
        for (ProfileChangeListener listener : profileListeners) {
            attempted.add(listener);
            try {
                listener.onProfileSpecificControlsChanged(enabled);
            } catch (Throwable t) {
                synchronized (ConfigManager.class) {
                    internal().get("profiles.json").getProp("profileSpecificControls").setAs(previous);
                    if (!internal().save("profiles.json")) {
                        t.addSuppressed(new IllegalStateException(
                                "Failed to persist the rolled-back profile-specific controls preference"
                        ));
                    }
                }
                Collections.reverse(attempted);
                for (ProfileChangeListener notified : attempted) {
                    try {
                        notified.onProfileSpecificControlsChanged(previous);
                    } catch (Throwable rollbackFailure) {
                        t.addSuppressed(rollbackFailure);
                    }
                }
                throw new IllegalStateException("Failed to change profile-specific controls", t);
            }
        }
    }

    private static void runProfileOperation(Runnable operation) {
        if (PROFILE_LIFECYCLE_LOCK.isHeldByCurrentThread() || !PROFILE_LIFECYCLE_LOCK.tryLock()) {
            throw new IllegalStateException("Another profile operation is already in progress");
        }
        try {
            operation.run();
        } finally {
            PROFILE_LIFECYCLE_LOCK.unlock();
        }
    }

    private static String defaultProfileIcon() {
        return "profiles";
    }

    private static void rebindInitializedConfigs(boolean restoreDefaults) {
        if (initializedConfigs.isEmpty()) return;
        ArrayList<Config> configs = new ArrayList<>(initializedConfigs.values());
        for (Config config : configs) {
            try {
                config.rebindToActiveProfile(restoreDefaults);
            } catch (Throwable ex) {
                LOGGER.error("Failed to rebind config {} onto active profile", config.id, ex);
            }
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
        if (this == active && !Boolean.TRUE.equals(t.getMetadata(CompatSnapshots.SNAPSHOT_METADATA))) {
            Config.captureDefaults(t);
        }
        Backend.RegistrationResult result = backend.register(t);
        if (this == active) {
            Tree registered = result.get();
            if (registered != null && registered.getID() != null) {
                rememberProfileSubdir(registered.getID());
            }
            notifyTreeRegistered(registered);
        }
        return result;
    }

    private static void notifyTreeRegistered(Tree tree) {
        if (tree == null || tree.getID() == null || treeListeners.isEmpty()) return;
        for (TreeRegistrationListener listener : treeListeners) {
            try {
                listener.onTreeRegistered(tree);
            } catch (Throwable t) {
                LOGGER.error("Tree registration listener failed for {}", tree.getID(), t);
            }
        }
    }

    public boolean delete(String id) {
        return backend.delete(id);
    }

    /** Stops tracking a tree without deleting its file, so it can be rebound to a new owner. */
    @ApiStatus.Internal
    public Tree unregister(String id) {
        return backend.unregister(id);
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
