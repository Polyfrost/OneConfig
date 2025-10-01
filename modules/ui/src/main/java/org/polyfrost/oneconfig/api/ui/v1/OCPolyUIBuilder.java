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

package org.polyfrost.oneconfig.api.ui.v1;

import dev.deftu.omnicore.api.client.screen.OmniScreen;
import dev.deftu.omnicore.api.loader.OmniLoader;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.Settings;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.utils.PolyUIBuilder;

import java.util.function.Consumer;

public final class OCPolyUIBuilder extends PolyUIBuilder {
    private float designedWidth, designedHeight;
    private Consumer<PolyUI> onClose;
    private boolean pauses, blurs;
    private boolean allowsDebug = true;

    private OCPolyUIBuilder() {
        Settings s = getSettings();
        s.enableInitCleanup(false);
    }

    public OCPolyUIBuilder onClose(Consumer<PolyUI> onClose) {
        this.onClose = onClose;
        return this;
    }

    public OCPolyUIBuilder onClose(Runnable onClose) {
        this.onClose = (p) -> onClose.run();
        return this;
    }

    public OCPolyUIBuilder atResolution(float designedWidth, float designedHeight) {
        this.designedWidth = designedWidth;
        this.designedHeight = designedHeight;
        return this;
    }

    public OCPolyUIBuilder pauses() {
        return pauses(true);
    }

    public OCPolyUIBuilder pauses(boolean state) {
        this.pauses = state;
        return this;
    }

    public OCPolyUIBuilder blurs() {
        return blurs(true);
    }

    public OCPolyUIBuilder blurs(boolean state) {
        this.blurs = state;
        return this;
    }

    public OCPolyUIBuilder allowsDebug() {
        return allowsDebug(true);
    }

    public OCPolyUIBuilder allowsDebug(boolean state) {
        this.allowsDebug = state;
        return this;
    }

    public PolyUI make(Drawable... drawables) {
        if (getRenderer() == null) renderer(UIManager.INSTANCE.getRenderer());
        if (allowsDebug) getSettings().enableDebugMode(OmniLoader.isDevelopment());
        return super.make(drawables);
    }

    /**
     * create, and open a new PolyUI screen, backed by the returned instance. Additionally, a window is also created and assigned to the instance.
     */
    public PolyUI makeAndOpen(Drawable... drawables) {
        PolyUI p = make(drawables);
        Object screen = UIManager.INSTANCE.createPolyUIScreen(p, designedWidth, designedHeight, pauses, blurs, onClose);
        p.setWindow(UIManager.INSTANCE.createWindow());
        Platform.screen().display(screen);
        return p;
    }

    /**
     * create, and open a new PolyUI screen, backed by the returned instance. Additionally, a window is also created and assigned to the instance.
     * <br>
     * Also returns a reference to the window object. use {@link Platform#screen()} and {@link ScreenPlatform#display(Object)} to display it.
     */
    public kotlin.Pair<PolyUI, OmniScreen> makeAndOpenWithRef(Drawable... drawables) {
        PolyUI p = make(drawables);
        OmniScreen screen = UIManager.INSTANCE.createPolyUIScreen(p, designedWidth, designedHeight, pauses, blurs, onClose);
        p.setWindow(UIManager.INSTANCE.createWindow());
        Platform.screen().display(screen);
        return new kotlin.Pair<>(p, screen);
    }

    public static OCPolyUIBuilder create() {
        return new OCPolyUIBuilder();
    }
}
