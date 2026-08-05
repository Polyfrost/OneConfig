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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.internal.ui.api.ConfigData;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HudModCardDataTest {
    private static final String OWNER_ID = "hud-card-test";
    private static final String FALLBACK_OWNER_ID = "hud-card-fallback";
    private static final String UNKNOWN_OWNER_ID = "hud-card-unknown";
    private static final String HUD_ID = "hud-card-test-provider";

    private final TestHud hud = new TestHud();
    private final SecondTestHud secondHud = new SecondTestHud();

    @AfterEach
    void cleanUp() {
        HudManager.INSTANCE.unregister(hud, false, false);
        HudManager.INSTANCE.unregister(secondHud, false, false);
        ConfigRegistry.INSTANCE.unregister("oneconfig.builtin");
        ConfigRegistry.INSTANCE.unregister(FALLBACK_OWNER_ID + ".json");
    }

    @Test
    void registeredHudIsExposedAsHudModCard() {
        HudManager.register(hud, OWNER_ID, "combat");

        ConfigData card = registryCardFor(TestHud.class);

        assertEquals("HUD Card Test", card.getTitle());
        assertEquals("combat", card.getIcon());
        assertEquals(Config.Category.HUD, card.getCategory());
        assertEquals(
                "oneconfig.hud:" + OWNER_ID + ":" + HUD_ID + ":" + TestHud.class.getName(),
                card.getId()
        );
        assertNotNull(card.getOnOpen());
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

        assertFalse(HudModCardDataKt.hudModCardConfigs().stream()
                .anyMatch(card -> card.getId().endsWith(":" + TestHud.class.getName())));
    }

    @Test
    void builtinMetadataEntryIsNotShownAsAnotherCard() {
        BuiltinHudRegistrar.register();

        assertFalse(ConfigRegistry.INSTANCE.getModCardConfigs().stream()
                .anyMatch(card -> card.getId().equals("oneconfig.builtin")));
    }

    private static ConfigData registryCardFor(Class<? extends Hud> providerClass) {
        return ConfigRegistry.INSTANCE.getModCardConfigs().stream()
                .filter(card -> card.getId().endsWith(":" + providerClass.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static ConfigData generatedCardFor(Class<? extends Hud> providerClass) {
        return HudModCardDataKt.hudModCardConfigs().stream()
                .filter(card -> card.getId().endsWith(":" + providerClass.getName()))
                .findFirst()
                .orElseThrow();
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
