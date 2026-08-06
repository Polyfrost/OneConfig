1.1.4 changelogs:
- Added an item list config option for picking and reordering items.
- HUDs now show up as mod cards, grouped by type with collapsible sections.
- Merged HUDs now keep rows flush when a HUD resizes.
- Fixed HUDs not being isolated per profile.
- Fixed mixins related crashes.

Public API:

- Added the `@ItemList` config annotation and `Visualizer.ItemListVisualizer`
- Added `ModCardType`, `ModCardTypes`, and `ModCardTypeResolver` for registering and resolving mod card categories
- Added `HudManager.pendingAdd` and `HudManager.getRevision`
- Added `TextHud.DateTime` constructor overloads taking an explicit id