package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatTreeRebuildTest {
    private static final String PROFILE = "oc_compat_rebuild_test";
    private static final String TREE_ID = "compat_rebuild_test";

    static final class ModConfig {
        String bar = "default";
    }

    private String disk = "default";
    private ModConfig unpatched = new ModConfig();
    private ModConfig patched = new ModConfig();

    private void save() {
        disk = unpatched.bar;
        patched = new ModConfig();
        patched.bar = unpatched.bar;
    }

    private void reload() {
        unpatched = new ModConfig();
        unpatched.bar = disk;
        patched = new ModConfig();
        patched.bar = disk;
    }

    private void openConfigScreen() {
        ModConfig captured = unpatched;
        Tree tree = Tree.tree(TREE_ID);
        Property<String> bar = Properties.functional(
                () -> captured.bar, v -> captured.bar = v, "bar", "Bar", "", String.class
        );
        bar.addMetadata("default", "default");
        tree.put(bar);
        tree.addMetadata("custom_save", (Runnable) this::save);
        CompatSnapshots.register(tree);
    }

    private void setThroughOneConfigUi(String value) {
        ConfigManager.active().get(TREE_ID).getProp("bar").setAs(value);
        ConfigManager.active().save(TREE_ID);
    }

    @BeforeEach
    void setUp() throws IOException {
        ConfigManager.active();
        ConfigManager.openProfile("");
        cleanup();
    }

    @AfterEach
    void tearDown() throws IOException {
        ConfigManager.openProfile("");
        cleanup();
    }

    private void cleanup() throws IOException {
        Path dir = ConfigManager.PROFILES_DIR.resolve(PROFILE);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path p : s.sorted(Comparator.reverseOrder()).toArray(Path[]::new)) Files.deleteIfExists(p);
            }
        }
        Files.deleteIfExists(ConfigManager.profileDir("").resolve("compat-snapshots.json"));
        Files.deleteIfExists(ConfigManager.profileDir("").resolve("compat-baseline.json"));
    }

    @Test
    void profilesStillApplyAfterTheModReplacesItsConfigInstance() {
        openConfigScreen();
        setThroughOneConfigUi("root");

        ConfigManager.createProfile(PROFILE);
        setThroughOneConfigUi("profile");
        ConfigManager.openProfile("");
        assertEquals("root", patched.bar, "root value before the reload");

        reload();
        openConfigScreen();

        ConfigManager.openProfile(PROFILE);
        assertEquals("profile", patched.bar, "the profile value must reach the mod's live config");

        ConfigManager.openProfile("");
        assertEquals("root", patched.bar, "and switching back must restore the root value");
    }
}
