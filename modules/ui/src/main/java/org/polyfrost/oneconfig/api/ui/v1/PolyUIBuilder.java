package org.polyfrost.oneconfig.api.ui.v1;

import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.screen.PolyUIScreenProvider;
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

import java.util.ServiceLoader;
import java.util.function.Consumer;

public final class PolyUIBuilder {
    public static final PolyUIScreenProvider DEFAULT = ServiceLoader.load(PolyUIScreenProvider.class, PolyUIScreenProvider.class.getClassLoader()).iterator().next();
    private final PolyUIScreenProvider provider;
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

    private PolyUIBuilder(PolyUIScreenProvider provider) {
        this.provider = provider;
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

    public PolyUI makeAndOpen(Drawable... drawables) {
        PolyUI p = make(drawables);
        Object screen = provider.create(p, desiredResolution, pauses, blurs, onClose);
        Platform.screen().display(screen);
        return p;
    }

    public static PolyUIBuilder builder() {
        return new PolyUIBuilder(DEFAULT);
    }

    public static PolyUIBuilder builder(PolyUIScreenProvider provider) {
        return new PolyUIBuilder(provider);
    }
}
