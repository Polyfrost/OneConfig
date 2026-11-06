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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    static class FailsOnceRegisteredConfig extends ValueConfig {
        FailsOnceRegisteredConfig(String id) {
            super(id);
        }

        @Override
        protected void initialize(boolean byConfigManager) {
            super.initialize(byConfigManager);
            throw new IllegalStateException("this config fails once registered");
        }
    }

    static class ValueHashedConfig extends LateConfig {
        private final String name;

        ValueHashedConfig(String id, String name) {
            super(id);
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ValueHashedConfig && name.equals(((ValueHashedConfig) other).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
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
    void aConfigHashedByItsFieldsCanBeCreatedLate() throws IOException {
        String first = "late-initialization-hashed-first-test.json";
        String second = "late-initialization-hashed-second-test.json";
        try {
            finishStartup();
            Config one = new ValueHashedConfig(first, "same");
            Config other = new ValueHashedConfig(second, "same");
            ConfigManager.initializeLate(config -> config == one || config == other, () -> {
            });
            assertNotNull(one.getTree(), "instances that compare equal are still separate configs");
            assertNotNull(other.getTree(), "instances that compare equal are still separate configs");
        } finally {
            ConfigManager.active().unregister(first);
            ConfigManager.active().unregister(second);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(first));
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(second));
        }
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
    void aProfileSwitchWaitsForAnOffModToComeBack() throws IOException {
        String id = "late-initialization-off-test.json";
        String profile = "late-initialization-off-profile";
        AtomicInteger built = new AtomicInteger();
        class CountedConfig extends ValueConfig {
            CountedConfig(String configId) {
                super(configId);
            }

            @Override
            protected void onInitialized(Tree tree) {
                built.incrementAndGet();
            }
        }
        try {
            Config config = new CountedConfig(id);
            finishStartup();
            assertEquals(1, built.get());

            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(profile);
            ConfigManager.openProfile(profile);
            assertEquals(1, built.get());
            assertFalse(ConfigManager.active().exists(id));

            assertTrue(ConfigManager.setOff(Set.of()));
            assertEquals(2, built.get());
            assertSame(config.getTree(), ConfigManager.active().get(id));
            assertFalse(ConfigManager.setOff(Set.of()));
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aProfileCreatedWhileAModIsOffStartsFromItsDefaults() throws IOException {
        String id = "late-initialization-off-defaults-test.json";
        String profile = "late-initialization-off-defaults-profile";
        try {
            ValueConfig config = new ValueConfig(id);
            finishStartup();
            config.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(profile);
            ConfigManager.openProfile("");
            ConfigManager.setOff(Set.of());
            assertEquals(Boolean.TRUE, config.getProperty("value").get());

            ConfigManager.openProfile(profile);
            assertEquals(Boolean.FALSE, config.getProperty("value").get());
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aProfileCreatedAndKeptWhileAModIsOffGivesItItsDefaultsOnceBack() throws IOException {
        String id = "late-initialization-off-kept-test.json";
        String profile = "late-initialization-off-kept-profile";
        try {
            ValueConfig config = new ValueConfig(id);
            finishStartup();
            config.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(profile);
            ConfigManager.setOff(Set.of());

            assertEquals(Boolean.FALSE, config.getProperty("value").get());
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aTreeOfAnOffModSitsOutANewProfileAndGetsItsDefaultsOnceBack() throws IOException {
        String id = "late-initialization-off-tree-test.json";
        String profile = "late-initialization-off-tree-profile";
        Property<Boolean> value = Properties.simple("value", "Value", "", false);
        Tree tree = Tree.tree(id).put(value);
        AtomicInteger calls = new AtomicInteger();
        try {
            ConfigManager.active().register(tree);
            value.setAs(true);
            ConfigManager.active().save(id);
            value.addCallback(set -> {
                calls.incrementAndGet();
                return false;
            });

            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(profile);
            assertEquals(0, calls.get(), "its callbacks are the mod's code, which is off");
            assertEquals(Boolean.TRUE, value.get());
            assertFalse(ConfigManager.active().exists(id));

            assertTrue(ConfigManager.setOff(Set.of()));
            assertSame(tree, ConfigManager.active().get(id));
            assertEquals(Boolean.FALSE, value.get());
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aProfileCreatedWhileATreesModIsOffKeepsItsDefaultsWhenItComesBackElsewhere() throws IOException {
        String id = "late-initialization-off-tree-elsewhere-test.json";
        String profile = "late-initialization-off-tree-elsewhere-profile";
        Property<Boolean> value = Properties.simple("value", "Value", "", false);
        Tree tree = Tree.tree(id).put(value);
        try {
            ConfigManager.active().register(tree);
            value.setAs(true);
            ConfigManager.active().save(id);

            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(profile);
            ConfigManager.openProfile("");
            assertTrue(ConfigManager.setOff(Set.of()));
            assertEquals(Boolean.TRUE, value.get());

            ConfigManager.openProfile(profile);
            assertEquals(Boolean.FALSE, value.get());
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
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
    void aReplacementThatPreloadsKeepsTheSavedValues() throws IOException {
        String id = "late-initialization-preloaded-replacement-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            AtomicReference<Config> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> {
                replacement.set(new ValueConfig(id));
                replacement.get().preload();
            });

            assertSame(replacement.get().getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, replacement.get().getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementAnEntrypointsWorkerBuiltTakesOver() throws IOException {
        String id = "late-initialization-worker-replacement-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            AtomicReference<Config> replacement = new AtomicReference<>();
            boolean waiting = ConfigManager.initializeLate(config -> true, () -> {
                Thread worker = new Thread(() -> replacement.set(new ValueConfig(id)));
                worker.start();
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });

            assertFalse(waiting);
            assertSame(replacement.get().getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, replacement.get().getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementAWorkerIsStillBuildingTakesOverInTheNextWindow() throws IOException, InterruptedException {
        String id = "late-initialization-worker-unfinished-test.json";
        try {
            Config old = new LateConfig(id);
            finishStartup();

            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Config> replacement = new AtomicReference<>();
            AtomicReference<Thread> worker = new AtomicReference<>();
            assertTrue(ConfigManager.initializeLate(config -> true, () -> {
                worker.set(new Thread(() -> {
                    try {
                        replacement.set(new BlockedConfig(id, entered, release));
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }));
                worker.get().start();
                try {
                    entered.await();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }));
            assertSame(old.getTree(), ConfigManager.active().get(id));

            release.countDown();
            worker.get().join();
            assertFalse(ConfigManager.initializeLate(config -> true, () -> {
            }));
            assertSame(replacement.get().getTree(), ConfigManager.active().get(id));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementReadsTheSavedValuesRightAfterItsPreload() throws IOException {
        String id = "late-initialization-preload-reads-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            AtomicReference<ValueConfig> replacement = new AtomicReference<>();
            AtomicReference<Property<?>> read = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> {
                replacement.set(new ValueConfig(id));
                replacement.get().preload();
                assertTrue(replacement.get().value);
                read.set(replacement.get().getProperty("value"));
                assertEquals(Boolean.TRUE, read.get().get());
                assertSame(old.getTree(), ConfigManager.active().get(id));
            });

            assertSame(replacement.get().getTree(), ConfigManager.active().get(id));
            assertSame(read.get(), replacement.get().getProperty("value"), "what a mod kept from its setup must stay live");
            assertEquals(Boolean.TRUE, read.get().get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    static class TwoValueConfig extends ValueConfig {
        @Switch(title = "Other")
        public boolean other = false;

        TwoValueConfig(String id) {
            super(id);
        }
    }

    @Test
    void whatAReplacementWritesAfterItsPreloadSurvivesItsRegistration() throws IOException {
        String id = "late-initialization-preload-writes-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            AtomicReference<ValueConfig> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> {
                replacement.set(new ValueConfig(id));
                replacement.get().preload();
                assertTrue(replacement.get().value);
                replacement.get().value = false;
            });

            assertSame(replacement.get().getTree(), ConfigManager.active().get(id));
            assertFalse(replacement.get().value);
            assertEquals(Boolean.FALSE, replacement.get().getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(ConfigManager.backup().getFolder().resolve(id));
        }
    }

    @Test
    void whatAReplacementLeftAloneTakesTheOlderInstancesNewerValue() throws IOException {
        String id = "late-initialization-preload-newer-test.json";
        try {
            TwoValueConfig old = new TwoValueConfig(id);
            finishStartup();

            AtomicReference<TwoValueConfig> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> {
                replacement.set(new TwoValueConfig(id));
                replacement.get().preload();
                replacement.get().other = true;
                old.getProperty("value").setAs(true);
            });

            assertTrue(replacement.get().value, "the older instance is saved after the replacement read its file");
            assertTrue(replacement.get().other);
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(ConfigManager.backup().getFolder().resolve(id));
        }
    }

    @Test
    void aWriteAfterThePreloadStaysOutOfTheDefaultsBackup() throws IOException {
        String id = "late-initialization-preload-writes-defaults-test.json";
        try {
            new ValueConfig(id);
            finishStartup();

            AtomicReference<ValueConfig> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> {
                replacement.set(new ValueConfig(id));
                replacement.get().preload();
                replacement.get().value = true;
            });
            assertTrue(replacement.get().value);

            replacement.get().restoreDefaults();
            assertFalse(replacement.get().value);
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(ConfigManager.backup().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementThatReadItsSavedValuesEarlyStillResetsToItsDefaults() throws IOException {
        String id = "late-initialization-preload-defaults-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            AtomicReference<ValueConfig> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> {
                replacement.set(new ValueConfig(id));
                replacement.get().preload();
            });
            assertEquals(Boolean.TRUE, replacement.get().getProperty("value").get());

            replacement.get().restoreDefaults();
            assertEquals(Boolean.FALSE, replacement.get().getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(ConfigManager.backup().getFolder().resolve(id));
        }
    }

    // what Hotwire does around the entrypoints of a mod it adds
    private static void startWithHotwire(Runnable entrypoints) throws ReflectiveOperationException {
        Field initialized = ConfigManager.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(null, false);
        entrypoints.run();
        ConfigManager.initialize();
    }

    @Test
    void aCopyAnAddedModMakesWaitsForItsWindowToReplaceTheOlderInstance() throws IOException, ReflectiveOperationException {
        String id = "late-initialization-reopened-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            AtomicReference<Config> copy = new AtomicReference<>();
            startWithHotwire(() -> copy.set(new ValueConfig(id)));

            assertNull(copy.get().getTree());
            assertSame(old.getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, ConfigManager.active().load(id).getProp("value").get());

            ConfigManager.initializeLate(config -> true, () -> {
            });

            assertSame(copy.get().getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, copy.get().getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aCopyAnAddedModPreloadsLeavesTheSavedValues() throws IOException, ReflectiveOperationException {
        String id = "late-initialization-reopened-preload-test.json";
        AtomicReference<Config> copy = new AtomicReference<>();
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            startWithHotwire(() -> {
                copy.set(new ValueConfig(id));
                copy.get().preload();
            });

            assertNull(copy.get().getTree());
            assertEquals(Boolean.TRUE, ConfigManager.active().load(id).getProp("value").get());
        } finally {
            ConfigManager.cancelInitialization(copy.get());
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aCopyAnAddedModStartsWithReadsTheSavedValues() throws IOException, ReflectiveOperationException {
        String id = "late-initialization-reopened-reads-test.json";
        AtomicReference<ValueConfig> copy = new AtomicReference<>();
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            startWithHotwire(() -> {
                copy.set(new ValueConfig(id));
                assertEquals(Boolean.TRUE, copy.get().getProperty("value").get());
                assertTrue(copy.get().value);
            });

            assertSame(old.getTree(), ConfigManager.active().get(id));
        } finally {
            ConfigManager.cancelInitialization(copy.get());
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void anAddedModsNewConfigStillInitializesWhenItStarts() throws ReflectiveOperationException {
        AtomicReference<Config> created = new AtomicReference<>();
        finishStartup();
        startWithHotwire(() -> created.set(new LateConfig("late-initialization-reopened-new-test.json")));
        assertNotNull(created.get().getTree());
    }

    @Test
    void aPreloadedCopyFromAnotherModLeavesTheSavedValues() throws IOException {
        String id = "late-initialization-preloaded-copy-test.json";
        AtomicReference<Config> copy = new AtomicReference<>();
        try {
            ValueConfig theirs = new ValueConfig(id);
            finishStartup();
            theirs.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            ConfigManager.initializeLate(config -> config != theirs, () -> {
                copy.set(new ValueConfig(id));
                copy.get().preload();
            });

            assertNull(copy.get().getTree());
            assertSame(theirs.getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, theirs.getProperty("value").get());
            assertEquals(Boolean.TRUE, ConfigManager.active().load(id).getProp("value").get());
        } finally {
            ConfigManager.cancelInitialization(copy.get());
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aCopyPreloadedBeforeItsWindowLeavesTheSavedValues() throws IOException {
        String id = "late-initialization-preloaded-early-test.json";
        Config copy = null;
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);
            ConfigManager.active().save(id);

            ConfigManager.setAwaitingLateWindow(config -> config.id.equals(id));
            copy = new ValueConfig(id);
            copy.preload();

            assertNull(copy.getTree());
            assertEquals(Boolean.TRUE, old.getProperty("value").get());
        } finally {
            ConfigManager.cancelInitialization(copy);
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
    void aReplacementThatFailsOnceRegisteredGivesTheOlderInstanceItsPlaceBack() throws IOException {
        String id = "late-initialization-failed-registered-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();

            AtomicReference<Config> failed = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> failed.set(new FailsOnceRegisteredConfig(id)));

            assertSame(old.getTree(), ConfigManager.active().get(id));
            // what a profile switch goes on to rebuild
            assertTrue(ConfigManager.createdConfigs().contains(old));
            assertFalse(ConfigManager.createdConfigs().contains(failed.get()));
        } finally {
            ConfigManager.active().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementThatFailsLeavesTheOlderDefaultsToResetTo() throws IOException {
        String id = "late-initialization-failed-defaults-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);

            ConfigManager.initializeLate(config -> true, () -> new FailsOnceRegisteredConfig(id) {
                {
                    value = true;
                }
            });
            old.restoreDefaults();

            assertEquals(Boolean.FALSE, old.getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(ConfigManager.backup().getFolder().resolve(id));
        }
    }

    @Test
    void aReplacementThatFailsLeavesAnUnreadableDefaultsBackupInPlace() throws IOException {
        String id = "late-initialization-unreadable-defaults-test.json";
        Path backup = ConfigManager.backup().getFolder().resolve(id);
        try {
            new ValueConfig(id);
            finishStartup();
            // reading a directory fails whatever user runs this, unlike a file without read permission
            Files.delete(backup);
            Files.createDirectory(backup);

            ConfigManager.initializeLate(config -> true, () -> new FailingConfig(id));

            assertTrue(Files.exists(backup));
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(backup);
        }
    }

    @Test
    void aReplacementResetsToItsOwnDefaultsAfterTheOlderInstanceWasReset() throws IOException {
        String id = "late-initialization-replaced-defaults-test.json";
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.restoreDefaults();

            AtomicReference<ValueConfig> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> replacement.set(new ValueConfig(id) {
                {
                    value = true;
                }
            }));
            replacement.get().getProperty("value").setAs(false);
            replacement.get().restoreDefaults();

            assertEquals(Boolean.TRUE, replacement.get().getProperty("value").get());
        } finally {
            ConfigManager.active().unregister(id);
            ConfigManager.backup().unregister(id);
            Files.deleteIfExists(ConfigManager.active().getFolder().resolve(id));
            Files.deleteIfExists(ConfigManager.backup().getFolder().resolve(id));
        }
    }

    @Test
    void aFailedSaveKeepsTheOlderInstanceAndItsUnsavedValues() throws IOException {
        String id = "late-initialization-unsaved-test.json";
        // a directory where the save writes its temporary file
        Path blocker = ConfigManager.active().getFolder().resolve(id + ".tmp");
        try {
            ValueConfig old = new ValueConfig(id);
            finishStartup();
            old.getProperty("value").setAs(true);
            Files.createDirectories(blocker);

            AtomicReference<Config> replacement = new AtomicReference<>();
            ConfigManager.initializeLate(config -> true, () -> replacement.set(new ValueConfig(id)));

            assertNull(replacement.get().getTree());
            assertSame(old.getTree(), ConfigManager.active().get(id));
            assertEquals(Boolean.TRUE, old.getProperty("value").get());
        } finally {
            Files.deleteIfExists(blocker);
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
