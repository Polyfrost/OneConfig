# OneConfig HUD

`dependsOn("events", "utils", "ui", "config", "config-impl")`

This module contains the HUD system used in OneConfig. It is the most complex module in OneConfig,
and again using `PolyUI` for its rendering.

The system is designed around a series of `Hud` elements, which are wrapped PolyUI components that can
be rotated, resized, recolored and much more by the user - **without the developer having to add any code about it themselves** -
they just have to add the code for the actual object they wish to display, making it very easy to add new
HUD modules.

The HUD system is an extension of the config system, and can also have arbitrary data stored in the HUD
which can be displayed to the end user in the same way as the standard config system. This is done so
the developer can have configuration for their HUD element separate from the rest of their mod, or just
be a HUD providing mod.

The HUD system is uses its own UI which is entirely separate from the standard OneConfig window, and so
the HUD system can be packaged individually.

For legacy compatability, a `LegacyHud` system is provided, which allows for Minecraft rendering methods
to be used still, but this is not recommended as many of the customization features such as changing of colors
and fonts understandably cannot be done outside of the PolyUI system.