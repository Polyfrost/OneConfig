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

import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.Settings;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.utils.PolyUIBuilder;

import java.util.function.Consumer;

public final class OCPolyUIBuilder extends PolyUIBuilder {
    private float desiredScreenWidth, desiredScreenHeight;
    private Consumer<PolyUI> onClose;
    private boolean pauses, blurs;

    private OCPolyUIBuilder() {
        Settings s = getSettings();
        s.enableInitCleanup(false);
        s.enableForceSettingInitialSize(true);
        s.enableDebugMode(Platform.loader().isDevelopmentEnvironment());
    }

    public OCPolyUIBuilder pauses() {
        pauses = true;
        return this;
    }

    public OCPolyUIBuilder blurs() {
        blurs = true;
        return this;
    }

    public OCPolyUIBuilder onClose(Consumer<PolyUI> onClose) {
        this.onClose = onClose;
        return this;
    }

    public OCPolyUIBuilder onClose(Runnable onClose) {
        this.onClose = (p) -> onClose.run();
        return this;
    }

    public OCPolyUIBuilder atResolution(float desiredScreenWidth, float desiredScreenHeight) {
        this.desiredScreenWidth = desiredScreenWidth;
        this.desiredScreenHeight = desiredScreenHeight;
        return this;
    }

    public PolyUI make(Drawable... drawables) {
        if (getRenderer() == null) setRenderer(UIManager.INSTANCE.getRenderer());
        return super.make(drawables);
    }

    /**
     * create, and open a new PolyUI screen, backed by the returned instance. Additionally, a window is also created and assigned to the instance.
     */
    public PolyUI makeAndOpen(Drawable... drawables) {
        PolyUI p = make(drawables);
        Object screen = UIManager.INSTANCE.createPolyUIScreen(p, desiredScreenWidth, desiredScreenHeight, pauses, blurs, onClose);
        p.setWindow(UIManager.INSTANCE.createWindow());
        Platform.screen().display(screen);
        return p;
    }

    public static OCPolyUIBuilder create() {
        return new OCPolyUIBuilder();
    }
}
