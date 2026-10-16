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

package org.polyfrost.oneconfig.api.config.v1;

/**
 * Lets a mod provide its own OneConfig layout, which replaces the one OneConfig would otherwise extract
 * from the mod's original config screen (YACL, Cloth Config, MidnightLib, ...). The original screen stays
 * reachable from the "open original" button.
 * <br>
 * A mod provides one by naming an implementation under the {@value #ENTRYPOINT} entrypoint of its
 * {@code fabric.mod.json}. The class is only loaded when OneConfig is installed, so this can be a
 * {@code compileOnly} dependency
 */
@FunctionalInterface
public interface ConfigEntrypoint {
    String ENTRYPOINT = "oneconfig:config";

    /**
     * Called once after every mod has initialized. Create your {@link Config} instances here and pass them
     * to {@link Context#register(Config)}.
     */
    void registerConfigs(Context context);

    /**
     * Information about, and actions for, the mod whose entrypoint is being called
     */
    interface Context {
        /**
         * The id of the mod that provides this entrypoint
         */
        String modId();

        /**
         * Marks the config as belonging to this mod, so it gets the "open original" button
         *
         * @return the config, for chaining
         */
        <T extends Config> T register(T config);

        /**
         * Replaces how the "open original" button opens this mod's original config screen.
         * By default, it opens the screen the mod gives Mod Menu.
         */
        void originalScreen(Runnable opener);
    }
}
