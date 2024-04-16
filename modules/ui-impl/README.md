# OneConfig UI

`dependsOn("ui", "events", "utils", "hud", "polyui", "nanovg", "tinyfd")`

This module contains the actual OneConfig UI implementation. It is fully internal and not part of the public API. While you may be able to use it, this is not recommended and is volatile.
> As mentioned above, unlike other API packages, this is not found under `org.polyfrost.oneconfig.api.` but instead `org.polyfrost.oneconfig.internal.ui` due to its internal nature.