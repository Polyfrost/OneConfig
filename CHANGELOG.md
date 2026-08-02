1.0.14 changelogs:

- HUD design studio now supports growth anchors, HUD locking and hiding with F1
- HUD editor state is now restored when reopening it
- Mod cards can be favorited, sorted alphabetically and reordered by dragging
- Added new option types: two-thumb range slider, inheritable slider, and ordered number chain
- Keybinds can now be kept out of the Minecraft controls screen
- Option text is now automatically translated where translation keys are used
- Added compatibility layers for Kaleido and wWaypoints
- Mod version is now shown in the mod info tooltip, and the OneConfig version in the credits menu
- Fixed the cursor not being hidden on Wayland
- Fixed various UI issues with contrast, scrollbars, GUI scaling, disabled options and page restoration
- Fixed HUDs going missing from the HUD library, and crashes when legacy HUDs change while rendering
- Fixed MoulConfig compatibility on mods that relocate it

Public API:

- Added `OneConfigUI.open`/`createScreen` overloads that take a route to open the UI on a specific page
- Added `HudAnchor` and growth anchor accessors on `Hud`, plus `Hud.canDelete`
- Added `Property.Display` with display listeners, and `Property.isImmutable`
- `PolyColor` constants now hand out a separate instance each time to avoid mutating a shared instance
- Added `PolyColor.withChroma` for deriving chroma variants
- Added `isGuiHidden` and `guiWidth`/`guiHeight` to the platform screen API