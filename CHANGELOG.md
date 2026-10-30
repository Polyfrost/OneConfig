## 1.6.0
- Fixed uku's Armor HUD compatibility not working on 1.21.11
- Removed Skia/Skiko in favor of our own Compose-based rendering logic
  - Should significantly improve performance in the UI and HUDs
- wWaypoints options that a server has switched off are now shown locked

### For mod developers
- This release introduces ABI-breaking changes. Please review any uses of Compose or Skia/Skiko APIs in your mods and adjust them accordingly.
