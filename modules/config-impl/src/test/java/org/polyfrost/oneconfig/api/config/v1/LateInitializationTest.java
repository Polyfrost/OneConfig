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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
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

    static class BlockedConfig extends LateConfig {
        static final AtomicBoolean initializedEarly = new AtomicBoolean();
        private final boolean built;

        BlockedConfig(String id, CountDownLatch entered, CountDownLatch release) throws InterruptedException {
            super(id);
            entered.countDown();
            release.await();
            built = true;
        }

        @Override
        protected void onInitialized(Tree tree) {
            if (!built) initializedEarly.set(true);
        }
    }

    private static Thread buildBlocked(String id, CountDownLatch entered, CountDownLatch release) {
        Thread thread = new Thread(() -> {
            try {
                new BlockedConfig(id, entered, release);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        thread.start();
        return thread;
    }

    @AfterEach
    void reopenStartup() throws ReflectiveOperationException {
        Field initialized = ConfigManager.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(null, false);
    }

    private static void finishStartup() {
        ConfigManager.initialize();
    }

    @Test
    void configsCreatedLateAreInitializedOnceTheCreatorReturns() {
        finishStartup();
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

    @Test
    void configsOtherThreadsAreBuildingStayOutOfTheWindow() throws InterruptedException {
        finishStartup();
        BlockedConfig.initializedEarly.set(false);
        CountDownLatch release = new CountDownLatch(1);

        CountDownLatch enteredBefore = new CountDownLatch(1);
        Thread before = buildBlocked("late-initialization-blocked-before-test.json", enteredBefore, release);
        enteredBefore.await();

        CountDownLatch enteredDuring = new CountDownLatch(1);
        AtomicReference<Thread> during = new AtomicReference<>();
        ConfigManager.initializeLate(BlockedConfig.class::isInstance, () -> {
            during.set(buildBlocked("late-initialization-blocked-during-test.json", enteredDuring, release));
            try {
                enteredDuring.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });

        release.countDown();
        before.join();
        during.get().join();
        assertFalse(BlockedConfig.initializedEarly.get());
    }

    @Test
    void configsHandedOverInTheWindowAreInitialized() {
        finishStartup();
        Config library = new LateConfig("late-initialization-claimed-test.json");
        ConfigManager.claimForLateInitialization(library);
        assertTrue(ConfigManager.isPendingInitialization(library));
        assertNull(library.getTree());

        ConfigManager.initializeLate(config -> false, () -> ConfigManager.claimForLateInitialization(library));
        assertNotNull(library.getTree());
    }

    @Test
    void handedOverCopiesOfARegisteredConfigStayOut() throws IOException {
        String id = "late-initialization-claimed-copy-test.json";
        try {
            ValueConfig saved = new ValueConfig(id);
            finishStartup();
            saved.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            Config copy = new ValueConfig(id);
            ConfigManager.initializeLate(config -> false, () -> ConfigManager.claimForLateInitialization(copy));
            assertNull(copy.getTree());
            assertEquals(Boolean.TRUE, saved.getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }
}
