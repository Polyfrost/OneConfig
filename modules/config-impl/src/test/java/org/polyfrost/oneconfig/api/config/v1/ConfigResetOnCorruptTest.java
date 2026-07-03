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

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full-stack test for issue #706: a config with a corrupt on-disk value is loaded through the real
 * {@link Config#initialize} path. The offending option must reset to its code default, every other
 * option must still load from disk, the config must not crash, and the bad file must be backed up.
 */
public class ConfigResetOnCorruptTest {

    private static final String ID = "reset_test_706.json";

    @SuppressWarnings("unused")
    public static class ResetConfig extends Config {
        @Switch(title = "Broken")
        public boolean broken = true;        // corrupt on disk -> must fall back to this default
        @Switch(title = "LoadedTrue")
        public boolean loadedTrue = false;   // valid `true` on disk -> must load
        @Switch(title = "LoadedFalse")
        public boolean loadedFalse = true;   // valid `false` on disk -> must load

        public ResetConfig() {
            super(ID, "Reset 706", Category.QOL);
        }

        @Override
        public void initialize(boolean byConfigManager) {
            super.initialize(byConfigManager);
        }
    }

    @Test
    void corruptOptionResetsButConfigStillLoads() throws Exception {
        Path dir = ConfigManager.active().getFolder();
        Files.createDirectories(dir);
        Path file = dir.resolve(ID);
        Path backup = dir.resolve(ID + ".corrupted");
        Files.deleteIfExists(file);
        Files.deleteIfExists(backup);

        // "broken" is stored as a string, which cannot be applied to the boolean option (the #706 crash).
        Files.write(file,
                "{ \"broken\": \"not_a_bool\", \"loadedTrue\": true, \"loadedFalse\": false }"
                        .getBytes(StandardCharsets.UTF_8));

        ResetConfig config = new ResetConfig();
        assertDoesNotThrow(() -> config.initialize(true));

        // the corrupt option fell back to its code default...
        assertTrue(config.broken, "corrupt option must reset to its default");
        // ...and the valid options still loaded from disk.
        assertTrue(config.loadedTrue, "valid option must load its stored value");
        assertFalse(config.loadedFalse, "valid option must load its stored value");

        // the problematic file was backed up...
        assertTrue(Files.exists(backup), "a backup of the problematic file must be created");
        // ...and the live file was re-saved without the bad value.
        String rewritten = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertFalse(rewritten.contains("not_a_bool"), "the bad value must be scrubbed from the live file");
    }
}
