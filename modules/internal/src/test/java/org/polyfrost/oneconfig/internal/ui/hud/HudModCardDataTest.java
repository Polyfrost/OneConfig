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

package org.polyfrost.oneconfig.internal.ui.hud;

import androidx.compose.runtime.snapshots.Snapshot;
import androidx.compose.runtime.snapshots.SnapshotStateObserver;
import kotlin.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.internal.ui.api.ConfigData;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudModCardDataTest {
    private static final String OWNER_ID = "hud-card-test";
    private static final String FALLBACK_OWNER_ID = "hud-card-fallback";
    private static final String UNKNOWN_OWNER_ID = "hud-card-unknown";
    private static final String NAMESPACED_OWNER_ID = "hud-card-namespaced";
    /** Present in ConfigRegistry's hidden mod card list */
    private static final String HIDDEN_OWNER_ID = "walksylib";
    private static final String HUD_ID = "hud-card-test-provider";

    private final TestHud hud = new TestHud();
    private final SecondTestHud secondHud = new SecondTestHud();
    private final SuffixedTestHud suffixedHud = new SuffixedTestHud();
    private final CompatTestHud compatHud = new CompatTestHud();

    @AfterEach
    void cleanUp() {
        HudManager.INSTANCE.unregister(hud, false, false);
        HudManager.INSTANCE.unregister(secondHud, false, false);
        HudManager.INSTANCE.unregister(suffixedHud, false, false);
        HudManager.INSTANCE.unregister(compatHud, false, false);
        hud.setConfigId(null);
        secondHud.setConfigId(null);
        suffixedHud.setConfigId(null);
        compatHud.setConfigId(null);
        ConfigRegistry.INSTANCE.unregister(BuiltinHudConfigKt.BUILTIN_HUD_CONFIG_ID);
        ConfigRegistry.INSTANCE.unregister(FALLBACK_OWNER_ID + ".json");
        ConfigRegistry.INSTANCE.unregister(NAMESPACED_OWNER_ID + "/main.json");
        ConfigRegistry.INSTANCE.unregister(UNKNOWN_OWNER_ID + "/other.json");
    }

    @Test
    void registeredHudIsExposedAsHudModCard() {
        HudManager.register(hud, OWNER_ID, "combat");

        ConfigData card = registryCardFor(TestHud.class);

        assertEquals("HUD Card Test HUD", card.getTitle());
        assertEquals("combat", card.getIcon());
        assertEquals(Config.Category.HUD, card.getCategory());
        assertEquals(
                "oneconfig.hud:" + OWNER_ID + ":" + HUD_ID + ":" + TestHud.class.getName(),
                card.getId()
        );
        assertNotNull(card.getOnOpen());
    }

    @Test
    void hudTitleAlreadyEndingInHudKeepsASingleSuffix() {
        HudManager.register(suffixedHud, OWNER_ID, "combat");

        assertEquals("Coords HUD", registryCardFor(SuffixedTestHud.class).getTitle());
    }

    @Test
    void registrationInvalidatesSnapshotReadersOfTheCardList() {
        AtomicBoolean invalidated = new AtomicBoolean();
        SnapshotStateObserver observer = new SnapshotStateObserver(command -> {
            command.invoke();
            return Unit.INSTANCE;
        });

        try {
            observer.start();
            observer.observeReads(this, ignored -> {
                invalidated.set(true);
                return Unit.INSTANCE;
            }, () -> {
                HudModCardDataKt.hudModCardConfigs();
                return Unit.INSTANCE;
            });

            HudManager.register(hud, OWNER_ID, "combat");
            Snapshot.Companion.sendApplyNotifications();

            assertTrue(invalidated.get());
        } finally {
            observer.stop();
        }
    }

    @Test
    void providersWithTheSameHudIdStillHaveUniqueCardIds() {
        HudManager.register(hud, OWNER_ID, "combat");
        HudManager.register(secondHud, OWNER_ID, "combat");

        assertNotEquals(registryCardFor(TestHud.class).getId(), registryCardFor(SecondTestHud.class).getId());
    }

    @Test
    void hudCardUsesOwnerMetadataWithoutReplacingOwnerConfig() {
        ConfigData owner = new TestConfigData(
                FALLBACK_OWNER_ID + ".json",
                "Fallback Owner",
                "/assets/fallback/icon.svg"
        );
        ConfigRegistry.INSTANCE.register(owner);
        HudManager.register(hud, FALLBACK_OWNER_ID);

        ConfigData card = generatedCardFor(TestHud.class);

        assertEquals("/assets/fallback/icon.svg", card.getIcon());
        assertEquals(owner, ConfigRegistry.INSTANCE.findById(FALLBACK_OWNER_ID + ".json"));
    }

    @Test
    void hudCardResolvesAnOwnerStoredUnderItsModNamespace() {
        ConfigRegistry.INSTANCE.register(new TestConfigData(
                NAMESPACED_OWNER_ID + "/main.json",
                "Namespaced Owner",
                "/assets/namespaced/icon.svg"
        ));
        HudManager.register(hud, NAMESPACED_OWNER_ID);

        assertEquals("/assets/namespaced/icon.svg", generatedCardFor(TestHud.class).getIcon());
    }

    @Test
    void hudCardDoesNotAdoptAnUnrelatedConfigFromTheSameNamespace() {
        ConfigRegistry.INSTANCE.register(new TestConfigData(
                UNKNOWN_OWNER_ID + "/other.json",
                "Unrelated Neighbour",
                "/assets/unrelated/icon.svg"
        ));
        HudManager.register(hud, UNKNOWN_OWNER_ID + "/hud");

        ConfigData card = generatedCardFor(TestHud.class);

        assertEquals("hud", card.getIcon());
        assertNull(card.getAuthors());
    }

    @Test
    void hudCardWithoutOwnerMetadataUsesHudIcon() {
        HudManager.register(hud, UNKNOWN_OWNER_ID);

        assertEquals("hud", generatedCardFor(TestHud.class).getIcon());
    }

    @Test
    void hudWithoutConfigIdUsesBuiltinIcon() {
        HudManager.register(hud);

        assertEquals(BuiltinHudConfigKt.BUILTIN_HUD_ICON, generatedCardFor(TestHud.class).getIcon());
    }

    @Test
    void unregisteredHudIsRemovedFromModCards() {
        HudManager.register(hud, OWNER_ID, "combat");

        HudManager.INSTANCE.unregister(hud, false, false);

        assertTrue(cardsFor(TestHud.class).isEmpty());
    }

    @Test
    void compatHudsAreNotShownAsModCards() {
        HudManager.register(compatHud, OWNER_ID, "combat");

        assertTrue(HudManager.INSTANCE.providers().contains(compatHud));
        assertTrue(cardsFor(CompatTestHud.class).isEmpty());
    }

    @Test
    void hudsOwnedByAHiddenModAreNotShownAsModCards() {
        HudManager.register(hud, HIDDEN_OWNER_ID);

        assertTrue(cardsFor(TestHud.class).isEmpty());
    }

    @Test
    void builtinMetadataEntryIsNotShownAsAnotherCard() {
        BuiltinHudRegistrar.register();

        assertFalse(ConfigRegistry.INSTANCE.getModCardConfigs().stream()
                .anyMatch(card -> card.getId().equals(BuiltinHudConfigKt.BUILTIN_HUD_CONFIG_ID)));
    }

    @Test
    void builtinHudsStillGetCardsAlthoughTheirOwnerIsHiddenAsACard() {
        HudManager.register(hud);
        BuiltinHudRegistrar.register();

        ConfigData card = registryCardFor(TestHud.class);

        assertEquals(
                "oneconfig.hud:" + BuiltinHudConfigKt.BUILTIN_HUD_CONFIG_ID + ":" + HUD_ID + ":" + TestHud.class.getName(),
                card.getId()
        );
    }

    private static List<ConfigData> cardsFor(Class<? extends Hud> providerClass) {
        return HudModCardDataKt.hudModCardConfigs().stream()
                .filter(card -> card.getId().endsWith(":" + providerClass.getName()))
                .toList();
    }

    private static ConfigData registryCardFor(Class<? extends Hud> providerClass) {
        return ConfigRegistry.INSTANCE.getModCardConfigs().stream()
                .filter(card -> card.getId().endsWith(":" + providerClass.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static ConfigData generatedCardFor(Class<? extends Hud> providerClass) {
        return cardsFor(providerClass).stream().findFirst().orElseThrow();
    }

    private static final class TestHud extends TextHud {
        private TestHud() {
            super(HUD_ID, "HUD Card Test", Hud.Category.Companion.getINFO(), "Test", "");
        }

        @Override
        protected String getText() {
            return "HUD";
        }
    }

    private static final class SecondTestHud extends TextHud {
        private SecondTestHud() {
            super(HUD_ID, "Second HUD Card Test", Hud.Category.Companion.getINFO(), "Test", "");
        }

        @Override
        protected String getText() {
            return "HUD";
        }
    }

    private static final class SuffixedTestHud extends TextHud {
        private SuffixedTestHud() {
            super("hud-card-test-suffixed", "Coords HUD", Hud.Category.Companion.getINFO(), "Test", "");
        }

        @Override
        protected String getText() {
            return "HUD";
        }
    }

    private static final class CompatTestHud extends TextHud {
        private CompatTestHud() {
            super("compat-hud-card-test", "Compat HUD Card Test", Hud.Category.Companion.getCOMPAT(), "Test", "");
        }

        @Override
        protected String getText() {
            return "HUD";
        }
    }

    private static final class TestConfigData implements ConfigData {
        private final String id;
        private final String title;
        private final String icon;

        private TestConfigData(String id, String title, String icon) {
            this.id = id;
            this.title = title;
            this.icon = icon;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Object getTitle() {
            return title;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public ConfigSource getSource() {
            return ConfigSource.OC;
        }

        @Override
        public Config.Category getCategory() {
            return Config.Category.OTHER;
        }
    }
}
