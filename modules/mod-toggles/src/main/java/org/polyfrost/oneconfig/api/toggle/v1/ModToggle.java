package org.polyfrost.oneconfig.api.toggle.v1;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The master switch shown on a mod's card in OneConfig
 * <br>
 * A mod gets one by naming an implementation under the {@value #ENTRYPOINT} entrypoint of its
 * {@code fabric.mod.json}. The class is only loaded when OneConfig is installed, so this can be a
 * {@code compileOnly} dependency
 */
public interface ModToggle {
    String ENTRYPOINT = "oneconfig:toggle";

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Whether the mod has to be set up before it can be switched on
     * <br>
     * Switching such a mod on opens it from its card instead, the same as clicking the card does
     */
    default boolean needsSetup() {
        return false;
    }

    static ModToggle of(BooleanSupplier get, Consumer<Boolean> set) {
        Objects.requireNonNull(get, "get");
        Objects.requireNonNull(set, "set");
        return new ModToggle() {
            @Override
            public boolean isEnabled() {
                return get.getAsBoolean();
            }

            @Override
            public void setEnabled(boolean enabled) {
                set.accept(enabled);
            }
        };
    }

    static ModToggle of(BooleanSupplier get) {
        return of(get, enabled -> {
        });
    }
}
