package org.polyfrost.oneconfig.api.toggle.v1;

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
}
