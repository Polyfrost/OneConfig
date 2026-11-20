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

package org.polyfrost.oneconfig.internal.compat;

import dev.moon.hotwire.api.HotwireApi;
import dev.moon.hotwire.api.ModLifecycleListener;
import dev.moon.hotwire.api.ModState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.platform.v1.internal.CompatibilityPlatformImpl;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;

/**
 * OneConfig's part in Hotwire turning mods off and on, adding and updating them while the game runs. Hotwire takes a
 * mod's objects out of OneConfig's collections and puts the same ones back itself, so what is left is what needs to
 * know about profiles and about when new configs are initialized. Every call comes on the game thread.
 * <br>
 * This is the only class that names Hotwire's types, so it must only be loaded when Hotwire is installed.
 */
public final class HotwireLifecycle implements ModLifecycleListener {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Hotwire");

    private HotwireLifecycle() {
    }

    public static void register() {
        HotwireApi.getInstance().addLifecycleListener(new HotwireLifecycle());
    }

    // the whole profile rather than what these mods own, which Hotwire decides in ways OneConfig cannot mirror
    @Override
    public void prepareOff(List<String> ids) {
        try {
            ConfigManager.active().saveAll();
            CompatSnapshots.INSTANCE.onProfileSaving(ConfigManager.activeProfile());
        } catch (Throwable t) {
            LOGGER.error("Failed to save the settings of {} before Hotwire turns them off", ids, t);
        }
    }

    // by what is out of place rather than by which mods came back, as their objects only are if the profile changed
    @Override
    public void finishOn(List<String> ids) {
        try {
            refreshMods();
            ConfigManager.rebindDetached();
            HudManager.INSTANCE.reloadForNewProviders();
            ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
        } catch (Throwable t) {
            LOGGER.error("Failed to bind {} to the active profile after Hotwire turned them on", ids, t);
        }
    }

    @Override
    public void prepareStart(List<String> ids) {
        ConfigManager.reopenInitialization();
    }

    @Override
    public void finishStart(List<String> ids, boolean ok) {
        try {
            if (ok) {
                refreshMods();
                ConfigEntrypoints.registerStarted(ids);
            } else {
                ConfigManager.pendingConfigs().forEach(ConfigManager::cancelInitialization);
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to set up the configs of {}, which Hotwire started", ids, t);
        } finally {
            // also what closes the window prepareStart opened
            try {
                ConfigManager.initialize();
            } finally {
                if (ok) ConfigEntrypoints.finishRegistration();
            }
        }
    }

    @Override
    public void finishUpdate(List<String> ids) {
        try {
            refreshMods();
        } catch (Throwable t) {
            LOGGER.error("Failed to read the mods again after Hotwire updated {}", ids, t);
        }
    }

    private static void refreshMods() {
        Map<String, String> copies = new HashMap<>();
        for (ModState mod : HotwireApi.getInstance().getMods()) {
            if (mod.copyOf() != null) copies.put(mod.id(), mod.copyOf());
        }
        CompatibilityPlatformImpl.liveCopies = copies;
        CompatLoader.INSTANCE.refreshMods();
    }
}
