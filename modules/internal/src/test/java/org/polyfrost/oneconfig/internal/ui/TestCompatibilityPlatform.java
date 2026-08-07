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

package org.polyfrost.oneconfig.internal.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.polyfrost.oneconfig.api.platform.v1.CompatibilityPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Options;

import java.util.Set;

public final class TestCompatibilityPlatform implements CompatibilityPlatform {
    @Override
    public void displayChatMessage(Component text) {
    }

    @Override
    public Set<ModInfo> getMods() {
        return Set.of();
    }

    @Override
    public boolean isDevelopment() {
        return true;
    }

    @Override
    public String version() {
        return "test";
    }

    @Override
    public String loader() {
        return "test";
    }

    @Override
    public Options options() {
        throw new UnsupportedOperationException("no game options in tests");
    }

    @Override
    public Keys keys() {
        throw new UnsupportedOperationException("no game keybinds in tests");
    }

    @Override
    public long windowHandle() {
        return 0L;
    }

    @Override
    public int fps() {
        return 0;
    }

    @Override
    public String resolveComponent(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Override
    public Object wrapPlatformComponent(Object component) {
        return component;
    }
}
