1.1.3 changelogs:
- Merged HUDs now move as a single group
- Fixed a rare JVM crash with Vulkan
- Fixed Compose rendering breaking on window resize
- Fixed the extra space between a text HUD's prefix, value, and suffix
- Fixed several crashes around screen handling, mod compatibility, and config loading

Public API:

- Added `Hud.selfAnchorPoint` (with `setSelfAnchorPointKeepingPosition`) and an `anchorTo` overload that takes the anchored HUD's own anchor point
- Added `Hud.effectiveAnchorParent`, `Hud.effectiveAnchorPoint`, `Hud.effectiveSelfAnchorPoint`, and `Hud.isMergeAnchored` for reading resolved anchoring state
- Added `HudManager.mergeGroupOf`, `HudManager.setMergeExclusions`, and `HudManager.drawMergedBackgrounds` for working with merged HUD groups
- Added `NotificationsManager.ensureInitialized`
- Added `ScreenPlatform.showMessage`
