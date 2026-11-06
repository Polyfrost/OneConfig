## 1.6.2
- Fix emoji rendering
- Fix rare Vulkan crash
- Add an API for detecting when HUDs are moved externally
- Fix FPS regression with 3DSkinLayers
- Fix FPS regression with HUDs
- Dropped compatibility with Hypixel SkyBlock mods (Apec, Firmament, Odin, Skyblocker, SkyHanni and other MoulConfig-based mods) on Minecraft versions below 26.1, which can no longer be used to play SkyBlock
- Added a Hotwire page, available with Hotwire 0.7.3 or newer, for turning mods on and off, updating them and adding new ones from Modrinth or a jar while the game runs
- Configs, keybinds and mod cards now follow the mods Hotwire adds, updates or turns off while the game runs

### For mod developers
- Added `ModInfo.parentId`, `ModInfo.seenMods` and `ModInfo.refreshLoadedMods()`, along with a `ModInfo` constructor that takes the parent mod's ID
- `ConfigEntrypoint`s now also run for mods added or updated while the game runs
