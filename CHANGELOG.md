1.1.0 changelogs:
- You can now select multiple HUDs at once, either by holding shift or by dragging a box around them
- Added copy, cut, paste, duplicate, select all, lock, reset and delete to the HUD editor, with keybinds you can change
- Added a setting to hide every HUD at once without turning them off individually
- The HUD editor menus now show the keybind for each action, which you can turn off
- Text HUDs can now be wrapped in square brackets
- OneConfig can now reopen the HUD editor instead of the main menu if that's where you left off
- Fixed chat messages not being picked up by mods that listen for them
- Fixed OneConfig menus showing through other mods' screens
- Fixed see-through HUDs on 1.21.11 and below
- The menu is faster to open and smoother to use

Public API:

- Added `HudManager.masterHudEnabled` for globally toggling HUD rendering
- Added `TextHud.brackets`, wrapping the finished line with brackets
- Added `KeybindUtils` with `getActionModifier` and `isActionModifierPressed`