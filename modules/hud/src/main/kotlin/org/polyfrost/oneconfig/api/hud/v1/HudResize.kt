package org.polyfrost.oneconfig.api.hud.v1

/**
 * What the design studio's corner handles are allowed to change on a HUD.
 */
enum class HudResize {
    /** Both axes, the normal case: the handles scale the HUD, or size it freely. */
    Both,

    /**
     * Width only. For HUDs whose height is not theirs to give - Skyblocker's status bars are a fixed height
     * and only expose a width - so a handle drag sizes them horizontally and leaves the height alone.
     */
    Width,

    /** Nothing: the HUD is fixed, and gets no handles at all. */
    None,
}
