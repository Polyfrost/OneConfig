1.1.11 changelogs:
- Prepare SDL support for keybind and window handling.
- Keybinds no longer fire while you are typing in a text field.
- Uncapped the main menu frame rate to your refresh rate plus 60.
- Updated Skyblocker HUD compatibility for Skyblocker 6.10.0, which rewrote both the fancy status bars and the tab HUD widgets.
- Hidden RConfig configs, categories, and entries are now hidden in OneConfig as well.
- Improved HUD performance by only invalidating the redraw cache on HUD-relevant state changes.
- Raster icons are now decoded off the render thread.
- Dropdowns now open on whichever side has more space.
- OneConfig now reopens smoothly while it is still playing its close animation.
- Pages without an animation now open immediately.
- Clicking the page you are already on no longer renavigates.
- Fixed a ConcurrentModificationException when rendering HUDs.
- Fixed title screen panorama rendering on versions below 1.21.8.
- Fixed OneConfig not opening from the title screen in development environments.
- Fixed the TickEvent injection target.
- Fixed the world unload event.
- Fixed pixel grid scaling on smaller resolutions.
- Fixed notification toast rendering.
- Fixed chat receive event handling.
- Fixed sidebar behavior.
- Removed the global HUD toggle.

Public API:

- Added `TextInputFocus`, `Modifier.trackTextInputFocus`, and `KeybindHelper.firesWhileTyping` for controlling whether keybinds fire while typing.
- Added `OneConfigKeybindAdapter` and the `KeybindCodec` interface.
- Added `PRESSED`, `RELEASED`, and `REPEAT` constants to `KeyInputEvent`, and `PRESSED` and `RELEASED` to `MouseInputEvent`.
- Added key code, mouse button, and name lookups to `Keys`.
- Added `PolyComposeHost.frameWithReport`.
- Removed `HudManager.masterHudEnabled`.
