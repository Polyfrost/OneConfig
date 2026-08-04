1.1.1 changelogs:
- HUDs can now be anchored to points on other HUDs, so they move together
- Neighbouring HUDs can now merge their backgrounds into one, optionally diagonally
- Added a favorites category to the mods list
- Padding options now have icons showing which side they affect
- The OneConfig screen can now be closed with the keybind that opens it
- Fixed the HUD bar being drawn underneath HUD contents
- Fixed OneConfig keybinds also firing their vanilla Minecraft action
- Fixed the keybind editor not closing when close animations are turned off
- Fixed the HUD library scrollbar changing size as you scroll
- Fixed chat messages being dropped when they could not be converted for listeners
- The menu now closes promptly and gives the cursor back sooner

Public API:

- Added HUD anchoring on `Hud`: `anchorTo`, `clearAnchor`, `isAnchored`, `anchorChainContains`, `anchorPointX`/`anchorPointY`, and the `anchorParent`, `anchorTargetId`, `anchorPoint`, `anchorOffsetX`/`anchorOffsetY` accessors
- Added `Hud.mergeBackground` and `Hud.mergeDiagonally` for merging backgrounds with neighbouring HUDs
- Added `HudManager.instanceById` for looking up a HUD instance by its id
- Added `TextHud.decorate` for applying prefix, suffix and brackets to a line
