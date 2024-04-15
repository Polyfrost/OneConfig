# OneConfig UI

`dependsOn("events", "utils", "polyui", "nanovg", "tinyfd")`

This module contains the UI system used in OneConfig. It provides the methods which
bridge the gap between PolyUI and Minecraft, as well as a simple Notifications system.

By default, it uses a nanovg-based rendering implementation, which is loaded by `LwjglManager`.
The implementation details are version specific and so are in `versions/src`.

For more information on how the UI works and how to use it, see the `PolyUI` project - our UI framework
utilized for OneConfig's frontend.

Outside of this, it also provides the ability
to blur backgrounds in menus, and exposes the `TinyFD` library for native file dialogs.