## Unreleased changes
- Added integration with the Hotwire mod (0.7.3 and above) for turning mods on and off, updating them and adding new ones without restarting the game

### For mod developers
- Added `ModInfo.parentId`, `ModInfo.seenMods` and `ModInfo.refreshLoadedMods()`, along with a `ModInfo` constructor that takes the parent mod's ID
- `ConfigEntrypoint`s now also run for mods added or updated via Hotwire while the game is running
