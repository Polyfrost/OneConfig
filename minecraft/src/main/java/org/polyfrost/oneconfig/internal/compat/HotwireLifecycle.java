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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.platform.v1.internal.CompatibilityPlatformImpl;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;
import org.polyfrost.oneconfig.internal.ui.compose.impls.HudEditorUIScreen;

/**
 * OneConfig's part in Hotwire turning mods off and on, adding and updating them while the game runs. Hotwire takes a
 * mod's objects out of OneConfig's collections and puts the same ones back itself, so what is left is what needs to
 * know about profiles, about when new configs are initialized, and what is worked out from the mods that are there.
 * Every call comes on the game thread, and none while OneConfig itself is off.
 * <br>
 * This is the only class that names Hotwire's types, so it must only be loaded when Hotwire is installed.
 */
public final class HotwireLifecycle implements ModLifecycleListener {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Hotwire");

    private HotwireLifecycle() {
    }

    public static void register() {
        HotwireApi api = HotwireApi.getInstance();
        // older builds cannot tell the classes of a copy from the ones in its mod's jar
        if (api.hasRevision(2)) {
            HudManager.INSTANCE.setSavedName(api::findOwnName);
            HudManager.INSTANCE.setRunningClass(name -> api.findRunningClass(name).orElse(null));
        }
        api.addLifecycleListener(new HotwireLifecycle());
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

    // Hotwire leaves the HUDs and mod cards alone after an off once OneConfig's listener has this method
    @Override
    public void finishOff(List<String> ids) {
        try {
            sync(false);
        } catch (Throwable t) {
            LOGGER.error("Failed to take out what was left of {} after Hotwire turned them off", ids, t);
        }
    }

    @Override
    public void finishOn(List<String> ids) {
        try {
            sync(true);
        } catch (Throwable t) {
            LOGGER.error("Failed to bind {} to the active profile after Hotwire turned them on", ids, t);
        }
    }

    // the mods are in the loader's list by now, which compat goes by to tell whose config an initializer builds
    @Override
    public void prepareStart(List<String> ids) {
        try {
            refreshMods();
        } catch (Throwable t) {
            LOGGER.error("Failed to read the mods again before Hotwire starts {}", ids, t);
        }
        ConfigManager.reopenInitialization();
    }

    @Override
    public void finishStart(List<String> ids, boolean ok) {
        try {
            if (ok) {
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
                if (ok) ConfigEntrypoints.finishStarted();
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

    // by what is out of place rather than by which mods changed, as their objects only are if the profile changed, and
    // Hotwire may have switched other mods while OneConfig itself was off
    private static void sync(boolean rebind) {
        Set<String> off = refreshMods();
        if (rebind) ConfigManager.rebindDetached();
        if (HudManager.INSTANCE.syncWithMods(off::contains)) HudEditorUIScreen.forgetScene();
        ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
    }

    // returns the mods that are off
    private static Set<String> refreshMods() {
        Map<String, String> copies = new HashMap<>();
        Set<String> off = new HashSet<>();
        for (ModState mod : HotwireApi.getInstance().getMods()) {
            if (mod.copyOf() != null) copies.put(mod.id(), mod.copyOf());
            if (!mod.on()) off.add(mod.id());
        }
        CompatibilityPlatformImpl.liveCopies = copies;
        CompatLoader.INSTANCE.refreshMods();
        return off;
    }
}
