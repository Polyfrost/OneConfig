package org.polyfrost.oneconfig.api.hud.v1

/**
 * What the design studio's corner handles are allowed to change on a HUD
 */
enum class HudResize {
    /** Both axes so the handles scale or freely size the HUD */
    Both,

    /** Width only for HUDs with a fixed height so a handle drag leaves the height alone */
    Width,

    /** The HUD is fixed and gets no handles */
    None,
}
