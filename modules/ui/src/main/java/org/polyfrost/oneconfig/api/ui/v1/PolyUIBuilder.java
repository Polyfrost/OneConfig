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
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.color.DarkTheme;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.event.InputManager;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.property.Settings;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.unit.Align;
import org.polyfrost.polyui.unit.Vec2;

import java.util.function.Consumer;

public final class PolyUIBuilder {
    private InputManager manager;
    private Renderer renderer;
    private Translator translator;
    private Align alignment = new Align(Align.Main.Center, Align.Cross.Start, Align.Mode.Horizontal, Vec2.ZERO, 50);
    private Colors colors = new DarkTheme();
    private PolyColor backgroundColor;
    private Vec2 desiredResolution;
    private Consumer<PolyUI> onClose;
    private Settings settings;
    private Vec2 size;
    private boolean pauses, blurs;

    private PolyUIBuilder() {
        settings = new Settings();
        settings.enableInitCleanup(false);
        settings.enableForceSettingInitialSize(true);
    }

    public PolyUIBuilder input(InputManager manager) {
        this.manager = manager;
        return this;
    }

    public PolyUIBuilder translator(Translator translator) {
        this.translator = translator;
        return this;
    }

	public PolyUIBuilder translatorDelegate(String translationDir) {
		Translator translator = this.translator == null ? this.translator = new Translator(settings, "", null) : this.translator;
		translator.addDelegate(translationDir);
		return this;
	}

    public PolyUIBuilder align(Align alignment) {
        this.alignment = alignment;
        return this;
    }

    public PolyUIBuilder pauses() {
        pauses = true;
        return this;
    }

    public PolyUIBuilder blurs() {
        blurs = true;
        return this;
    }

    public PolyUIBuilder colors(Colors colors) {
        this.colors = colors;
        return this;
    }

    public PolyUIBuilder backgroundColor(PolyColor color) {
        this.backgroundColor = color;
        return this;
    }

    public PolyUIBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public PolyUIBuilder renderer(Renderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public PolyUIBuilder size(Vec2 size) {
        this.size = size;
        return this;
    }

    public PolyUIBuilder onClose(Consumer<PolyUI> onClose) {
        this.onClose = onClose;
        return this;
    }

    public PolyUIBuilder onClose(Runnable onClose) {
        this.onClose = (p) -> onClose.run();
        return this;
    }

    public PolyUIBuilder atResolution(Vec2 desiredResolution) {
        this.desiredResolution = desiredResolution;
        return this;
    }

    public PolyUI make(Drawable... drawables) {
        return new PolyUI(drawables, renderer == null ? UIManager.INSTANCE.getRenderer() : renderer, settings, manager, translator, backgroundColor, alignment, colors, size);
    }

    /**
     * create, and open a new PolyUI screen, backed by the returned instance. Additionally, a window is also created and assigned to the instance.
     */
    public PolyUI makeAndOpen(Drawable... drawables) {
        PolyUI p = make(drawables);
        Object screen = UIManager.INSTANCE.createPolyUIScreen(p, desiredResolution, pauses, blurs, onClose);
        p.setWindow(UIManager.INSTANCE.createWindow());
        Platform.screen().display(screen);
        return p;
    }

    public static PolyUIBuilder builder() {
        return new PolyUIBuilder();
    }
}
