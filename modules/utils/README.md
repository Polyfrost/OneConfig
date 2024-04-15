# OneConfig Utilities

This module contains a variety of utilities used in OneConfig.
This is where the `Platform` classes are located, providing access to
Minecraft methods which are abstracted from any loader or version.

Here is a list of the utilities provided by this module:

- `ColorUtils` - utilities for working with colors, stored as 32-bit integers
- `Deprecator` - a utility for deprecating methods, and reporting what is using them
- `IOUtils` & `NetworkUtils` - utilities for working with files and streams, and getting them from the internet
- `JsonUtils` - utilities for working with `Gson` and `JsonElement`
- `LogScanner` - utilities for scanning stack traces to find the source of an error
- `Multithreading` - utilities for working with threads and tasks
- `OneImage` - utilities for working with `BufferedImage`s
- `SimpleProfiler` - simple push/pop based profiler
- `MHUtils(.kt)` - utilities for working with `MethodHandles`, allowing reflection-like access in modern Java versions
- `Platform` - utilities for working with Minecraft, abstracted from any loader or version
- `LoaderPlatform` - utilities for working and displaying metadata about loaded mods

All the other modules are dependent on this module for the utilities it provides. Note that some utilities,
which are more dependent on Minecraft are in the `versions/src` directory.