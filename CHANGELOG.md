# Changelog

All notable changes to OneConfig are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- PolyGlass Dark and PolyGlass Light themes, with real frosted glass behind the menus
- Custom themes. Start from any theme, change it, share it with a code and use it on every profile
- Theme tints and palette themes
- A Sound Theme picker on the Themes page, with Match Theme, OneConfig and Minecraft
- Precision editing in the HUD editor: X and Y fields you can drag or type math into, distances shown while holding Alt, live gap labels, snapping to equal spacing and padding, and a grid you toggle with G
- A Reduced Motion setting that drops sliding, scaling and bouncing from OneConfig's menus and keeps fades
- A new Changelog page that lists every release, and a new Credits page
- API: A public UI component kit with text fields, menus, tooltips, dialogs, buttons, tabs, player heads, drag-to-reorder lists, an Esc stack and accessibility helpers
- API: A `swatch` option for `@Color` and the Kotlin config DSL

### Changed
- The old PolyGlass theme is now called Polyfrosted
- The GUI and animation settings moved from Preferences to the Themes page. Your saved values carry over
- The sidebar sections can now be folded, the mod list scrolls when it doesn't fit, and Changelog and Credits moved to the info menu next to the notifications bell
- UI sound volume, ambience volume and HUD drag opacity are now set as percentages
- Modifier-only OneConfig keybinds, such as a lone Shift or Ctrl, now open and close the menu when you let go of the key
- Updated open and close sounds for the OneConfig menu
- Mod Menu now lists OneConfig as a single mod instead of a separate "OneConfig (Platform Module)" entry
- Fixed click events and text hovers being dropped from chat messages that OneConfig converts on 1.8.9 (item and entity hovers are still dropped)

### Removed
- API: Removed DevAuth dependency

### Fixed
- Fixed fullscreen settings breaking when switching between Minecraft versions that use GLFW and SDL
- Fixed a rare crash with OneConfig keybinds in the controls screen
- Fixed parts of the OneConfig menus not redrawing after their cached textures expired
- Clicking a toast or dragging a HUD editor panel no longer clicks what is underneath
- Fixed the game crashing on launchers that inject fake mods
- API: Fixed Loom logging a large stack trace while remapping OneConfig's sources jar in dependent mods

## [1.6.2] - 2026-11-06

### Added
- Add an API for detecting when HUDs are moved externally

### Removed
- Dropped compatibility with Hypixel SkyBlock mods (Apec, Firmament, Odin, Skyblocker, SkyHanni and other MoulConfig-based mods) on Minecraft versions below 26.1, which can no longer be used to play SkyBlock

### Fixed
- Fix emoji rendering
- Fix rare Vulkan crash
- Fix FPS regression with 3DSkinLayers
- Fix FPS regression with HUDs

## [1.6.1] - 2026-11-01

### Changed
- Number spinners in the HUD editor now keep their unit next to the value
- HUDs that keep failing to update now only log the error once instead of every frame
- Improved performance of Minecraft-font text with a chroma full shadow on 1.21.11 and above

### Fixed
- Fixed legacy HUDs being drawn behind the OneConfig UI outside a world
- Fixed the HUD editor tour picking legacy HUDs
- Fixed rounded corners rendering incorrectly on elements too small for their corner radius
- Fixed the HUD editor tour's highlight breaking for HUDs at the edge of the screen
- Fixed the OneConfig UI closing with a rendering error when a HUD fails to update outside a world, such as Legacy Skyblock's Pet Display on 26.x
- Fixed OneConfig's UI being corrupted with VulkanMod on 1.21.1
- Fixed a crash when opening the OneConfig menu with VulkanMod on 1.21.11
- Fixed the OneConfig menu's background and animations rendering upside down with VulkanMod on 1.21.11 and 26.1
- Fixed toasts and other GUI elements being partially cut off with VulkanMod on 1.21.11 and 26.1
- API: Fixed builds depending on OneConfig for 1.8.9 failing to resolve LWJGL STB natives

## [1.6.0] - 2026-10-30

### Changed
- Waypoints options that a server has switched off are now shown locked
- API: This release introduces ABI-breaking changes. Please review any uses of Compose or Skia/Skiko APIs in your mods and adjust them accordingly.

### Removed
- Removed Skia/Skiko in favor of our own Compose-based rendering logic
  - Should significantly improve performance in the UI and HUDs

### Fixed
- Fixed uku's Armor HUD compatibility not working on 1.21.11

## [1.5.2] - 2026-10-27

### Fixed
- Fixed getting stuck on title screen panorama with Slipstream and PolyPlus onboarding on 1.21.10 and 1.21.11 on macOS

## [1.5.1] - 2026-10-27

### Fixed
- Fix see through parts of HUDs, such as backgrounds and text edges, looking darker than they should on 1.8.9
- Opening the settings of mods like Bring Blocking Back from the Mods page no longer reopens them over and over until the game crashes

## [1.5.0] - 2026-10-24

### Added
- Add community translations for OneConfig and many other mods
- Sodium is now translated too, using the Translations for Sodium resource pack
- Translators are now credited in the credits screen
- The accent color can now be a gradient. Turn on "Gradient" in the accent color picker and pick a second color
- Add keybinds for switching profiles. Set one on any profile card on the Profiles page. A notification confirms the switch, and you can turn it off in Preferences
- Add a sort control to the Mods page. Keep your own drag order, or sort mods A to Z
- Add a "Full Shadow" option for text HUDs, which outlines the text on every side instead of using a drop shadow
- Add a guided tour of the HUD editor. Replay it any time by right-clicking an empty spot and choosing "Editor Tour"
- Add a Search keybind that jumps to the search bar in the OneConfig menu and HUD editor. In the HUD editor it searches the selected HUD's settings, or the HUD library when nothing is selected
- Add "Select all" and "Deselect all" to dropdowns where you can pick several options
- API: Add an experimental public UI API (`api.ui.v1`) for pages, themes, components, search and keybinds, so mods no longer need OneConfig's internals
- API: Add an API for adding your own sort modes to the Mods page
- API: Add `SliderBounds` so sliders can change their minimum and maximum at runtime
- API: Add opt-in gradient colors with `@Color(gradient = true)`
- API: Add an `@EffectList` option for picking status effects
- API: Add item stack icons for item list options, for item variants such as potions
- API: Add a full width option for options that need the whole row

### Changed
- Move the "Instant search" setting into the Search category
- Better Screens, Render Tweaks and Shake Tweaks now provide their own OneConfig on/off toggle, so OneConfig no longer adds one for them

### Fixed
- Fix the on/off toggle not turning off some mods
- Fix one broken line in your Minecraft options resetting all of your settings
- Fix copy and paste not working on 1.8.9 on Apple Silicon Macs, and copying images on Linux with Wayland
- Fix 1.8.9 settings such as clouds changing after playing a newer version
- Fix the theme and accent color not changing when you switch profiles
- Fix colored text showing without colors in HUDs and in Chat Patches and YACL config screens
- Fix YACL options not updating when another option changes them
- Fix the "HUD is locked" notice appearing on every key press while editing a locked HUD
- Fix a warning being logged for every chat message with transparent colors on newer versions
- Fix notification icons, the HUD editor search bar, item pickers and the mod card on/off indicator not following your theme
- Fix OneConfig's menus failing to show text on Windows when the game is installed in a deeply nested folder
- API: Fix nested annotated commands and handler aliases not being registered

## [1.4.4] - 2026-10-19

### Added
- Add a config entrypoint (`ConfigEntrypoint`) so mods can provide their own OneConfig layout instead of having their original screen extracted
- Add a generic tile picker, used by item list options

### Changed
- Rework header titles and navigation controls
- Rename the "Open original screen" button to "Original menu" and give it an external link icon
- Make popups and dropdowns follow the open/close animation and UI scaling
- Improve config screen extraction and its retry handling for third-party config libraries

### Fixed
- Fix the "All" mod filter chip sometimes being hidden
- Fix mouse buttons staying held after a screen closes or loses focus
- Fix numpad decimal, menu, ISO and JIS keys not being mapped on legacy versions

## [1.4.3] - 2026-10-17

### Added
- Add Range Number option, plus Range Slider and Range Number list options
- Add compatibility for WalksyLib 2.0

### Changed
- Improve HUD rendering performance with better caching
- Tighten up HUD library cards and pack rows, and label them
- Move scrollbars into their own gutters and allow scrolling while hovering them

### Fixed
- Fix duplicating HUDs copying more than the source HUD's values
- Fix accordion header toggles ignoring the option's display state
- Fix certain mods breaking when their Cloth Config save ran outside the Cloth Config screen
- Fix the UI failing to set up its GL context on Java 25+

## [1.4.2] - 2026-10-14

### Added
- Support mod toggles for lambdynamiclights and uku Armor HUD

### Changed
- Allow explicitly added 3rd party master switches to be overriden
- Improve performance with some HUDs

### Fixed
- Fix Simple Voice Chat mod toggle in certain scenarios
- Fix accordion description pushing some of its toggle off the section
- Fix HUD elements set to hide in F3 being hidden even when debug renderers were the only F3 elements currently drawing

## [1.4.1] - 2026-10-12

### Added
- Add the `mod-toggles` API so mods can ship their own toggle through the `oneconfig:toggle` entrypoint
- Add `@MasterToggle` (and `masterToggle()` for KtConfig) to give a OneConfig mod a toggle on its card
- Add proper toggles to hud elements

### Removed
- Remove iconographic from mod toggles

### Fixed
- Fix option display state not updating properly

## [1.4.0] - 2026-10-11

### Added
- Add universal mod toggles

### Fixed
- Fixed config search leaking into HUD Design Studio, causing some options to be unexpectedly hidden
- Fixed the HUD Design Studio's search bar not doing anything

## [1.3.0] - 2026-10-09

### Added
- Added undo and redo to the HUD editor, with rebindable keybinds
- Added a Hidden category to the mods list, with a toggle on each mod card to hide it
- Added value formatters to number and slider options (`ValueFormatter`, available on the annotations, `KtConfig` and the config DSL)
- Added a favorite color to themes, used by the favorite star
- Bundled the Windows arm64 Skiko runtime

### Changed
- Improved mod card dragging: cards now follow the cursor smoothly, and auto scroll speeds up the longer you hold near an edge
- Updated Compose to 1.13.0-alpha02 and Skiko to 0.154.0, now built from upstream
- Marked as incompatible with PolyPlus versions below 1.2.49

### Fixed
- Fixed the wrong mod card being picked up while cards are animating or overlapping
- Fixed mod cards dropping into the wrong slot after a quick release
- Fixed a held mod card not being picked up when scrolling, and opening after it was scrolled
- Fixed the last favorite being unfavorited when dropped
- Fixed mod cards sliding in from off-screen
- Fixed the mods grid scrolling on its own when its cards reorder
- Fixed the missing scrollbar padding in the mods grid
- Fixed options sharing display state with other options that compare equal
- Fixed clicks being treated as drags in the HUD editor
- Fixed HUDs excluded from merging dragging their neighbors along
- Fixed HUD editor outlines not redrawing when the GUI size changes
- Fixed rendering corruption on Vulkan
- Fixed framebuffers being rebound too late when restoring GL state on 1.8.9
- Fixed scroll wheel speed on 1.8.9

## [1.2.22] - 2026-10-06

### Changed
- Improved search results by including aliases

### Fixed
- Fixed Wayland support when using BlazeSDL

## [1.2.21] - 2026-10-05

### Changed
- use Steve as the fallback player head

### Fixed
- fix ambience leaking through oneconfig ui
- fix YACL compat
- stop minecraft font glyphs bleeding into the next atlas cell

## [1.2.20] - 2026-10-04

### Fixed
- Fixed range slider option labels in the HUD editor being squeezed to one character per line

## [1.2.19] - 2026-10-04

### Removed
- Removed player head border

### Fixed
- Fixed add HUD pill not being clickable
- Fixed background blur clipping

## [1.2.18] - 2026-10-03

### Changed
- Split resource packs between 1.8 and modern
- (1.8) use GlStateManager for item atlas projection and viewport restore

## [1.2.17] - 2026-10-03

### Fixed
- Fix settings being lost on 1.8.9 (again)
- Fix hidden options being included in UI

## [1.2.16] - 2026-10-03

### Fixed
- Fix settings being lost

## [1.2.15] - 2026-10-03

### Added
- Added scrollbar and mod icon rounding values to themes, bringing the Minecraft theme closer to its intended look

### Changed
- Mod category chips can now be deselected, and only categories that have mods are shown

### Fixed
- Fixed Poppins text spacing, newlines and alignment in HUDs
- Fixed scrollbar shape not re-applying
- Fixed audio ducking when switching between the PolyGlass and Minecraft themes
- Fixed audio ducking when switching between the HUD editor and the OneConfig screen

## [1.2.14] - 2026-10-03

### Added
- Added scrollbar and mod icon rounding values to themes, bringing the Minecraft theme closer to its intended look

### Changed
- Mod category chips can now be deselected, and only categories that have mods are shown

### Fixed
- Fixed Poppins text spacing, newlines and alignment in HUDs
- Fixed scrollbar shape not re-applying
- Fixed audio ducking when switching between the PolyGlass and Minecraft themes
- Fixed audio ducking when switching between the HUD editor and the OneConfig screen

## [1.2.13] - 2026-10-02

### Fixed
- 1.8.9: Fixed expensive GL state capture reducing performance

## [1.2.12] - 2026-10-02

### Fixed
- run profile switches on correct thread

## [1.2.11] - 2026-10-02

### Fixed
- fix unknown options being wiped in 1.8

## [1.2.10] - 2026-10-02

### Changed
- Improve performance by optimizing x/y get/sets
- Replace native confirmation dialogs with Compose confirmations

### Fixed
- Use SDL dialogs on Minecraft 26.3
- Fire ScreenOpenEvent for all screen changes on 26.2+
- Restore previous framebuffer after render target changes
- Redraw menu HUDs after add or delete

## [1.2.9] - 2026-09-30

### Changed
- replace opening behavior with a remember last page dropdown

### Fixed
- fix missing config settings overwriting
- pause text input while a cached Compose screen is closed
- fix out of main thread config watcher updates

## [1.2.8] - 2026-10-01

### Added
- Added proper compatibility for Meowdding HUDs rather than detecting them all as SkyCubed HUDs
- Add support for a lot more options in config compat layers
- Add parity to Java API for KtConfig / Kt DSL config API

### Changed
- Dropped 1.21.11 support and added 26.2 support for Meowdding HUDs
- The HUD editor now suggests installing EvergreenHUD when no mods providing HUDs are installed
- Improved HUD rendering performance alongside legacy and compat HUDs (such as VanillaHUD), which no longer make every other HUD redraw whenever they change size
- Bump Compose
- Keep and restore search across reopen and navigation
- Run Compose RectManager dispatch on the client thread

### Fixed
- Fixed Skysoft 0.1.30+ settings not appearing in OneConfig
- Fix rebinding keybinds that start unbounded

## [1.2.7] - 2026-09-25

### Fixed
- vulkan fixes

## [1.2.6] - 2026-09-24

### Added
- add `shouldShow` API for HUDs

### Changed
- Measure item count text width with real Skia font renderer

### Fixed
- Fix Wayland issues on 1.8

## [1.2.5] - 2026-09-23

### Fixed
- correct Vulkan offscreen barriers and submission order
- stop grayed out keybinds from not being grayed out in keybinds list

## [1.2.4] - 2026-09-21

### Fixed
- Fixed offscreen HUDs being glitched with Gnetum

## [1.2.3] - 2026-09-21

### Fixed
- fixed gl context not being created on wayland/egl

## [1.2.2] - 2026-09-20

### Changed
- optimize item icon rendering
- HUD preview improvements
- the game is now paused while the HUD editor is open
- moved UI warm-up to startup, removing the first-open stutter

### Fixed
- fixed duplicate padding on accordion options
- fixed SkyCubed HUD placement before overlay bounds are available

## [1.2.1] - 2026-09-18

### Removed
- remove ui scaling config

## [1.2.0] - 2026-09-15

### Added
- Added support for Minecraft 26.3

### Fixed
- Fixed conflicting keybind handling always using Right Shift instead of the actual OneConfig open keybind
- Fixed notifications not being clickable on macOS Retina and possibly other HiDPI setups

## [1.1.17] - 2026-09-10

### Changed
- Improve notification performance

### Fixed
- Fix minor bugs

## [1.1.16] - 2026-09-10

### Changed
- Disable mod card placement animation on window resize

### Fixed
- Fix rounded corners having weird colors
- Prevent opening animation flicker

## [1.1.15] - 2026-09-07

### Added
- Add uku's Armor HUD compat

## [1.1.14] - 2026-09-06

### Changed
- release search focus after warm up

### Removed
- Remove onboarding

## [1.1.13] - 2026-09-05

### Added
- Add support for mouse binds
- Add ukulib config support

### Changed
- Significantly improve OneConfig performance
- Update Compose, Skiko

## [1.1.12] - 2026-09-02

### Fixed
- Fix rare crash with Compose

## [1.1.11] - 2026-09-02

### Added
- Prepare SDL support for keybind and window handling.
- API: Added `TextInputFocus`, `Modifier.trackTextInputFocus`, and `KeybindHelper.firesWhileTyping` for controlling whether keybinds fire while typing.
- API: Added `OneConfigKeybindAdapter` and the `KeybindCodec` interface.
- API: Added `PRESSED`, `RELEASED`, and `REPEAT` constants to `KeyInputEvent`, and `PRESSED` and `RELEASED` to `MouseInputEvent`.
- API: Added key code, mouse button, and name lookups to `Keys`.
- API: Added `PolyComposeHost.frameWithReport`.

### Changed
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

### Removed
- Removed the global HUD toggle.
- API: Removed `HudManager.masterHudEnabled`.

### Fixed
- Fixed a ConcurrentModificationException when rendering HUDs.
- Fixed title screen panorama rendering on versions below 1.21.8.
- Fixed OneConfig not opening from the title screen in development environments.
- Fixed the TickEvent injection target.
- Fixed the world unload event.
- Fixed pixel grid scaling on smaller resolutions.
- Fixed notification toast rendering.
- Fixed chat receive event handling.
- Fixed sidebar behavior.

## [1.1.10] - 2026-08-22

### Fixed
- Fixed more compat huds issues

## [1.1.9] - 2026-08-22

### Fixed
- Fixed compat HUDs breaking after relaunch

## [1.1.8] - 2026-08-21

### Fixed
- Fixed HUDs breaking when the window is resized.

## [1.1.7] - 2026-08-21

### Added
- Added centralized search corpus and search providers.
- Added Stella mod compatibility.
- API: Added back the `renderMode` API.

### Changed
- Improved profile management, including fixes for profile switching, exporting on macOS, and HUD profile switching.
- Rewrote the custom GUI scale option and improved pixel grid snapping/scaling.
- Reimplemented Hypixel detection via the official Mod API.

### Removed
- Dropped layered mappings and cleaned up dependencies.

### Fixed
- Fixed a ton of YACL compatibility issues.
- Fixed keybind combos being allowed for Minecraft keybinds.
- Fixed sound duplicating when switching themes.
- Fixed item list and draggable list scroll issues.
- Fixed slider arrows not disabling at limits.
- Fixed initial radio button animation.
- Fixed action bar HUD using window bounds.
- Fixed HUD flicker when opening/closing OneConfig.
- Fixed offscreen legacy HUD rendering across 1.21.
- Fixed Mod Menu config button and mod hierarchy.
- Fixed EffectTimerPlus crash recovery.

## [1.1.6] - 2026-08-07

### Added
- Added a range slider config option for picking a minimum and maximum value.
- API: Added the `@RangeSlider` config annotation
- API: Added `Hud.keepBgWhenHidden`

### Changed
- HUDs can now keep their background visible while hidden.

### Fixed
- Fixed the search results screen crashing when HUD mod cards matched.

## [1.1.5] - 2026-08-07

### Changed
- Re-enabled Firmament HUD compatibility

### Fixed
- Fixed config overwriting
- Fixed crashes from mixins

## [1.1.4] - 2026-08-06

### Added
- Added an item list config option for picking and reordering items.
- API: Added the `@ItemList` config annotation and `Visualizer.ItemListVisualizer`
- API: Added `ModCardType`, `ModCardTypes`, and `ModCardTypeResolver` for registering and resolving mod card categories
- API: Added `HudManager.pendingAdd` and `HudManager.getRevision`
- API: Added `TextHud.DateTime` constructor overloads taking an explicit id

### Changed
- HUDs now show up as mod cards, grouped by type with collapsible sections.
- Merged HUDs now keep rows flush when a HUD resizes.

### Fixed
- Fixed HUDs not being isolated per profile.
- Fixed mixins related crashes.

## [1.1.3] - 2026-08-06

### Added
- API: Added `Hud.selfAnchorPoint` (with `setSelfAnchorPointKeepingPosition`) and an `anchorTo` overload that takes the anchored HUD's own anchor point
- API: Added `Hud.effectiveAnchorParent`, `Hud.effectiveAnchorPoint`, `Hud.effectiveSelfAnchorPoint`, and `Hud.isMergeAnchored` for reading resolved anchoring state
- API: Added `HudManager.mergeGroupOf`, `HudManager.setMergeExclusions`, and `HudManager.drawMergedBackgrounds` for working with merged HUD groups
- API: Added `NotificationsManager.ensureInitialized`
- API: Added `ScreenPlatform.showMessage`

### Changed
- Merged HUDs now move as a single group

### Fixed
- Fixed a rare JVM crash with Vulkan
- Fixed Compose rendering breaking on window resize
- Fixed the extra space between a text HUD's prefix, value, and suffix
- Fixed several crashes around screen handling, mod compatibility, and config loading

## [1.1.2] - 2026-08-04

### Added
- API: Added `Hud.canMergeBackground`, which HUDs override to opt in or out of having their background fused with neighbouring HUDs (defaults to `true` for `TextHud` only)
- API: Added `Hud.hudBackground`, which applies the HUD's background to a modifier unless it is turned off or already drawn as part of a fused shape

### Changed
- Minecraft theme text is now scaled to sit closer to the pixel grid
- API: `Hud.bgMerged` is now publicly readable so custom HUDs can skip drawing their own background while it is fused

### Fixed
- Fixed merged HUD backgrounds being drawn twice behind custom HUDs

## [1.1.1] - 2026-08-04

### Added
- HUDs can now be anchored to points on other HUDs, so they move together
- Neighbouring HUDs can now merge their backgrounds into one, optionally diagonally
- Added a favorites category to the mods list
- Padding options now have icons showing which side they affect
- The OneConfig screen can now be closed with the keybind that opens it
- API: Added HUD anchoring on `Hud`: `anchorTo`, `clearAnchor`, `isAnchored`, `anchorChainContains`, `anchorPointX`/`anchorPointY`, and the `anchorParent`, `anchorTargetId`, `anchorPoint`, `anchorOffsetX`/`anchorOffsetY` accessors
- API: Added `Hud.mergeBackground` and `Hud.mergeDiagonally` for merging backgrounds with neighbouring HUDs
- API: Added `HudManager.instanceById` for looking up a HUD instance by its id
- API: Added `TextHud.decorate` for applying prefix, suffix and brackets to a line

### Changed
- The menu now closes promptly and gives the cursor back sooner

### Fixed
- Fixed the HUD bar being drawn underneath HUD contents
- Fixed OneConfig keybinds also firing their vanilla Minecraft action
- Fixed the keybind editor not closing when close animations are turned off
- Fixed the HUD library scrollbar changing size as you scroll
- Fixed chat messages being dropped when they could not be converted for listeners

## [1.1.0] - 2026-08-03

### Added
- You can now select multiple HUDs at once, either by holding shift or by dragging a box around them
- Added copy, cut, paste, duplicate, select all, lock, reset and delete to the HUD editor, with keybinds you can change
- Added a setting to hide every HUD at once without turning them off individually
- The HUD editor menus now show the keybind for each action, which you can turn off
- Text HUDs can now be wrapped in square brackets
- OneConfig can now reopen the HUD editor instead of the main menu if that's where you left off
- API: Added `HudManager.masterHudEnabled` for globally toggling HUD rendering
- API: Added `TextHud.brackets`, wrapping the finished line with brackets
- API: Added `KeybindUtils` with `getActionModifier` and `isActionModifierPressed`

### Changed
- The menu is faster to open and smoother to use

### Fixed
- Fixed chat messages not being picked up by mods that listen for them
- Fixed OneConfig menus showing through other mods' screens
- Fixed see-through HUDs on 1.21.11 and below

## [1.0.14] - 2026-08-02

### Added
- HUD design studio now supports growth anchors, HUD locking and hiding with F1
- Mod cards can be favorited, sorted alphabetically and reordered by dragging
- Added new option types: two-thumb range slider, inheritable slider, and ordered number chain
- Keybinds can now be kept out of the Minecraft controls screen
- Option text is now automatically translated where translation keys are used
- Added compatibility layers for Kaleido and wWaypoints
- Mod version is now shown in the mod info tooltip, and the OneConfig version in the credits menu
- API: Added `OneConfigUI.open`/`createScreen` overloads that take a route to open the UI on a specific page
- API: Added `HudAnchor` and growth anchor accessors on `Hud`, plus `Hud.canDelete`
- API: Added `Property.Display` with display listeners, and `Property.isImmutable`
- API: Added `PolyColor.withChroma` for deriving chroma variants
- API: Added `isGuiHidden` and `guiWidth`/`guiHeight` to the platform screen API

### Changed
- HUD editor state is now restored when reopening it
- API: `PolyColor` constants now hand out a separate instance each time to avoid mutating a shared instance

### Fixed
- Fixed the cursor not being hidden on Wayland
- Fixed various UI issues with contrast, scrollbars, GUI scaling, disabled options and page restoration
- Fixed HUDs going missing from the HUD library, and crashes when legacy HUDs change while rendering
- Fixed MoulConfig compatibility on mods that relocate it

## [1.0.13] - 2026-07-29

### Added
- Added ability to change UI glow alpha

### Changed
- HUD add menu now is opened by default and contains all the HUDs in one scroll box

### Fixed
- Fixed TinyFD-related API issues
- Fix more text-wrapping issues

## [1.0.12] - 2026-07-28

### Added
- Added "Show in Chat" toggle to huds

### Changed
- HUD designer chrome now slides out of view while dragging
- Unlocked HUDs are now prioritized when picking overlapping HUDs
- HUDs now remain visible in the HUD editor when "Show in GUIs" is off
- "Reset all to default" now also restores all HUD settings
- Right Shift keybinds from newly installed mods are now automatically unbound (if you set it back it won't be unbound again)
- Odin's conflicting Click GUI keybind is now automatically unbound the first time you launch with it (if you set it back it won't be unbound again)

### Removed
- Removed date and time HUDs outside development environment (use EvergreenHUD instead)

## [1.0.11] - 2026-07-28

### Changed
- Use new DataStorage repo for 3rd party mod categorization
- Make main menu FPS sampler not account for unfocused FPS

## [1.0.10] - 2026-07-28

### Changed
- Significantly improve HUD / font performance

## [1.0.9] - 2026-07-26

### Fixed
- Stop notification click always firing

## [1.0.8] - 2026-07-26

### Added
- Support for WalksyLib and MidnightLib

### Fixed
- Fix compat with Gnetum
- Fix crash with null tree ids with Mod Menu

## [1.0.7] - 2026-07-24

### Removed
- Remove window blur to improve performance

### Fixed
- Fix obscure Vulkan GPU corruption
- Fix rendering with Wayland

## [1.0.6] - 2026-07-23

### Fixed
- Fix window size calculations

## [1.0.5] - 2026-07-23

### Fixed
- Fix compat with Sodium Extra's Reduce Resolution with Vulkan

## [1.0.4] - 2026-07-22

### Added
- Add ability to lock HUDs
- Add main menu FPS sampling event

## [1.0.3] - 2026-07-20

### Fixed
- Fix crash when F3 + T in UI
- Fix window padding not working on small GUI scales

## [1.0.2] - 2026-07-20

### Changed
- Optimize layout performance further

### Fixed
- Fix YACL config compat not working half the time

## [1.0.1] - 2026-07-17

### Removed
- Remove feedback page

### Fixed
- Fix HUD rendering out of world in the edit HUD menu
- Fix legacy HUDs not rendering in OneConfig UI

## [1.0.0] - 2026-07-16

### Added
- Drag HUD elements directly in the OneConfig UI
- Open the HUD editor from the OneConfig UI with Right Shift
- Add a "hidden" toggle for HUD elements

### Changed
- Various performance improvements
- Always draw boxes around HUD elements in the HUD editor (#758)
- Load profiles asynchronously and stop copying the whole config folder

### Fixed
- Fix ModMenu compat on 26.2
- Fix various font renderer bugs (#753)
  - Should work much more reliably and for all MC-supported characters
- Fix the GUI Scale setting being unreadable in the HUD designer (#756)
- Fix the "OneConfig (Platform Module)" mod card occasionally appearing (#754)
- Fix single-instance HUDs showing up in the HUD drawer (#755, #739)
- Fix HUDs always resetting to 0,0 (#746)
- Fix OneConfig keybinds not being changeable through the vanilla Controls menu on 1.21.1-1.21.8 (#737)
- Fix original compat mod screens not saving config values
- Fix various Skyblocker issues

## [1.0.0-beta.7] - 2026-07-10

### Added
- Drag HUD elements directly in the OneConfig UI
- Open the HUD editor from the OneConfig UI with Right Shift
- Add a "hidden" toggle for HUD elements

### Changed
- Various performance improvements
- Always draw boxes around HUD elements in the HUD editor (#758)
- Load profiles asynchronously and stop copying the whole config folder

### Fixed
- Fix ModMenu compat on 26.2
- Fix various font renderer bugs (#753)
  - Should work much more reliably and for all MC-supported characters
- Fix the GUI Scale setting being unreadable in the HUD designer (#756)
- Fix the "OneConfig (Platform Module)" mod card occasionally appearing (#754)
- Fix single-instance HUDs showing up in the HUD drawer (#755, #739)
- Fix HUDs always resetting to 0,0 (#746)
- Fix OneConfig keybinds not being changeable through the vanilla Controls menu on 1.21.1-1.21.8 (#737)
- Fix original compat mod screens not saving config values
- Fix various Skyblocker issues

## [1.0.0-beta.6] - 2026-07-04

### Added
- Add VulkanMod support
- Add BlazeSDL support
- New translation API
- Add all vanilla Minecraft keybinds to OneConfig keybinds
- Add missing options to KtConfig, including transformed and observable entries
- Add ModMenu entries

### Changed
- Make profiles support keybinds and all mods, even compat mods
- Dispatch ServerJoinEvent once the play connection is established (#694)
- Reset config on corrupt data and improve tree overwrite resilience
- Slow down chroma speed

### Removed
- Stop including Compose Multiplatform in the JAR

### Fixed
- Fix keybinds not unbinding
- Fix keybind infinite recursion StackOverflowError between OneConfig and kryptconfig (#688)
- Fix LegacyHud editor issues (#686)
- Fix Minecraft theme having rounded corners (#712)
- Fix Fabric API dependency issues (#721)
- Fix non-daemon threads delaying Minecraft shutdown on 26.2 (#722)
- Fix FrameBufferEvent and ChatSendEvent mixins (#719)
- Attempt to fix Sodium Extra resolution scaling
- Fix various profiles issues
- Fix theme consistency in many places
- Properly dispose Compose in certain situations

## [1.0.0-beta.5] - 2026-06-25

### Added
- Introduce new categories
  - Performance, Visuals, HUD, Utility
- Add a new third-party mod category list
  - This is a list of mods that we assign to categories based on their functionality
- Add description tooltips to Accordions and HUD settings
- Add HUD snapping
- Add HUD arrow keys to move the HUD
- Implement notifications center
- Add multi-key (chord) keybinds (#676)
- Show keybind conflicts in the keybinds screen (#675)

### Changed
- Put external mod keybinds in OC Keybinds GUI
- Make OneConfig key binds display in Minecraft key binds menu
- Optimize some dev utilities
- Move HUD settings/delete icon inside the HUD
- Round non-SVG icons automatically (#659)
- Set default step size to 1f (#682)
- Improve DesktopHelper utils and reliability

### Fixed
- Fix duplicate mod card when a mod has both OneConfig and Mod Menu config support (#663)
- Fix various issues with KtConfigs (#683, #685, #684)
- Fix `@Include` fields showing in config menu (#681)
- Fix UI clipping sidebar (#665)
- Fix UI keybind working in other GUIs
- Fix `HudManager#isEditing` not updating properly
- Fix HUD resizing not being correctly proportional to mouse drag (#662)
- Fix slider text glitching out in HUD designer/settings (#667)
- Fix MultiSelectDropdown not saving (#664)
- Fix chroma UI accent color not working (#642)
- Fix subcategories not being displayed as accordions in Cloth Config (#639)
- Fix YACL compat not using min/max values (#669)
- Fix hidden/test configs not being excluded from global search results (#679)

## [1.0.0-beta.4] - 2026-06-18

### Fixed
- Fix test HUD being included by accident

## [1.0.0-beta.3] - 2026-06-17

### Changed
- Bumped 26.2 from rc to stable

### Fixed
- Fix multiline mc style text not working.
- Fix an issue that caused most dropdowns to be empty.
- Fix showing alpha slider for all color options.

## [1.0.0-beta.2] - 2026-06-15

### Added
- Add `oneconfig` as a modid alias
- Warn when slider step is larger than its range

### Changed
- Improve support with Dandelion (Skyblocker)
- Update modern theme sounds
- Update Minecraft theme icons
- Update our 26.2 build to 26.2-rc.2

### Fixed
- Fix font letters randomly disappearing
- Fix translated text being lowercase
- Fix clipboard not working on Windows/Linux
- Fix not being able to move legacy HUDs
- Fix mouse input event

## [1.0.0-beta.1] - 2026-06-08

### Added
- Initial release
