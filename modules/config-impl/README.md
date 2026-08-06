# OneConfig Config Implementation

`dependsOn("utils", "config", "polyui")`

This module contains the default implementation of the configuration system used in OneConfig.

> See the `config` module for more information on how the configuration system works internally.

It utilizes files for storage, and uses `PolyUI` to show the trees to the end user.
It supports profiles and even potentially zip-based storage.
It supports reading and writing to `.json`, `.yaml` and `.toml` files, thanks to `NightConfig`.

The collection process both supports a java builder-style pattern and an annotation based config collector.

The frontend system requires usage of `Visualizer`s, attached as metadata, which are simple functional interfaces which
take properties and turn them into PolyUI drawable elements, and then this is handed to the UI and displayed to the user,
with callbacks so the tree is updated accordingly.

todo add some pictures here

## Item lists

`@ItemList` provides a searchable selector for items registered by Minecraft or another mod. The
backing value stores namespaced registry IDs, which keeps configuration files independent of
localized item names and Minecraft implementation classes.

```java
@ItemList(
    title = "Tracked items",
    description = "Items shown by the tracker",
    maxEntries = 8
)
public List<String> trackedItems = new ArrayList<>(List.of(
    "minecraft:diamond",
    "minecraft:golden_apple"
));
```

Both `String[]` and `List<String>` fields are supported. Set `maxEntries = 1` to use the control as
a single-item selector. Unknown IDs remain in the configuration and can still be removed, so a
temporarily missing content mod does not silently destroy the user's selection.
