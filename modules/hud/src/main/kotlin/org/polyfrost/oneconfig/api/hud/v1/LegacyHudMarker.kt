package org.polyfrost.oneconfig.api.hud.v1

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
public interface LegacyHudMarker {
    public val supportsScale: Boolean get() = true
}