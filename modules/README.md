# OneConfig Modules

This folder contains the many modules that OneConfig offers to developers.
Each is designed to be an independent package, though some are dependent on other packages for their usage. 
They each are under the namespace `org.polyfrost.oneconfig.api.*`.

Here is a list of the modules, and a brief description of what they do (For more information on how to use each module, see the README in the module's folder):
- [commands](commands/README.md) - The tree based command system used in OneConfig.
- [config](config/README.md) - The tree based configuration system used in OneConfig.
- [config-impl](config-impl/README.md) - The default implementation of the configuration system used in OneConfig.
- [events](events/README.md) - The event system used in OneConfig.
- [hud](hud/README.md) - The HUD system used in OneConfig.
- [ui](ui/README.md) - The UI system used in OneConfig.
- [utils](utils/README.md) - Various utilities used in OneConfig.

Each module has the design philosophy of being decoupled from the internal structure, so that the developer can use the modules with separate front-facing APIs
depending on preference. For example, the command system has three different ways to create commands, and the config system has two different ways to collect configuration data. 
This also helps to reduce interdependence and reduce the complexity of the codebase.

> For Minecraft dependant code, see the `versions` directory. Some utilities and classes are found in this directory, under the same package name as the module.