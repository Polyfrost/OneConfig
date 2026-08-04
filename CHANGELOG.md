1.1.2 changelogs:
- Fixed merged HUD backgrounds being drawn twice behind custom HUDs
- Minecraft theme text is now scaled to sit closer to the pixel grid

Public API:

- Added `Hud.canMergeBackground`, which HUDs override to opt in or out of having their background fused with neighbouring HUDs (defaults to `true` for `TextHud` only)
- Added `Hud.hudBackground`, which applies the HUD's background to a modifier unless it is turned off or already drawn as part of a fused shape
- `Hud.bgMerged` is now publicly readable so custom HUDs can skip drawing their own background while it is fused
