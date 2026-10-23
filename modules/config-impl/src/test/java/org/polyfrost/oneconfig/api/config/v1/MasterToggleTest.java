package org.polyfrost.oneconfig.api.config.v1;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.Accordion;
import org.polyfrost.oneconfig.api.config.v1.annotations.MasterToggle;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MasterToggleTest {

    private static final String PROFILE = "master-toggle-test";

    @SuppressWarnings("unused")
    public static class MasterConfig extends Config {
        @Switch(title = "Enabled")
        public static boolean modEnabled = false;
        @Switch(title = "Top")
        public static boolean top = true;

        @Accordion(title = "Nested")
        public static class Nested {
            @Switch(title = "Inner")
            public static boolean inner = true;
        }

        int initialized = 0;

        public MasterConfig() {
            super("master_toggle_test.json", "Master Toggle", Category.QOL);
            disableAllUnless("modEnabled");
        }

        @Override
        protected void onInitialized(Tree tree) {
            initialized++;
        }
    }

    @SuppressWarnings("unused")
    public static class SetupConfig extends Config {
        @Switch(title = "Hidden")
        public static boolean hidden = true;
        @Text(title = "Migrated")
        public static String migrated = "default";

        int fired = 0;

        public SetupConfig(Path legacy) {
            super("setup_replay_test.json", "Setup Replay", Category.QOL);
            hideIf("hidden", () -> true);
            addCallback("hidden", () -> {
            });
            loadFrom(legacy);
        }

        @Override
        protected void initialize(boolean byConfigManager) {
            super.initialize(byConfigManager);
            if (tree == null) return;
            addCallback("migrated", () -> fired++);
        }
    }

    @SuppressWarnings("unused")
    public static class AnnotatedConfig extends Config {
        @MasterToggle
        public static boolean active = true;
        @Switch(title = "Other")
        public static boolean other = true;

        @Accordion(title = "Nested")
        public static class Nested {
            @MasterToggle
            @Switch(title = "Nested toggle")
            public static boolean nestedToggle = true;
        }

        public AnnotatedConfig() {
            super("master_toggle_annotation_test.json", "Master Toggle Annotation", Category.QOL);
        }
    }

    @SuppressWarnings("unused")
    public static class WrongTypeConfig extends Config {
        @MasterToggle
        public static String active = "yes";

        public WrongTypeConfig() {
            super("master_toggle_wrong_type_test.json", "Master Toggle Wrong Type", Category.QOL);
        }
    }

    @Test
    void masterToggleAnnotationMakesAHiddenOption() {
        AnnotatedConfig config = new AnnotatedConfig();
        config.preload();
        for (String id : new String[]{"active", "Nested.nestedToggle"}) {
            Property<?> toggle = config.getProperty(id);
            assertEquals(Boolean.TRUE, toggle.getMetadata("masterToggle"));
            assertEquals(Boolean.TRUE, toggle.getMetadata("hidden"));
        }
        assertNull(config.getProperty("other").getMetadata("masterToggle"));
    }

    @Test
    void masterToggleAnnotationRejectsNonBooleans() {
        assertThrows(RuntimeException.class, () -> new WrongTypeConfig().preload());
    }

    @Test
    void masterToggleDisablesNestedOptionsOnce() {
        MasterConfig config = new MasterConfig();
        config.preload();
        config.initialize(false);
        assertState(config, 1);

        ConfigManager.createProfile(PROFILE);
        try {
            assertState(config, 2);
        } finally {
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(PROFILE);
        }
    }

    @Test
    void constructorSetupSurvivesProfileSwitchButMigrationRunsOnce() throws Exception {
        Path legacy = Files.createTempFile("setup_replay_legacy", ".json");
        Files.writeString(legacy, "{ \"migrated\": \"first\" }");
        Path stored = ConfigManager.active().getFolder().resolve("setup_replay_test.json");
        Files.deleteIfExists(stored);
        SetupConfig config = new SetupConfig(legacy);
        config.preload();
        assertEquals("first", config.getProperty("migrated").get());
        Files.writeString(legacy, "{ \"migrated\": \"second\" }");

        assertSetupAppliedOnce(config);
        ConfigManager.createProfile(PROFILE + "-setup");
        try {
            assertSetupAppliedOnce(config);
            assertNotEquals("second", config.getProperty("migrated").get());
            ConfigManager.openProfile("");
            assertSetupAppliedOnce(config);
        } finally {
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(PROFILE + "-setup");
            Files.deleteIfExists(legacy);
            Files.deleteIfExists(stored);
        }
    }

    private static void assertSetupAppliedOnce(SetupConfig config) {
        Property<?> hidden = config.getProperty("hidden");
        assertEquals(Property.Display.HIDDEN, hidden.getDisplay());
        assertEquals(1, hidden.callbacks.size(), "constructor setup must be replayed onto the rebuilt tree");
        int before = config.fired;
        config.getProperty("migrated").setAs("set " + before);
        assertEquals(before + 1, config.fired, "setup from an initialize() override must not also be replayed");
    }

    private static void assertState(MasterConfig config, int initialized) {
        assertEquals(initialized, config.initialized);
        Property<?> toggle = config.getProperty("modEnabled");
        assertEquals(Property.Display.SHOWN, toggle.getDisplay());
        assertEquals(Property.Display.DISABLED, config.getProperty("top").getDisplay());
        assertEquals(Property.Display.DISABLED, config.getProperty("Nested.inner").getDisplay());
        assertEquals(2, toggle.callbacks.size());
        toggle.setAs(true);
        assertEquals(Property.Display.SHOWN, config.getProperty("Nested.inner").getDisplay());
        toggle.setAs(false);
    }
}
