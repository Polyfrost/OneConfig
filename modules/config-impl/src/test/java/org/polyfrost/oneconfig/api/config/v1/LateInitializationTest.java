package org.polyfrost.oneconfig.api.config.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

class LateInitializationTest {
    static class LateConfig extends Config {
        LateConfig(String id) {
            super(id, "Late", Category.QOL);
        }
    }

    static class ValueConfig extends LateConfig {
        @Switch(title = "Value")
        public boolean value = false;

        ValueConfig(String id) {
            super(id);
        }
    }

    static class SetupConfig extends ValueConfig {
        SetupConfig(String id) {
            super(id);
            addAliases("value", new String[]{"alias"});
        }
    }

    @AfterEach
    void reopenStartup() throws ReflectiveOperationException {
        Field initialized = ConfigManager.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(null, false);
    }

    private static void finishStartup() {
        ConfigManager.initializeLate(config -> false, () -> {
        });
    }

    @Test
    void configsCreatedLateAreInitializedOnceTheCreatorReturns() {
        AtomicReference<Config> created = new AtomicReference<>();
        ConfigManager.initializeLate(config -> false, () -> {
            created.set(new LateConfig("late-initialization-test.json"));
            assertNull(created.get().getTree());
            assertTrue(ConfigManager.createdConfigs().contains(created.get()));
        });
        assertNotNull(created.get().getTree());
        assertTrue(ConfigManager.createdConfigs().contains(created.get()));
    }

    @Test
    void addedConfigsCreatedBeforeTheWindowOpensAreKeptForIt() {
        finishStartup();
        Config early = new LateConfig("late-initialization-early-test.json");
        assertNull(early.getTree());
        assertTrue(ConfigManager.createdConfigs().contains(early));
        ConfigManager.initializeLate(early::equals, () -> assertNull(early.getTree()));
        assertNotNull(early.getTree());
    }

    @Test
    void otherConfigsStayOutOfTheWindowAndKeepSavedValues() throws IOException {
        String id = "late-initialization-other-test.json";
        try {
            ValueConfig saved = new ValueConfig(id);
            finishStartup();
            saved.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            Config other = new ValueConfig(id);
            ConfigManager.initializeLate(saved::equals, () -> {
            });
            assertNull(other.getTree());
            assertEquals(Boolean.TRUE, saved.getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void constructorSetupWaitsForLateInitialization() {
        finishStartup();
        Config late = new SetupConfig("late-initialization-setup-test.json");
        assertTrue(ConfigManager.isPendingInitialization(late));
        ConfigManager.initializeLate(late::equals, () -> {
        });
        assertFalse(ConfigManager.isPendingInitialization(late));
        assertEquals(List.of("alias"), late.getProperty("value").getMetadata("aliases"));
    }

    @Test
    void configsThatInitializedThemselvesAreNotKept() {
        finishStartup();
        Config early = new LateConfig("late-initialization-preload-test.json");
        early.preload();
        Tree tree = early.getTree();
        ConfigManager.initializeLate(early::equals, () -> {
        });
        assertSame(tree, early.getTree());
        assertEquals(1, ConfigManager.createdConfigs().stream().filter(early::equals).count());
    }
}
