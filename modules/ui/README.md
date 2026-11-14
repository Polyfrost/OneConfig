# OneConfig UI

The UI module is OneConfig's Compose UI kit. It gives your mod the same theme, controls, menus, dialogs and toasts
that OneConfig's own screens use, so your screens look and behave like the rest of OneConfig. The components live in
`org.polyfrost.oneconfig.api.ui.v1` and its subpackages. Screens, HUD layers and other code that needs Minecraft
types live in `org.polyfrost.oneconfig.api.ui.v1.platform`, which ships in the per-version OneConfig jar.

## Setup

Add the Polyfrost repository and depend on the module. The per-version jar is named after the Minecraft version and
loader, such as `26.3-fabric` or `1.8.9-ornithe`. You need it to host a screen or HUD layer.

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    maven("https://repo.polyfrost.org/releases")
}

dependencies {
    modImplementation("org.polyfrost.oneconfig:26.3-fabric:1.8.0")
    implementation("org.polyfrost.oneconfig:ui:1.8.0")
    implementation("org.polyfrost.oneconfig:notifications:1.8.0")
}
```

The `notifications` artifact is only needed at compile time when you pass a `NotificationType`, for example to
`Toasts.show`.

The kit's API is marked `@ExperimentalOneConfigUiApi`, and using it without opting in is a compile error. Opt in per
file with `@file:OptIn(ExperimentalOneConfigUiApi::class)`, or for the whole module:

```kotlin
kotlin {
    compilerOptions.optIn.add("org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi")
}
```

The same annotation covers the HUD API. Anything under an `internal` package or marked `@InternalOneConfigUiApi` is
not for mods.

## Theme and tokens

Kit components need a theme and a few hosts (context menus, confirm dialogs, toasts) above them. Inside a
`OneConfigComposeScreen`, a `ComposeHudLayer` or OneConfig's own pages they are already there. Anywhere else, wrap
your content in `ProvideOneConfigTheme`. Nesting it is harmless.

```kotlin
ProvideOneConfigTheme(theme = null, scaleToUi = true) {
    MyPanel()
}
```

`theme = null` uses the player's active theme. `scaleToUi = true` applies OneConfig's UI scale and pixel snapping.

Read the current theme with `OneConfigTheme.current`. It returns a `OneConfigUITheme`, and its tokens follow the
player's theme:

```kotlin
val theme = OneConfigTheme.current
Box(Modifier.background(theme.componentBackground, OneConfigTheme.shapes.medium))
Text("Saved", color = theme.success, style = OneConfigTheme.typography.small)
```

Surfaces and text: `pageBackground`, `sidebarBackground`, `componentBackground`, `popupBackground`,
`modCardBackground`, `chipBackground`, `borderColor`, `textColor`, `textColorSecondary`, `mutedText`,
`accentTextColor`, `accentBrush`. `OneConfigTheme.accent` is the accent colour on its own.

State colours: `error`, `warning`, `success`, `info`, with `onError` and `onSuccess` for text drawn on top of them.
They are adjusted to stay readable on the page. Interaction values: `focusRingColor`, `scrimColor`, `disabledAlpha`,
`hoverOverlayAlpha`, `pressedOverlayAlpha`, `controlTrackColor`, `controlThumbColor`. For text over images use
`onMedia` and `onMediaShadow`. `checkerLight` and `checkerDark` back `Modifier.transparencyCheckerboard()`.

`theme.style` is a `ThemeStyle` (`Flat`, `Glass` or `Pixel`) and `theme.isLight` tells light from dark. Check these
when your drawing depends on the theme, since players can pick custom themes you have never seen.

Shapes, typography and spacing come with the theme:

- `OneConfigTheme.shapes`: `small`, `medium`, `large`, `field`, `pill`, `circle`.
- `OneConfigTheme.typography`: `title`, `heading`, `body`, `label`, `small`, `caption`, `eyebrow`, `mono`.
- `OneConfigTheme.spacing`: `xxs` to `xxl` (2dp to 24dp) and `pageGutter`.
- `OneConfigTheme.shape(radius)` gives a shape with that corner radius that turns square under the pixel theme.
  `Shape.concentric(inset)` gives the inner shape for content inset inside another.
- `OneConfigTheme.icon(name)` returns the pixel variant of a built-in icon under the pixel theme. The kit's `Icon`
  already does this.

`OneConfigPrefs` exposes the player's appearance settings as read-only snapshot state: `animationsEnabled`,
`reducedMotion`, `backgroundBlur`, `guiOpenAnimation` and `uiScale`. For your own animations, wrap specs in
`Motion.spec(...)` or `Motion.finiteSpec(...)` and they snap when the player wants less motion.

To ship your own theme, start from a built-in one and override what you need:

```kotlin
val midnight = OneConfigTheme.custom(OneConfigTheme.POLYFROSTED_DARK, "mymod:midnight", "Midnight") {
    pageBackground = Color(0xFF0B1020)
    accentTextColor = Color(0xFF7AA2FF)
}
OneConfigTheme.activate(midnight)
```

Custom themes are not listed on the Themes page and are not remembered across restarts. For a light or dark area
inside a screen, use `ProvideThemeVariant(dark = true) { ... }`.

## Components

Every control follows the same shape: required values, then `modifier`, then options with defaults, then the trailing
lambda. Interactive controls take `enabled` and `interactionSource`, and announce themselves to the narrator. Heights
come from `ControlSize` (`Compact` 28dp, `Regular` 32dp, `Large` 38dp). Buttons and menu items share one style enum,
`ControlStyle`: `Primary`, `Secondary`, `Ghost`, `Danger`, `DangerFilled`. Defaults sit in `XxxDefaults` objects such
as `ButtonDefaults` and `TextFieldDefaults`.

| Group | Components |
|---|---|
| Buttons | `TextButton`, `IconButton`, `SplitButton`, `MoreButton`, `AnchoredPopupButton`, `ArmedConfirmButton`, `LinkText`, `Chip` |
| Fields | `TextField`, `SearchField`, `NumberField`, `KeybindField` |
| Selection | `Switch`, `SwitchRow`, `Checkbox`, `ToggleRow`, `Slider`, `Select`, `MultiSelect`, `RadioGroup`, `TabRow`, `SegmentedChips`, `ColorSwatchButton`, `ColorPickerPopup` |
| Text and icons | `Text`, `Icon`, `TooltipBox`, `Badge` |
| Layout | `PageScaffold`, `CenteredPanel`, `SectionHeader`, `Divider`, `KeyValueRow`, `CodeBlock`, `CardGrid`, `SelectionBar`, `ScrollContainer`, `Scrollbar` |
| Feedback | `ProgressBar`, `StatusText`, `EmptyState`, `NotificationsCenterPanel` |
| Images | `rememberImageBitmap`, `PolyBitmap.fromFile`, `ImageViewer` |
| Player heads | `PlayerHead`, `PlayerHeadStack`, `PlayerHeads` |

Buttons:

```kotlin
TextButton("Delete", icon = "trash", onClick = ::delete, style = ControlStyle.Danger)
IconButton("settings", contentDescription = "Settings", onClick = ::openSettings)
```

`IconButton` shows its `contentDescription` as a tooltip unless you pass `tooltip`. Its look is an
`IconButtonStyle`: `Ghost`, `Framed`, `Filled` or `Overlay`. Icons take a built-in name, a classpath path or a file
path, and every icon parameter has a `Painter` overload.

Fields and selection:

```kotlin
TextField(name, onValueChange = { name = it }, placeholder = "Profile name")
Slider(scale, onValueChange = { scale = it }, range = 0.5f..2f, step = 0.1f, label = "Scale")
Select(mode, options = Mode.entries, onSelect = { mode = it }, label = { it.title })
```

`TabRow` takes a list of `TabItem(value, label)` and a `TabStyle` (`Pill`, `Underline`, `Segmented`). `NumberField`
takes `range`, `step`, `decimals` and `unit`. For colours, `ColorSwatchButton` opens the picker itself; `ColorPickerPopup` with `rememberColorPickerModel`
gives you the picker alone.

Text and icons:

```kotlin
TooltipBox("Copies the code") { Icon("copy", contentDescription = "Copy") }
```

`Text` also accepts an `AnnotatedString`, an Adventure `Component` and a list of lines. Text you write for players
can be a `PageText` (`PageText.translatable(key, fallback)`), turned into a string with `PageText.resolve()`.

Layout and scrolling: `ScrollContainer` scrolls its content with OneConfig's wheel feel and scrollbar.
`Modifier.smoothWheelScroll(state)` adds that wheel feel to your own lazy lists, `Scrollbar(state)` draws the bar,
and `Modifier.fadingEdgesMask(state)` fades the ends. `CardGrid(items, key = { it.id }) { card -> ... }` lays out
cards in responsive columns with optional group headers. For your own surfaces, `Modifier.themedSurface(kind)` draws
a `SurfaceKind` (`Panel`, `Popup`, `Card`, `Field`) in the theme's flat, glass or pixel look. `Modifier.selectableRow`
and `Modifier.selectableCard` make rows and cards pickable. `rememberReorderState(onMove)` with
`Modifier.reorderItem(state, key, index)` lets players drag items into a new order.

Images and heads:

```kotlin
val shot = rememberImageBitmap(path, maxWidth = 512)
PlayerHead(playerId, size = 24.dp, name = playerName)
```

`rememberImageBitmap` loads off the render thread and reloads when the file changes. `PlayerHead` falls back to a
coloured initial from `name` while the skin is missing, `PlayerHeadStack` overlaps several heads, and
`PlayerHeads.face(id)` and `PlayerHeads.bitmap(id)` give you the image for your own drawing.

Interaction helpers for custom controls:

- `Modifier.oneConfigClickable(label, onClick)` adds hover and press feedback, the click sound, the focus ring, the
  hand cursor and the narrator role in one call.
- `rememberHoverState()`, `Modifier.hiddenUntilHover(visible)` and `playUiSound(UiSound.Click)`.
- `Modifier.focusRegion(order)` and `Modifier.focusRoot()` for Tab and arrow-key focus, and
  `Modifier.captureKeybinds()` to stop mod keybinds firing while an element is focused or hovered.
- `EscapeCloses(onClose)` closes your own popup on Escape before the screen behind it.
- `rememberAnchoredPosition(placement)` positions a popup next to its anchor, flipping and clamping to the window.
  `popupMaxHeight()` gives the tallest a popup may be.

## Menus

A menu is a list of `MenuEntry` values. `MenuAction` is a clickable item; `MenuSeparator`, `MenuHeading`,
`MenuRadioGroup`, `MenuIconRow` and `MenuCustomItem` cover the rest. A `MenuAction` with `children` opens a submenu.

```kotlin
val entries = listOf(
    MenuAction("Rename", onClick = ::rename, icon = "text-input", shortcutHint = "F2"),
    MenuAction("Duplicate", onClick = ::duplicate),
    MenuSeparator,
    MenuAction("Delete", onClick = ::delete, icon = "trash", danger = true),
)
```

Show it from a trigger with `DropdownMenu`, placed in the same `Box` as the trigger:

```kotlin
var open by remember { mutableStateOf(false) }
Box {
    TextButton("Options", onClick = { open = true })
    DropdownMenu(expanded = open, onDismiss = { open = false }, entries = entries)
}
```

Or attach it to any element as a right-click menu. The entries lambda runs each time the menu opens, so it can read
current state. The Menu key and Shift+F10 open it too.

```kotlin
Row(Modifier.contextMenu { entries }) { ... }
```

`SplitButton` and `MoreButton` take entries directly. `MenuItem`, `MenuSurface` and `MenuList` let you build a menu
by hand. Arrow keys, type-ahead and Escape work in all of them.

To add items to OneConfig's own menus, register a contributor. It runs each time the menu opens and returns the
entries to add, or an empty list.

```kotlin
ModCardMenuContributors.register { modId ->
    if (modId == "mymod") listOf(MenuAction("Open wiki", onClick = ::openWiki)) else emptyList()
}
```

`OptionMenuContributors` does the same for the menu on each config option, and gets the option's `Property`.

## Dialogs and feedback

`ModalDialog` is the kit's modal, and the other dialogs build on it. It draws over a scrim, closes on Escape and on a scrim click, and works inside and
outside OneConfig's screens.

```kotlin
if (showDialog) {
    ModalDialog(
        onDismiss = { showDialog = false },
        title = { DialogTitle("Reset layout") },
        buttons = {
            TextButton(DialogDefaults.cancelLabel, onClick = { showDialog = false }, style = ControlStyle.Ghost)
            TextButton("Reset", onClick = { reset(); showDialog = false }, style = ControlStyle.Danger)
        },
    ) {
        Text("Every HUD goes back to its default position.")
    }
}
```

`ConfirmDialog(title, text, confirmLabel, onConfirm, onDismiss)` is the common yes or no case. When you do not want
to keep dialog state, `Confirm.ask` queues one from any thread, including non-composable code:

```kotlin
Confirm.ask("Delete profile?", "This cannot be undone.", confirmLabel = "Delete profile", onConfirm = ::deleteProfile)
```

`TextPromptDialog(title, initial, onConfirm, onDismiss, validate = { ... })` asks for one line of text. `validate`
returns a message when the text is wrong, or null when it is fine. For an inline confirm, `ArmedConfirmButton` needs
a second click within a few seconds.

Toasts are short messages over the current screen. They queue, pause on hover and follow reduced motion.

```kotlin
Toasts.show("Copied to clipboard")
Toasts.show("Upload failed", NotificationType.ERROR, durationMillis = 6000)
```

For a message inside your layout, `rememberStatusMessage()` with `StatusText` shows a line that clears itself.

`EmptyState` fills a page or list that has nothing to show, and also covers loading and errors through `LoadState`:

```kotlin
EmptyState("No screenshots yet", icon = "folder", state = if (loading) LoadState.Loading else LoadState.Idle)
```

## Hosting

These classes are in `org.polyfrost.oneconfig.api.ui.v1.platform`, in the per-version jar. Use them from the client
thread.

`OneConfigComposeScreen` is a full-window Compose screen with the theme, Escape handling, confirm and toast hosts,
and background blur already set up. Closing it shows the screen that was open before.

```kotlin
class MyScreen : OneConfigComposeScreen(Component.literal("My Mod")) {
    @Composable
    override fun Content() {
        CenteredPanel {
            SectionHeader("General")
            TextButton("Done", onClick = ::dismiss, style = ControlStyle.Primary)
        }
    }
}

OneConfigScreens.display(MyScreen())
```

Override `blurBackground`, `closeOnEscape` or `onClosed()` to change the defaults. Pass
`renderMode = OneConfigComposeScreen.RenderMode.Continuous` when the content changes every frame in ways Compose does
not see. `OneConfigScreens.display`, `current` and `preventReplacement` work the same on every Minecraft version, and
`ScreenBlur.draw(alpha)` blurs the world behind a screen of your own.

`ComposeHudLayer` draws a Compose scene into the HUD, outside any screen. Create it once and call `draw` from your
HUD render callback every frame it should show. The sample uses the 26.1 name `GuiGraphicsExtractor`; older versions
call it `GuiGraphics`, and on 1.8.9 the call is `draw()` with no argument.

```kotlin
val watermark = ComposeHudLayer("mymod:watermark") {
    Text("My Mod", color = OneConfigTheme.current.onMedia)
}

fun renderHud(graphics: GuiGraphicsExtractor) {
    watermark.draw(graphics)
}
```

The layer takes pointer input while chat or a screen implementing `ComposeHudLayer.InputScreen` is open. Set
`acceptsInput = false` for display-only layers, toggle `visible` to hide it, and `close()` it when you are done. A
layer that throws is logged once and retried after a growing delay.

`InlinePolyCanvas` draws with OneConfig's renderer straight into the frame, without a Compose scene. Coordinates are
framebuffer pixels from the top left of the window.

```kotlin
InlinePolyCanvas.draw(graphics) { canvas ->
    canvas.context.rect(10f, 10f, 120f, 24f, PolyColor.WHITE, radius = 4f)
}
```

`PolyOffscreen(width, height)` is an offscreen target for Minecraft rendering. Render into it with
`renderBeforePresent { ... }` (or `render` at a safe point in the frame) and show the result with
`RenderContext.blit(x, y, w, h)`, for example inside `drawPoly` in a Compose draw. `EntityPreview(playerId, yaw = 30f)`
does this for you and shows a live player model, with a head as the fallback when the player is not loaded.

Mods that show a screen before OneConfig has finished starting, such as a crash screen, call
`OneConfigUI.ensureInitialized()` first.

## Config helpers

These belong to the config modules; see [config](../config/README.md) and [config-impl](../config-impl/README.md).

- `Property.collectAsState()` and `Property.asFlow()` (in `org.polyfrost.oneconfig.api.ui.v1.config`) follow an
  option's value from Compose or a coroutine.
- `Tree.switch`, `Tree.slider`, `Tree.dropdown`, `Tree.color`, `Tree.button`, `Tree.text`, `Tree.keybind`,
  `Tree.hidden` and `Tree.group` build a config tree without annotations.
- `MetaKeys` lists the typed metadata keys, read and written with `node[MetaKeys.TITLE]`.
