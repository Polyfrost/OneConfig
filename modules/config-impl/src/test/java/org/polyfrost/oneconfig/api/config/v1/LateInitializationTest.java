package org.polyfrost.oneconfig.api.config.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
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

    static class FailingConfig extends ValueConfig {
        FailingConfig(String id) {
            super(id);
        }

        @Override
        protected Tree makeTree() {
            throw new IllegalStateException("this config never builds");
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
        ConfigManager.setAwaitingLateWindow(config -> false);
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
        ConfigManager.setAwaitingLateWindow(config -> true);
        Config late = new SetupConfig("late-initialization-setup-test.json");
        assertTrue(ConfigManager.isPendingInitialization(late));
        ConfigManager.initializeLate(late::equals, () -> {
        });
        assertFalse(ConfigManager.isPendingInitialization(late));
        assertEquals(List.of("alias"), late.getProperty("value").getMetadata("aliases"));
    }

    @Test
    void constructorSetupInitializesConfigsNoWindowIsComingFor() {
        finishStartup();
        Config late = new SetupConfig("late-initialization-ordinary-test.json");
        assertNotNull(late.getTree());
        assertEquals(List.of("alias"), late.getProperty("value").getMetadata("aliases"));
    }

    @Test
    void aConfigStillBeingBuiltIsReportedUntilItCanJoin() throws InterruptedException {
        finishStartup();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Config> built = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                built.set(new BlockedConfig("late-initialization-retry-test.json", entered, release));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        worker.start();
        entered.await();

        assertTrue(ConfigManager.initializeLate(BlockedConfig.class::isInstance, () -> {
        }));
        release.countDown();
        worker.join();

        assertFalse(ConfigManager.initializeLate(built.get()::equals, () -> {
        }));
        assertNotNull(built.get().getTree());
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
    void configsOtherThreadsFinishedBuildingJoinTheWindow() throws InterruptedException {
        finishStartup();
        AtomicReference<Config> built = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            built.set(new LateConfig("late-initialization-worker-test.json"));
            done.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        worker.start();
        done.await();

        ConfigManager.initializeLate(built.get()::equals, () -> {
        });
        release.countDown();
        worker.join();
        assertNotNull(built.get().getTree());
    }

    @Test
    void aConfigAWorkerFinishesDuringTheWindowIsInitialized() {
        finishStartup();
        AtomicReference<Config> built = new AtomicReference<>();
        boolean waiting = ConfigManager.initializeLate(config -> config == built.get(), () -> {
            Thread worker = new Thread(() -> built.set(new LateConfig("late-initialization-during-test.json")));
            worker.start();
            try {
                worker.join();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        assertFalse(waiting);
        assertNotNull(built.get().getTree());
    }

    @Test
    void aReplacementForAFailedConfigKeepsItsSetup() throws IOException {
        String id = "late-initialization-failed-test.json";
        try {
            finishStartup();
            ConfigManager.initializeLate(config -> false, () -> new FailingConfig(id));
            AtomicReference<Config> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> false, () -> replacement.set(new SetupConfig(id)));
            assertEquals(List.of("alias"), replacement.get().getProperty("value").getMetadata("aliases"));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void theFirstInstanceOfAnIdWinsTheWindow() throws IOException {
        String id = "late-initialization-order-test.json";
        try {
            finishStartup();
            List<Config> instances = new ArrayList<>();
            for (int i = 0; i < 20; i++) instances.add(new ValueConfig(id));
            ConfigManager.initializeLate(instances::contains, () -> {
            });
            assertNotNull(instances.get(0).getTree());
            assertEquals(1, instances.stream().filter(config -> config.getTree() != null).count());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void configsNoWindowClaimsAreReleased() throws InterruptedException {
        finishStartup();
        WeakReference<Config> unclaimed = new WeakReference<>(new LateConfig("late-initialization-released-test.json"));
        for (int i = 0; i < 100 && unclaimed.get() != null; i++) {
            System.gc();
            Thread.sleep(10);
        }
        assertNull(unclaimed.get());
    }

    private static void collect(WeakReference<Config> reference, int attempts) throws InterruptedException {
        for (int i = 0; i < attempts && reference.get() != null; i++) {
            System.gc();
            Thread.sleep(10);
        }
    }

    @Test
    void configsAWindowIsComingForNeedNoOtherReference() throws IOException, InterruptedException {
        String id = "late-initialization-held-test.json";
        try {
            finishStartup();
            ConfigManager.setAwaitingLateWindow(config -> config.id.equals(id));
            WeakReference<Config> first = new WeakReference<>(new LateConfig(id));
            WeakReference<Config> copy = new WeakReference<>(new LateConfig(id));
            collect(first, 20);
            assertNotNull(first.get());

            ConfigManager.initializeLate(config -> config.id.equals(id), () -> {
            });
            assertNotNull(first.get().getTree());
            collect(copy, 100);
            assertNull(copy.get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void anEntrypointOfTheUpdatedModReplacesItsOlderInstance() throws IOException {
        String id = "late-initialization-replaced-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            AtomicReference<Config> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> replacement.set(new ValueConfig(id)));

            assertSame(replacement.get().getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, replacement.get().getProperty("value").get());
            assertTrue(ConfigManager.createdConfigs().contains(replacement.get()));
            assertFalse(ConfigManager.createdConfigs().contains(old));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void anEntrypointCannotReplaceAConfigOfAnotherMod() throws IOException {
        String id = "late-initialization-other-mod-test.json";
        try {
            ValueConfig theirs = new ValueConfig(id);
            finishStartup();

            AtomicReference<Config> mine = new AtomicReference<>();
            ConfigManager.initializeLate(config -> config != theirs, () -> mine.set(new ValueConfig(id)));

            assertNull(mine.get().getTree());
            assertSame(theirs.getTree(), ConfigManager.active().get(id));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aTemporaryCopyOfTheUpdatedModDoesNotReplaceIt() throws IOException {
        String id = "late-initialization-temporary-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();

            Config copy = new ValueConfig(id);
            ConfigManager.initializeLate(config -> true, () -> {
            });

            assertNull(copy.getTree());
            assertSame(old.getTree(), ConfigManager.active().get(id));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementThatFailsLeavesTheOlderInstanceRegistered() throws IOException {
        String id = "late-initialization-failed-replacement-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();

            ConfigManager.initializeLate(config -> true, () -> new FailingConfig(id));

            assertSame(old.getTree(), ConfigManager.active().get(id));
            assertTrue(ConfigManager.createdConfigs().contains(old));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aSecondInstanceInTheWindowKeepsSavedValues() throws IOException {
        String id = "late-initialization-duplicate-test.json";
        Files.write(ConfigManager.active().getFolder().resolve(id), "{\"value\": true}".getBytes(StandardCharsets.UTF_8));
        try {
            finishStartup();
            ValueConfig first = new ValueConfig(id);
            AtomicReference<Config> second = new AtomicReference<>();
            ConfigManager.initializeLate(first::equals, () -> second.set(new ValueConfig(id)));
            assertNull(second.get().getTree());
            assertEquals(Boolean.TRUE, first.getProperty("value").get());
            assertTrue(ConfigManager.createdConfigs().contains(second.get()));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void configsHandedOverInTheWindowAreInitialized() {
        finishStartup();
        Config library = new LateConfig("late-initialization-claimed-test.json");
        ConfigManager.claimForLateInitialization(library);
        assertTrue(ConfigManager.createdConfigs().contains(library));
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
