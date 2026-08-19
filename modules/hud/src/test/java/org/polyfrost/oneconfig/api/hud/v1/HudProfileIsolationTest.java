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

package org.polyfrost.oneconfig.api.hud.v1;

import kotlin.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.polyfrost.oneconfig.api.config.v1.Tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudProfileIsolationTest {
    private static final String PROFILE = "oc_test_hud_profile";

    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        ConfigManager.openProfile("");
        deleteProfile();
        wipeHudState();
    }

    @AfterEach
    void tearDown() throws Exception {
        ConfigManager.openProfile("");
        HudManager.INSTANCE.unregister(new ProfileTestHud(), true, false);
        HudManager.INSTANCE.unregister(new LateRegisteredHud(), true, false);
        wipeHudState();
        deleteProfile();
    }

    @Test
    void hudSettingsDoNotLeakBetweenProfiles() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();
        assertEquals(1, instances());
        hud().setHidden(true);
        ConfigManager.active().saveAll();

        ConfigManager.createProfile(PROFILE);
        assertEquals(1, instances(), "the blank profile should have its own instance of the HUD");
        assertFalse(hud().getHidden(), "a blank profile must start from the HUD defaults");

        hud().setHidden(false);
        ConfigManager.active().saveAll();

        ConfigManager.openProfile("");
        assertTrue(hud().getHidden(), "the Default profile must keep its own HUD settings");

        ConfigManager.openProfile(PROFILE);
        assertFalse(hud().getHidden(), "the blank profile must keep its own HUD settings");
    }

    @Test
    void aHudWhoseStaticSizeIsNotKnownYetKeepsItsResetDefaultOpen() throws Exception {
        UnsizedHud provider = new UnsizedHud();
        HudManager.register(provider);
        try {
            launch();
            Tree tree = HudManager.INSTANCE.getHudsOfType(UnsizedHud.class).get(0).getTree();
            assertNull(tree.getProp("staticW").getMetadata("default"),
                    "a zero staticW must not be recorded as the reset default");
            assertNull(tree.getProp("staticH").getMetadata("default"),
                    "a zero staticH must not be recorded as the reset default");
        } finally {
            HudManager.INSTANCE.unregister(provider, true, true);
        }
    }

    @Test
    void switchingProfilesDoesNotFireHudOptionCallbacks() throws Exception {
        CallbackHud provider = new CallbackHud();
        HudManager.register(provider);
        try {
            launch();
            Hud loaded = HudManager.INSTANCE.getHudsOfType(CallbackHud.class).get(0);
            loaded.setRelativeX(loaded.getRelativeX() + 0.25f);
            CallbackHud.positionCallbacks = 0;

            ConfigManager.createProfile(PROFILE);
            ConfigManager.openProfile("");

            assertEquals(0, CallbackHud.positionCallbacks,
                    "restoring provider defaults for a profile switch must not fire the HUD's own callbacks");
        } finally {
            HudManager.INSTANCE.unregister(provider, true, true);
        }
    }

    @Test
    void wrappedHudPositionIsPerProfile() throws Exception {
        launch();
        TestWrapper wrapper = new TestWrapper();
        wrapper.register();

        wrapper.setX(100f);
        ConfigManager.createProfile(PROFILE);
        assertEquals(10f, wrapper.getX(), "a new profile must start from the wrapped HUD's default position");

        wrapper.setX(200f);
        ConfigManager.openProfile("");
        assertEquals(100f, wrapper.getX(), "the Default profile must keep its own wrapped HUD position");

        ConfigManager.openProfile(PROFILE);
        assertEquals(200f, wrapper.getX(), "the other profile must keep its own wrapped HUD position");
        assertTrue(wrapper.saves > 0, "applying a profile must let the wrapped mod persist the move");
    }

    @Test
    void clonedProfileKeepsHudSettings() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();
        hud().setHidden(true);
        ConfigManager.active().saveAll();

        ConfigManager.cloneProfile("", PROFILE);

        assertEquals(1, instances());
        assertTrue(hud().getHidden(), "a cloned profile must copy the source HUD settings");
        assertEquals(Boolean.FALSE, hud().getTree().getProp("hidden").getMetadata("default"),
                "Reset must still use the HUD's code default after cloning a changed profile");
    }

    @Test
    void hudDeletedInOneProfileStillExistsInTheOther() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();

        ConfigManager.createProfile(PROFILE);

        HudManager.INSTANCE.removeHud(hud(), true);
        assertEquals(0, instances());

        ConfigManager.openProfile("");
        assertEquals(1, instances(), "deleting a HUD in one profile must not delete it in another");
    }

    @Test
    void hudTreeOfTheOldProfileIsNotCarriedOntoTheNewOne() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();
        Object old = hud().getTree();

        ConfigManager.createProfile(PROFILE);
        assertFalse(ConfigManager.active().trees().stream().anyMatch(t -> t == old),
                "the old profile's HUD tree must not be carried onto the new profile");

        assertFalse(hud().getTree() == old, "the reload must build the HUD from the new profile's own tree");
    }

    @Test
    void failedDefaultHudIsRolledBackWithoutStoppingOtherProviders() throws Exception {
        FailingSetupHud failing = new FailingSetupHud();
        HealthySetupHud healthy = new HealthySetupHud();
        HudManager.register(failing, healthy);
        try {
            launch();

            assertTrue(HudManager.INSTANCE.getHudsOfType(FailingSetupHud.class).isEmpty());
            assertFalse(failing.isReal(), "a failed single-instance provider must be usable again");
            assertFalse(ConfigManager.active().trees().stream()
                            .anyMatch(tree -> "huds/test-failing-setup".equals(tree.getID())),
                    "a failed candidate must not remain tracked by the backend");
            assertEquals(1, HudManager.INSTANCE.getHudsOfType(HealthySetupHud.class).size(),
                    "one broken provider must not abort the rest of the HUD load");
        } finally {
            HudManager.INSTANCE.unregister(failing, true, false);
            HudManager.INSTANCE.unregister(healthy, true, false);
            ConfigManager.active().delete("huds/test-failing-setup");
            ConfigManager.active().delete("huds/test-healthy-setup");
        }
    }

    @Test
    void linkageErrorInOneHudDoesNotAbortOtherProviders() throws Exception {
        LinkageFailingSetupHud failing = new LinkageFailingSetupHud();
        HealthySetupHud healthy = new HealthySetupHud();
        HudManager.register(failing, healthy);
        try {
            launch();

            assertTrue(HudManager.INSTANCE.getHudsOfType(LinkageFailingSetupHud.class).isEmpty());
            assertFalse(failing.isReal());
            assertEquals(1, HudManager.INSTANCE.getHudsOfType(HealthySetupHud.class).size(),
                    "a missing optional HUD dependency must not stop healthy providers");
        } finally {
            HudManager.INSTANCE.unregister(failing, true, false);
            HudManager.INSTANCE.unregister(healthy, true, false);
            ConfigManager.active().delete("huds/test-linkage-failing-setup");
            ConfigManager.active().delete("huds/test-healthy-setup");
        }
    }

    @Test
    void linkageErrorWhileCheckingADefaultHudDoesNotAbortOtherProviders() throws Exception {
        EligibilityFailingHud failing = new EligibilityFailingHud();
        HealthySetupHud healthy = new HealthySetupHud();
        HudManager.register(failing, healthy);
        try {
            launch();

            assertTrue(HudManager.INSTANCE.getHudsOfType(EligibilityFailingHud.class).isEmpty());
            assertEquals(1, HudManager.INSTANCE.getHudsOfType(HealthySetupHud.class).size(),
                    "a broken default-visibility check must not stop healthy providers");
        } finally {
            HudManager.INSTANCE.unregister(failing, true, false);
            HudManager.INSTANCE.unregister(healthy, true, false);
            ConfigManager.active().delete("huds/test-eligibility-failing");
            ConfigManager.active().delete("huds/test-healthy-setup");
        }
    }

    @Test
    void providerRegisteredAfterInitializationIsLoadedWithoutAProfileSwitch() throws Exception {
        LateRegisteredHud provider = new LateRegisteredHud();
        launch();
        assertTrue(HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).isEmpty());

        HudManager.register(provider);
        try {
            drainPendingProfileReload();
            assertEquals(1, HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).size(),
                    "a provider registered by a later startup handler must be available immediately");
        } finally {
            HudManager.INSTANCE.unregister(provider, true, true);
        }
    }

    @Test
    void lateRegistrationDoesNotBreakAHudAlreadyLoadedFromDisk() throws Exception {
        LateRegisteredHud initialProvider = new LateRegisteredHud();
        HudManager.register(initialProvider);
        launch();
        assertEquals(1, HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).size());
        ConfigManager.active().saveAll();

        // Simulate the next launch loading the persisted class before that mod reaches its own
        // InitializationEvent handler. The backend is intentionally kept warm to catch tree merges.
        HudManager.INSTANCE.unregister(initialProvider, true, false);
        launch();
        assertEquals(1, HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).size());

        LateRegisteredHud registeredProvider = new LateRegisteredHud();
        HudManager.register(registeredProvider);
        try {
            drainPendingProfileReload();
            assertEquals(1, HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).size());
            Hud loaded = HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).get(0);
            assertTrue(loaded.getTree().getProp("prefix") != null,
                    "the same-backend reload must not clear the rebuilt HUD tree");
        } finally {
            HudManager.INSTANCE.unregister(registeredProvider, true, true);
        }
    }

    @Test
    void lateRegistrationDoesNotDiscardUnsavedHudChanges() throws Exception {
        ProfileTestHud existingProvider = new ProfileTestHud();
        LateRegisteredHud lateProvider = new LateRegisteredHud();
        HudManager.register(existingProvider);
        launch();
        hud().setHidden(true);

        HudManager.register(lateProvider);
        try {
            drainPendingProfileReload();
            assertTrue(hud().getHidden(),
                    "reloading for a late provider must first persist the live HUD state");
            assertEquals(1, HudManager.INSTANCE.getHudsOfType(LateRegisteredHud.class).size());
        } finally {
            HudManager.INSTANCE.unregister(lateProvider, true, true);
        }
    }

    private static Hud hud() {
        return HudManager.INSTANCE.getHudsOfType(ProfileTestHud.class).get(0);
    }

    private static int instances() {
        return HudManager.INSTANCE.getHudsOfType(ProfileTestHud.class).size();
    }

    private static void launch() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        knownProviders().clear();
        set("registryTree", null);
        set("init", false);
        for (Hud provider : new ArrayList<>(HudManager.INSTANCE.providers())) {
            set(provider, "tree", null);
        }
        HudManager.INSTANCE.initialize();
    }

    private static void deleteProfile() throws IOException {
        deleteRecursively(ConfigManager.PROFILES_DIR.resolve(PROFILE));
    }

    private static void wipeHudState() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        knownProviders().clear();
        set("registryTree", null);
        set("init", false);
        pendingProfileReload().set(null);
        Path folder = ConfigManager.active().getFolder();
        deleteRecursively(folder.resolve("huds"));
        Files.deleteIfExists(folder.resolve("hud-registry.json"));
        ConfigManager.active().delete("hud-registry.json");
        for (Path p : trackedHudTrees()) ConfigManager.active().delete(p.toString());
    }

    private static Collection<Path> trackedHudTrees() {
        ArrayList<Path> out = new ArrayList<>();
        ConfigManager.active().trees().forEach(t -> {
            String id = t.getID();
            if (id != null && id.startsWith("huds")) out.add(Paths.get(id));
        });
        return out;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path p : (Iterable<Path>) stream.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<String> knownProviders() throws Exception {
        Field f = HudManager.class.getDeclaredField("knownProviders");
        f.setAccessible(true);
        return (java.util.Set<String>) f.get(HudManager.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    private static AtomicReference<String> pendingProfileReload() throws Exception {
        Field f = HudManager.class.getDeclaredField("pendingProfileReload");
        f.setAccessible(true);
        return (AtomicReference<String>) f.get(HudManager.INSTANCE);
    }

    private static void set(String name, Object value) throws Exception {
        set(HudManager.INSTANCE, name, value);
    }

    private static void drainPendingProfileReload() throws Exception {
        Method method = HudManager.class.getDeclaredMethod("drainProfileReload");
        method.setAccessible(true);
        method.invoke(HudManager.INSTANCE);
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        Class<?> cls = owner.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                f.set(owner, value);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    static class TestWrapper implements OneConfigHudWrapper {
        private String id = "test-wrapped-hud";
        private String name = "Test Wrapped HUD";
        private float x = 10f;
        private float y = 20f;
        int saves = 0;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String value) {
            id = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String value) {
            name = value;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public void setX(float value) {
            x = value;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public void setY(float value) {
            y = value;
        }

        @Override
        public float getScale() {
            return 1f;
        }

        @Override
        public void setScale(float value) {
        }

        @Override
        public float getScaledWidth() {
            return 20f;
        }

        @Override
        public void setScaledWidth(float value) {
        }

        @Override
        public float getScaledHeight() {
            return 10f;
        }

        @Override
        public void setScaledHeight(float value) {
        }

        @Override
        public void save() {
            saves++;
        }
    }

    static class CallbackHud extends TextHud {
        static int positionCallbacks = 0;

        CallbackHud() {
            super("test-callback", "Test Callback HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public void setup() {
            addCallback("relativeX", (Runnable) () -> positionCallbacks++);
        }

        @Override
        public String getText() {
            return "test";
        }
    }

    static class ProfileTestHud extends TextHud {
        ProfileTestHud() {
            super("test-profile-isolation", "Test Profile Isolation HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public Pair<Float, Float> defaultPosition() {
            return new Pair<>(10f, 10f);
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public String getText() {
            return "test";
        }
    }

    /** Stands in for a wrapped external HUD, whose size is not known when its tree is built. */
    static class UnsizedHud extends TextHud {
        UnsizedHud() {
            super("test-unsized", "Test Unsized HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public float getStaticW() {
            return 0f;
        }

        @Override
        public void setStaticW(float value) {
        }

        @Override
        public float getStaticH() {
            return 0f;
        }

        @Override
        public void setStaticH(float value) {
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public String getText() {
            return "test";
        }
    }

    static class LateRegisteredHud extends TextHud {
        LateRegisteredHud() {
            super("test-late-registration", "Test Late Registration HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public Pair<Float, Float> defaultPosition() {
            return new Pair<>(10f, 10f);
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public String getText() {
            return "late";
        }
    }

    static class FailingSetupHud extends TextHud {
        FailingSetupHud() {
            super("test-failing-setup", "Failing Setup HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public void setup() {
            throw new IllegalStateException("setup failed");
        }

        @Override
        public String getText() {
            return "fail";
        }
    }

    static class HealthySetupHud extends TextHud {
        HealthySetupHud() {
            super("test-healthy-setup", "Healthy Setup HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public String getText() {
            return "healthy";
        }
    }

    static class LinkageFailingSetupHud extends TextHud {
        LinkageFailingSetupHud() {
            super("test-linkage-failing-setup", "Linkage Failing Setup HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public void setup() {
            throw new NoClassDefFoundError("missing.optional.HudDependency");
        }

        @Override
        public String getText() {
            return "linkage-fail";
        }
    }

    static class EligibilityFailingHud extends TextHud {
        EligibilityFailingHud() {
            super("test-eligibility-failing", "Eligibility Failing HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public boolean showByDefault() {
            throw new NoClassDefFoundError("missing.optional.VisibilityDependency");
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public String getText() {
            return "eligibility-fail";
        }
    }
}
