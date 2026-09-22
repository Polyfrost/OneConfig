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

package org.polyfrost.oneconfig.api.config.v1

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Files
import java.util.Comparator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class KtConfigProfileRebindTest {
    private class RebindConfig : KtConfig("kt_profile_rebind.json", "Kt Rebind", Category.OTHER) {
        var enabled: Boolean by switch(false, "Enabled")

        val treeTitle: Any? get() = tree?.title
    }

    @BeforeEach
    fun setUp() {
        ConfigManager.active()
        ConfigManager.openProfile("")
        cleanup()
    }

    @AfterEach
    fun tearDown() {
        ConfigManager.openProfile("")
        cleanup()
    }

    @Test
    fun `re-titling a tree with the same title is a no-op`() {
        val tree = Tree.tree("retitle_test")
        tree.title = "Same"
        assertDoesNotThrow { tree.title = "Same" }
        assertEquals("Same", tree.title)
    }

    @Test
    fun `switching profiles repeatedly rebinds a kotlin config`() {
        val config = RebindConfig()
        config.initialize(false)
        config.enabled = true
        config.save()

        ConfigManager.createProfile(PROFILE)
        assertFalse(config.enabled, "new profile should start from Kotlin config defaults")
        config.enabled = false
        config.save()

        repeat(2) {
            ConfigManager.openProfile("")
            assertTrue(config.enabled, "root profile value lost after rebind")
            assertEquals("Kt Rebind", config.treeTitle, "tree lost its title on rebind")

            ConfigManager.openProfile(PROFILE)
            assertFalse(config.enabled, "profile value lost after rebind")
            assertEquals("Kt Rebind", config.treeTitle, "tree lost its title on rebind")
        }
    }

    private fun cleanup() {
        val path = ConfigManager.PROFILES_DIR.resolve(PROFILE)
        if (Files.exists(path)) {
            Files.walk(path).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
        Files.deleteIfExists(java.nio.file.Paths.get("config").resolve("kt_profile_rebind.json"))
    }

    private companion object {
        const val PROFILE = "oc_test_kt_rebind"
    }
}
