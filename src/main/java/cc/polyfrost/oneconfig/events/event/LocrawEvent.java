package cc.polyfrost.oneconfig.events.event;

import cc.polyfrost.oneconfig.utils.hypixel.LocrawInfo;

/**
 * Called when the player's location in Hypixel is received via the /locraw command.
 *
 * @see LocrawInfo
 * @see cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils
 */
public class LocrawEvent {
    public final LocrawInfo info;

    public LocrawEvent(LocrawInfo info) {
        this.info = info;
    }
}
